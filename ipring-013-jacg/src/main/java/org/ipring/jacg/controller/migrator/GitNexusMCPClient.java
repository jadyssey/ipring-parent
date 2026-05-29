package org.ipring.jacg.controller.migrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ipring.jacg.util.JavaParseLogUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GitNexusClient 的 JSON-RPC over stdio 实现。
 *
 * <p>通过 {@code npx gitnexus mcp} 子进程与 GitNexus MCP 服务通信。
 * 所有 MCP 工具调用均遵循 JSON-RPC 2.0 规范。</p>
 */
public final class GitNexusMCPClient implements GitNexusClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicInteger REQUEST_ID = new AtomicInteger(0);
    private static final long INIT_TIMEOUT_MS = 30_000;
    private static final long CALL_TIMEOUT_MS = 120_000;

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "")
            .toLowerCase().contains("win");

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private BufferedReader stderrReader;
    private volatile boolean initialized;

    // ---- 生命周期 ----

    public synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        // 多策略启动 GitNexus MCP 进程，兼容 Windows/Linux/Mac
        String lastError = null;
        for (String[] command : buildCommandCandidates()) {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(false);
                process = builder.start();
                stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                // 发送 MCP initialize 请求
                ObjectNode initReq = MAPPER.createObjectNode();
                initReq.put("jsonrpc", "2.0");
                initReq.put("id", REQUEST_ID.incrementAndGet());
                initReq.put("method", "initialize");

                ObjectNode initParams = MAPPER.createObjectNode();
                initParams.put("protocolVersion", "2024-11-05");
                initParams.set("capabilities", MAPPER.createObjectNode());
                ObjectNode clientInfo = MAPPER.createObjectNode();
                clientInfo.put("name", "ipring-jacg-migrator");
                clientInfo.put("version", "1.0.0");
                initParams.set("clientInfo", clientInfo);
                initReq.set("params", initParams);

                sendRequest(initReq);
                JsonNode initResp = readResponse(INIT_TIMEOUT_MS);
                if (initResp == null || initResp.has("error")) {
                    // 当前命令失败了，清理并尝试下一个
                    shutdownProcess();
                    lastError = "GitNexus MCP 初始化失败: "
                            + (initResp != null && initResp.has("error") ? initResp.get("error") : "无响应");
                    continue;
                }
                // MCP 协议要求 initialize 后必须发送 initialized 通知
                sendNotification("notifications/initialized");
                initialized = true;
                JavaParseLogUtils.logInfo("GitNexus MCP client initialized successfully via: "
                        + String.join(" ", command));
                return;
            } catch (IOException e) {
                shutdownProcess();
                lastError = e.getMessage();
            }
        }
        throw new MigrationException(MigrationStep.CONFIG_VALIDATION,
                "无法启动 GitNexus MCP 进程，已尝试多种策略。最后错误: " + lastError
                        + "。请确保已安装 Node.js 并执行过 npx gitnexus。");
    }

    /**
     * 构建所有可能的启动命令候选。
     *
     * <p>Windows 需要 {@code cmd /c} 包装 + {@code .cmd} 后缀，
     * Linux/Mac 直接使用 {@code npx}。</p>
     */
    private List<String[]> buildCommandCandidates() {
        List<String[]> candidates = new ArrayList<>();
        if (IS_WINDOWS) {
            // Windows: cmd /c npx.cmd gitnexus mcp
            candidates.add(new String[]{"cmd", "/c", "npx.cmd", "gitnexus", "mcp"});
            candidates.add(new String[]{"cmd", "/c", "npx", "gitnexus", "mcp"});
            candidates.add(new String[]{"npx.cmd", "gitnexus", "mcp"});
            candidates.add(new String[]{"npx", "gitnexus", "mcp"});
            // 常见 Node.js 安装路径
            candidates.add(new String[]{"cmd", "/c", System.getenv("APPDATA") + "\\npm\\npx.cmd", "gitnexus", "mcp"});
            candidates.add(new String[]{"cmd", "/c", "C:\\Program Files\\nodejs\\npx.cmd", "gitnexus", "mcp"});
        } else {
            candidates.add(new String[]{"npx", "gitnexus", "mcp"});
            candidates.add(new String[]{"/usr/local/bin/npx", "gitnexus", "mcp"});
            candidates.add(new String[]{"/usr/bin/npx", "gitnexus", "mcp"});
        }
        return candidates;
    }

    private void shutdownProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        process = null;
        stdin = null;
        stdout = null;
        stderrReader = null;
    }

    public synchronized void shutdown() {
        shutdownProcess();
        initialized = false;
    }

    // ---- GitNexusClient 接口实现 ----

    @Override
    public List<RepoInfo> listRepos() {
        ensureInitialized();
        JsonNode result = callTool("list_repos", MAPPER.createObjectNode());
        List<RepoInfo> repos = new ArrayList<>();
        if (result != null && result.isArray()) {
            for (JsonNode repo : result) {
                RepoInfo info = new RepoInfo();
                info.setName(repo.has("name") ? repo.get("name").asText() : "unknown");
                info.setPath(repo.has("path") ? repo.get("path").asText() : "unknown");
                info.setLastCommit(repo.has("lastCommit") ? repo.get("lastCommit").asText() : "");
                // indexedAt / indexedDate 兼容
                String indexed = repo.has("indexedAt") ? repo.get("indexedAt").asText()
                        : repo.has("indexedDate") ? repo.get("indexedDate").asText() : "";
                info.setIndexedDate(indexed);
                // stats 是嵌套对象
                JsonNode stats = repo.has("stats") ? repo.get("stats") : repo;
                info.setFiles(stats.has("files") ? stats.get("files").asInt() : 0);
                info.setNodes(stats.has("nodes") ? stats.get("nodes").asLong() : 0);
                info.setEdges(stats.has("edges") ? stats.get("edges").asLong() : 0);
                info.setCommunities(stats.has("communities") ? stats.get("communities").asInt() : 0);
                info.setProcesses(stats.has("processes") ? stats.get("processes").asInt() : 0);
                repos.add(info);
            }
        }
        return repos;
    }

    @Override
    public List<GitNexusSymbol> queryExecutionFlows(String entryClassName, String entryMethodName, String repoName) {
        ensureInitialized();
        ObjectNode params = MAPPER.createObjectNode();
        params.put("query", entryClassName + " " + entryMethodName);
        params.put("limit", 10);
        params.put("include_content", true);
        if (repoName != null && !repoName.isEmpty()) {
            params.put("repo", repoName);
        }
        JsonNode result = callTool("query", params);
        return parseSymbolsFromResult(result);
    }

    @Override
    public GitNexusSymbolContext getSymbolContext(String symbolName, String kind, String filePath, String repoName) {
        ensureInitialized();
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", symbolName);
        if (kind != null && !kind.isEmpty()) params.put("kind", kind);
        if (filePath != null && !filePath.isEmpty()) params.put("file_path", filePath);
        if (repoName != null && !repoName.isEmpty()) params.put("repo", repoName);
        params.put("include_content", false);

        JsonNode result = callTool("context", params);
        return parseSymbolContext(result);
    }

    @Override
    public GitNexusImpactResult analyzeImpact(String target, String direction, int maxDepth, String repoName) {
        ensureInitialized();
        ObjectNode params = MAPPER.createObjectNode();
        params.put("target", target);
        params.put("direction", direction);
        params.put("maxDepth", maxDepth);
        if (repoName != null && !repoName.isEmpty()) params.put("repo", repoName);

        JsonNode result = callTool("impact", params);
        return parseImpactResult(result);
    }

    @Override
    public List<String> resolveImplementations(String interfaceName, String repoName) {
        ensureInitialized();
        String cypher = String.format(
                "MATCH (impl:Class)-[r:CodeRelation {type: 'IMPLEMENTS'}]->(iface:Interface) "
                        + "WHERE iface.name = '%s' OR iface.fullName = '%s' "
                        + "RETURN impl.name, impl.filePath",
                shortName(interfaceName), interfaceName);
        GitNexusQueryResult cypherResult = executeCypher(cypher, repoName);
        return parseClassNamesFromMarkdownTable(cypherResult.getMarkdown());
    }

    @Override
    public List<MethodInfo> resolveClassMethods(String className, String repoName) {
        ensureInitialized();
        String cypher = String.format(
                "MATCH (c:Class {name: '%s'})-[r:CodeRelation {type: 'HAS_METHOD'}]->(m:Method) "
                        + "RETURN m.name, m.parameterCount, m.returnType",
                shortName(className));
        GitNexusQueryResult result = executeCypher(cypher, repoName);
        return parseMethodsFromMarkdownTable(result.getMarkdown());
    }

    @Override
    public Map<String, String> resolveClassFields(String className, String repoName) {
        ensureInitialized();
        String cypher = String.format(
                "MATCH (c:Class {name: '%s'})-[r:CodeRelation {type: 'HAS_PROPERTY'}]->(p:Property) "
                        + "RETURN p.name, p.declaredType",
                shortName(className));
        GitNexusQueryResult result = executeCypher(cypher, repoName);
        Map<String, String> fields = new LinkedHashMap<>();
        if (result.getMarkdown() != null) {
            String[] lines = result.getMarkdown().split("\n");
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("|") && line.endsWith("|")) {
                    String[] cols = line.split("\\|");
                    if (cols.length >= 3) {
                        fields.put(cols[1].trim(), cols[2].trim());
                    }
                }
            }
        }
        return fields;
    }

    @Override
    public GitNexusChangeReport detectChanges(String repoName) {
        ensureInitialized();
        ObjectNode params = MAPPER.createObjectNode();
        params.put("scope", "unstaged");
        if (repoName != null && !repoName.isEmpty()) params.put("repo", repoName);

        JsonNode result = callTool("detect_changes", params);
        GitNexusChangeReport report = new GitNexusChangeReport();
        report.setChangedSymbols(new ArrayList<>());
        report.setAffectedProcesses(new ArrayList<>());
        if (result != null) {
            if (result.has("changedSymbols") && result.get("changedSymbols").isArray()) {
                result.get("changedSymbols").forEach(s -> report.getChangedSymbols().add(s.asText()));
            }
            if (result.has("affectedProcesses") && result.get("affectedProcesses").isArray()) {
                result.get("affectedProcesses").forEach(p -> report.getAffectedProcesses().add(p.asText()));
            }
            if (result.has("risk")) {
                report.setRiskSummary(result.get("risk").asText());
            }
        }
        return report;
    }

    @Override
    public GitNexusQueryResult executeCypher(String cypherQuery, String repoName) {
        ensureInitialized();
        ObjectNode params = MAPPER.createObjectNode();
        params.put("query", cypherQuery);
        if (repoName != null && !repoName.isEmpty()) params.put("repo", repoName);

        JsonNode result = callTool("cypher", params);
        GitNexusQueryResult queryResult = new GitNexusQueryResult();
        if (result != null) {
            if (result.has("markdown")) queryResult.setMarkdown(result.get("markdown").asText());
            if (result.has("row_count")) queryResult.setRowCount(result.get("row_count").asInt());
        }
        return queryResult;
    }

    // ---- JSON-RPC 通信 ----

    private synchronized JsonNode callTool(String toolName, ObjectNode params) {
        try {
            ObjectNode req = MAPPER.createObjectNode();
            req.put("jsonrpc", "2.0");
            req.put("id", REQUEST_ID.incrementAndGet());
            req.put("method", "tools/call");
            ObjectNode callParams = MAPPER.createObjectNode();
            callParams.put("name", toolName);
            callParams.set("arguments", params);
            req.set("params", callParams);

            sendRequest(req);
            JsonNode response = readResponse(CALL_TIMEOUT_MS);
            if (response == null) {
                throw new MigrationException(MigrationStep.ENTRY_RESOLUTION,
                        "GitNexus 工具调用超时: " + toolName);
            }
            if (response.has("error")) {
                JsonNode error = response.get("error");
                throw new MigrationException(MigrationStep.ENTRY_RESOLUTION,
                        "GitNexus 工具调用失败 [" + toolName + "]: "
                                + error.get("message").asText());
            }
            // 提取 result.content[0].text
            JsonNode result = response.get("result");
            if (result != null && result.has("content") && result.get("content").isArray()) {
                JsonNode content = result.get("content");
                if (content.size() > 0 && content.get(0).has("text")) {
                    String text = content.get(0).get("text").asText();
                    try {
                        return MAPPER.readTree(text);
                    } catch (Exception e) {
                        // 如果不是 JSON，包装为 text 节点
                        return MAPPER.createObjectNode().put("text", text);
                    }
                }
            }
            return result;
        } catch (MigrationException e) {
            throw e;
        } catch (Exception e) {
            throw new MigrationException(MigrationStep.ENTRY_RESOLUTION,
                    "GitNexus 通信异常 [" + toolName + "]: " + e.getMessage(), e);
        }
    }

    private void sendRequest(ObjectNode request) throws IOException {
        String json = MAPPER.writeValueAsString(request);
        stdin.write(json);
        stdin.newLine();
        stdin.flush();
    }

    /**
     * 发送 JSON-RPC 通知（无 id，无需等待响应）。
     *
     * <p>MCP 协议要求在 initialize 成功后发送 {@code notifications/initialized}，
     * 否则服务端不处理后续 tool 调用。</p>
     */
    private void sendNotification(String method) throws IOException {
        ObjectNode notification = MAPPER.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.set("params", MAPPER.createObjectNode());
        String json = MAPPER.writeValueAsString(notification);
        stdin.write(json);
        stdin.newLine();
        stdin.flush();
    }

    private JsonNode readResponse(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                if (stdout.ready() || delayForReady(50)) {
                    String line = stdout.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        return MAPPER.readTree(line);
                    } catch (Exception e) {
                        JavaParseLogUtils.logWarn("Failed to parse GitNexus response line: " + line);
                    }
                }
                checkProcessAlive();
            }
        } catch (IOException e) {
            JavaParseLogUtils.logWarn("GitNexus read error: " + e.getMessage());
        }
        return null;
    }

    private boolean delayForReady(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void checkProcessAlive() {
        if (process != null && !process.isAlive()) {
            int exitCode = process.exitValue();
            String stderr = readStderr();
            throw new MigrationException(MigrationStep.CONFIG_VALIDATION,
                    "GitNexus 进程意外退出 (exitCode=" + exitCode + "): " + stderr);
        }
    }

    private String readStderr() {
        StringBuilder sb = new StringBuilder();
        try {
            while (stderrReader != null && stderrReader.ready()) {
                String line = stderrReader.readLine();
                if (line != null) sb.append(line).append("\n");
            }
        } catch (IOException ignored) { /* best effort */ }
        return sb.toString().trim();
    }

    // ---- 响应解析 ----

    private List<GitNexusSymbol> parseSymbolsFromResult(JsonNode result) {
        if (result == null) return Collections.emptyList();
        List<GitNexusSymbol> symbols = new ArrayList<>();

        JsonNode processSymbols = result.get("process_symbols");
        if (processSymbols != null && processSymbols.isArray()) {
            for (JsonNode ps : processSymbols) {
                JsonNode symbolList = ps.get("symbols");
                if (symbolList != null && symbolList.isArray()) {
                    for (JsonNode s : symbolList) {
                        symbols.add(parseSymbol(s));
                    }
                }
            }
        }
        return symbols;
    }

    private GitNexusSymbol parseSymbol(JsonNode s) {
        GitNexusSymbol symbol = new GitNexusSymbol();
        symbol.setName(safeText(s, "name"));
        symbol.setFullName(safeText(s, "fullName"));
        symbol.setFilePath(safeText(s, "filePath"));
        symbol.setKind(safeText(s, "kind"));
        symbol.setModule(safeText(s, "module"));
        symbol.setLineNumber(s.has("lineNumber") ? s.get("lineNumber").asInt() : 0);
        return symbol;
    }

    private GitNexusSymbolContext parseSymbolContext(JsonNode result) {
        GitNexusSymbolContext ctx = new GitNexusSymbolContext();
        if (result == null) return ctx;

        // context 返回格式: { "symbol": { "name":..., "kind":..., "filePath":... },
        //                    "incoming": {...}, "outgoing": {"has_method":[{...}]} }
        JsonNode symbol = result.has("symbol") ? result.get("symbol") : result;
        ctx.setName(safeText(symbol, "name"));
        ctx.setFilePath(safeText(symbol, "filePath"));
        ctx.setKind(safeText(symbol, "kind"));

        // incoming: 调用方的多种关系类型
        JsonNode incoming = result.has("incoming") ? result.get("incoming") : null;
        List<String> callers = new ArrayList<>();
        if (incoming != null && incoming.isObject()) {
            incoming.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(item ->
                            callers.add(item.isObject() ? safeText(item, "name") : item.asText()));
                }
            });
        }
        ctx.setCallers(callers);

        // outgoing: 被调用方，特别关注 has_method（方法列表）
        JsonNode outgoing = result.has("outgoing") ? result.get("outgoing") : null;
        List<String> callees = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        if (outgoing != null && outgoing.isObject()) {
            outgoing.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    for (JsonNode item : entry.getValue()) {
                        String name = item.isObject() ? safeText(item, "name") : item.asText();
                        if ("has_method".equals(entry.getKey())) {
                            methods.add(name);
                        } else {
                            callees.add(name);
                        }
                    }
                }
            });
        }
        ctx.setCallees(callees);
        ctx.setMethods(methods);
        ctx.setImplementations(new ArrayList<>());
        ctx.setInterfaces(new ArrayList<>());
        ctx.setFields(new ArrayList<>());
        return ctx;
    }

    private GitNexusImpactResult parseImpactResult(JsonNode result) {
        GitNexusImpactResult ir = new GitNexusImpactResult();
        if (result == null) return ir;

        ir.setRisk(safeText(result, "risk"));
        ir.setAffectedProcesses(parseStringList(result, "affected_processes"));

        Map<Integer, List<String>> byDepth = new LinkedHashMap<>();
        JsonNode depthNode = result.get("byDepth");
        if (depthNode != null && depthNode.isObject()) {
            depthNode.fields().forEachRemaining(entry -> {
                int depth = Integer.parseInt(entry.getKey());
                List<String> items = new ArrayList<>();
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(item -> {
                        // item 是对象 {"name":"Xxx.java","filePath":"src/...",...}
                        if (item.isObject()) {
                            items.add(item.has("name") ? item.get("name").asText() : item.asText());
                        } else {
                            items.add(item.asText());
                        }
                    });
                }
                byDepth.put(depth, items);
            });
        }
        ir.setByDepth(byDepth);
        return ir;
    }

    private List<String> parseStringList(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        if (node != null && node.has(field) && node.get(field).isArray()) {
            node.get(field).forEach(item -> list.add(item.asText()));
        }
        return list;
    }

    private List<String> parseClassNamesFromMarkdownTable(String markdown) {
        if (markdown == null || markdown.isEmpty()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        String[] lines = markdown.split("\n");
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("|") && line.endsWith("|")) {
                String[] cols = line.split("\\|");
                if (cols.length >= 2) {
                    String name = cols[1].trim();
                    if (!name.isEmpty()) names.add(name);
                }
            }
        }
        return names;
    }

    private List<MethodInfo> parseMethodsFromMarkdownTable(String markdown) {
        if (markdown == null || markdown.isEmpty()) return Collections.emptyList();
        List<MethodInfo> methods = new ArrayList<>();
        String[] lines = markdown.split("\n");
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("|") && line.endsWith("|")) {
                String[] cols = line.split("\\|");
                if (cols.length >= 4) {
                    MethodInfo m = new MethodInfo();
                    m.setName(cols[1].trim());
                    try { m.setParameterCount(Integer.parseInt(cols[2].trim())); } catch (NumberFormatException ignored) {}
                    m.setReturnType(cols[3].trim());
                    methods.add(m);
                }
            }
        }
        return methods;
    }

    // ---- 工具方法 ----

    private static String safeText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText() : "";
    }

    private static String shortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return fullName;
        int idx = fullName.lastIndexOf('.');
        return idx < 0 ? fullName : fullName.substring(idx + 1);
    }
}
