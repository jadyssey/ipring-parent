package org.ipring.jacg.controller.migrator;

import java.util.List;
import java.util.Map;

/**
 * GitNexus 知识图谱客户端 —— 迁移工具与 GitNexus MCP 之间的唯一集成入口。
 *
 * <p>封装所有与 GitNexus 的交互（查询、符号上下文、影响分析、Cypher 查询等），
 * 确保迁移核心逻辑不直接依赖任何第三方分析库，仅依赖 GitNexus 知识图谱。</p>
 *
 * <h4>设计原则</h4>
 * <ul>
 *   <li>单一入口：所有数据查询均通过此接口，不做旁路绕过</li>
 *   <li>纯数据返回：返回结构化的标准 Java 类型，不暴露 MCP 协议细节</li>
 *   <li>Fail-fast：查询失败时抛出 MigrationException，由调用方统一处理</li>
 * </ul>
 */
public interface GitNexusClient {

    /**
     * 列出所有已索引的代码仓库及其统计信息。
     *
     * @return 仓库信息列表（包含名称、路径、文件数、节点数、边数、社区数、执行流数等）
     * @throws MigrationException 当 GitNexus 服务不可用时
     */
    List<RepoInfo> listRepos();

    /**
     * 根据入口类和方法名查询知识图谱，获取相关的执行流（Process）。
     *
     * @param entryClassName  入口类全限定名或简单名
     * @param entryMethodName 入口方法名
     * @param repoName        仓库名（可选，单仓库时可传 null）
     * @return 执行流列表，每个执行流包含涉及的符号（类、方法）及其文件位置
     * @throws MigrationException 查询失败
     */
    List<GitNexusSymbol> queryExecutionFlows(String entryClassName, String entryMethodName, String repoName);

    /**
     * 获取指定符号的 360 度全景视图（调用者、被调用者、继承关系等）。
     *
     * @param symbolName 符号名（方法名、类名）
     * @param kind       符号类型提示（如 "Method", "Class", "Interface"）
     * @param filePath   文件路径提示（用于消歧义，可选）
     * @param repoName   仓库名（可选）
     * @return 符号的完整上下文信息
     * @throws MigrationException 查询失败
     */
    GitNexusSymbolContext getSymbolContext(String symbolName, String kind, String filePath, String repoName);

    /**
     * 分析修改某个符号的影响范围（爆炸半径）。
     *
     * @param target    目标符号名
     * @param direction "upstream"（谁依赖它）或 "downstream"（它依赖谁）
     * @param maxDepth  最大关系深度
     * @param repoName  仓库名（可选）
     * @return 受影响符号按深度分组的结果
     * @throws MigrationException 分析失败
     */
    GitNexusImpactResult analyzeImpact(String target, String direction, int maxDepth, String repoName);

    /**
     * 查找接口的所有实现类。
     *
     * @param interfaceName 接口全限定名
     * @param repoName      仓库名（可选）
     * @return 实现类的全限定名列表
     * @throws MigrationException 查询失败
     */
    List<String> resolveImplementations(String interfaceName, String repoName);

    /**
     * 查找一个类的所有方法。
     *
     * @param className 类全限定名
     * @param repoName  仓库名（可选）
     * @return 方法名列表
     * @throws MigrationException 查询失败
     */
    List<MethodInfo> resolveClassMethods(String className, String repoName);

    /**
     * 查找一个类的所有字段。
     *
     * @param className 类全限定名
     * @param repoName  仓库名（可选）
     * @return 字段名与类型声明的映射
     * @throws MigrationException 查询失败
     */
    Map<String, String> resolveClassFields(String className, String repoName);

    /**
     * 检测未提交的 git 变更，分析受影响的下游执行流。
     *
     * @param repoName 仓库名（可选）
     * @return 变更分析与受影响流程
     * @throws MigrationException 检测失败
     */
    GitNexusChangeReport detectChanges(String repoName);

    /**
     * 执行自定义 Cypher 图查询（用于复杂关系发现）。
     *
     * @param cypherQuery Cypher 查询语句
     * @param repoName    仓库名（可选）
     * @return 查询结果（Markdown 表格格式 + 行数）
     * @throws MigrationException 查询失败
     */
    GitNexusQueryResult executeCypher(String cypherQuery, String repoName);

    // ======================== 数据模型 ========================

    /** GitNexus 返回的符号信息 */
    class GitNexusSymbol {
        private String name;
        private String fullName;
        private String filePath;
        private String kind;
        private String module;
        private int lineNumber;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    }

    /** 符号360度全景视图 */
    class GitNexusSymbolContext {
        private String name;
        private String filePath;
        private String kind;
        private List<String> callers;
        private List<String> callees;
        private List<String> implementations;
        private List<String> interfaces;
        private List<String> methods;
        private List<String> fields;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }

        public List<String> getCallers() { return callers; }
        public void setCallers(List<String> callers) { this.callers = callers; }

        public List<String> getCallees() { return callees; }
        public void setCallees(List<String> callees) { this.callees = callees; }

        public List<String> getImplementations() { return implementations; }
        public void setImplementations(List<String> implementations) { this.implementations = implementations; }

        public List<String> getInterfaces() { return interfaces; }
        public void setInterfaces(List<String> interfaces) { this.interfaces = interfaces; }

        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }

        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
    }

    /** 影响分析结果 */
    class GitNexusImpactResult {
        private String risk;
        private Map<Integer, List<String>> byDepth;
        private List<String> affectedProcesses;

        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }

        public Map<Integer, List<String>> getByDepth() { return byDepth; }
        public void setByDepth(Map<Integer, List<String>> byDepth) { this.byDepth = byDepth; }

        public List<String> getAffectedProcesses() { return affectedProcesses; }
        public void setAffectedProcesses(List<String> affectedProcesses) { this.affectedProcesses = affectedProcesses; }
    }

    /** 方法信息 */
    class MethodInfo {
        private String name;
        private int parameterCount;
        private String returnType;
        private String signature;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getParameterCount() { return parameterCount; }
        public void setParameterCount(int parameterCount) { this.parameterCount = parameterCount; }

        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }

    /** 变更检测报告 */
    class GitNexusChangeReport {
        private List<String> changedSymbols;
        private List<String> affectedProcesses;
        private String riskSummary;

        public List<String> getChangedSymbols() { return changedSymbols; }
        public void setChangedSymbols(List<String> changedSymbols) { this.changedSymbols = changedSymbols; }

        public List<String> getAffectedProcesses() { return affectedProcesses; }
        public void setAffectedProcesses(List<String> affectedProcesses) { this.affectedProcesses = affectedProcesses; }

        public String getRiskSummary() { return riskSummary; }
        public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    }

    /** Cypher 查询结果 */
    class GitNexusQueryResult {
        private String markdown;
        private int rowCount;

        public String getMarkdown() { return markdown; }
        public void setMarkdown(String markdown) { this.markdown = markdown; }

        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    }

    /** 仓库索引信息 */
    class RepoInfo {
        private String name;
        private String path;
        private int files;
        private long nodes;
        private long edges;
        private int communities;
        private int processes;
        private String indexedDate;
        private String lastCommit;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public int getFiles() { return files; }
        public void setFiles(int files) { this.files = files; }

        public long getNodes() { return nodes; }
        public void setNodes(long nodes) { this.nodes = nodes; }

        public long getEdges() { return edges; }
        public void setEdges(long edges) { this.edges = edges; }

        public int getCommunities() { return communities; }
        public void setCommunities(int communities) { this.communities = communities; }

        public int getProcesses() { return processes; }
        public void setProcesses(int processes) { this.processes = processes; }

        public String getIndexedDate() { return indexedDate; }
        public void setIndexedDate(String indexedDate) { this.indexedDate = indexedDate; }

        public String getLastCommit() { return lastCommit; }
        public void setLastCommit(String lastCommit) { this.lastCommit = lastCommit; }

        @Override
        public String toString() {
            return name + "{files=" + files + ", nodes=" + nodes + ", edges=" + edges
                    + ", communities=" + communities + ", processes=" + processes + "}";
        }
    }
}
