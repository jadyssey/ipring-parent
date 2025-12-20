package org.ipring.jacg.process;

import com.adrninistrator.jacg.annotation.util.AnnotationAttributesParseUtil;
import com.adrninistrator.jacg.dto.annotation.ListStringAnnotationAttribute;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CallChainProcessor {

    private static final String SPLIT = "&";
    private static final List<String> CONTROLLER_ANNOTATION_LIST = Arrays.asList("org.springframework.web.bind.annotation.PostMapping", "org.springframework.web.bind.annotation.GetMapping", "org.springframework.web.bind.annotation.PutMapping", "org.springframework.web.bind.annotation.DeleteMapping");
    private static final String ANNO_VALUE = "value";
    /**
     * 处理调用链数据，只保留叶子节点的完整路径
     */
     public static List<String> processCallChainsString(List<String> inputLines) {
        List<String> result = new ArrayList<>();
        if (inputLines == null || inputLines.isEmpty()) {
            return result;
        }
        
        // 解析所有节点
        List<CallNode> nodes = parseNodes(inputLines);
        if (nodes.isEmpty()) {
            return result;
        }
        
        return processCallChainsByNodes(nodes);
    }
    public static List<String> processCallChainsMethodCall(List<MethodCallLineData4Ee> inputLines) {
        List<String> result = new ArrayList<>();
        if (inputLines == null || inputLines.isEmpty()) {
            return result;
        }

        // 解析所有节点
        List<CallNode> nodes = parseMethodCallNodes(inputLines);
        if (nodes.isEmpty()) {
            return result;
        }

        return processCallChainsByNodes(nodes);
    }

    private static List<String> processCallChainsByNodes(List<CallNode> nodes) {
        List<String> result = new ArrayList<>();
        if (nodes.isEmpty()) {
            return result;
        }

        // 构建树结构
        CallNode root = buildTree(nodes);
        if (root == null) {
            return result;
        }

        // 查找所有叶子节点并获取完整路径
        List<List<CallNode>> leafPaths = findLeafPaths(root);

        // 转换为字符串格式
        for (List<CallNode> path : leafPaths) {
            String chain = buildChainString(path);
            result.add(chain);
        }

        return result;
    }

    /**
     * 解析每一行，提取层级和节点信息
     */
    private static List<CallNode> parseMethodCallNodes(List<MethodCallLineData4Ee> methodCallLineData4Ees) {
        List<CallNode> nodes = new ArrayList<>();

        for (MethodCallLineData4Ee methodCallLineData4Ee : methodCallLineData4Ees) {
            if (Objects.isNull(methodCallLineData4Ee)) continue;
            int level = methodCallLineData4Ee.getMethodCallLevel();
            String content = methodCallLineData4Ee.getActualFullMethod();
            if (Objects.nonNull(methodCallLineData4Ee.getMethodAnnotationMap())) {
                content = CONTROLLER_ANNOTATION_LIST.stream().map(anno -> Optional.ofNullable(methodCallLineData4Ee.getMethodAnnotationMap())
                        .map(map -> map.get(anno)).map(map -> AnnotationAttributesParseUtil.getAttributeValueFromMap(map, ANNO_VALUE, ListStringAnnotationAttribute.class))
                        .map(ListStringAnnotationAttribute::getAttributeList).filter(CollectionUtils::isNotEmpty).map(list -> String.join(",", list)).orElse(null)).filter(Objects::nonNull).collect(Collectors.joining(","));
            }
            nodes.add(new CallNode(level, content));
        }
        return nodes;
    }

    /**
     * 解析每一行，提取层级和节点信息
     */
    private static List<CallNode> parseNodes(List<String> lines) {
        List<CallNode> nodes = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]#\\s*(.+)");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int level = Integer.parseInt(matcher.group(1));
                String content = matcher.group(2).trim();

                // 清理内容（去掉括号部分）
                int parenIndex = content.indexOf('(');
                if (parenIndex != -1) {
                    content = content.substring(0, parenIndex).trim();
                }

                nodes.add(new CallNode(level, content));
            }
        }
        return nodes;
    }
    
    /**
     * 构建树形结构
     */
    private static CallNode buildTree(List<CallNode> nodes) {
        if (nodes.isEmpty()) return null;
        
        CallNode root = nodes.get(0);
        Stack<CallNode> stack = new Stack<>();
        stack.push(root);
        
        for (int i = 1; i < nodes.size(); i++) {
            CallNode currentNode = nodes.get(i);
            
            // 弹出栈顶元素直到找到父节点
            while (!stack.isEmpty() && stack.peek().level >= currentNode.level) {
                stack.pop();
            }
            
            if (!stack.isEmpty()) {
                CallNode parent = stack.peek();
                parent.children.add(currentNode);
                currentNode.parent = parent;
            }
            
            stack.push(currentNode);
        }
        
        return root;
    }
    
    /**
     * 查找所有叶子节点的完整路径（使用深度优先搜索）[1,4](@ref)
     */
    private static List<List<CallNode>> findLeafPaths(CallNode root) {
        List<List<CallNode>> paths = new ArrayList<>();
        if (root == null) return paths;
        
        List<CallNode> currentPath = new ArrayList<>();
        
        // 深度优先搜索遍历树结构[1](@ref)
        dfs(root, currentPath, paths);
        
        return paths;
    }
    
    /**
     * 深度优先搜索递归函数
     */
    private static void dfs(CallNode node, List<CallNode> currentPath, List<List<CallNode>> paths) {
        if (node == null) return;
        
        // 将当前节点加入路径
        currentPath.add(node);
        
        // 如果是叶子节点，保存当前路径[7](@ref)
        if (node.children.isEmpty()) {
            paths.add(new ArrayList<>(currentPath));
        } else {
            // 递归处理子节点
            for (CallNode child : node.children) {
                dfs(child, currentPath, paths);
            }
        }
        
        // 回溯，移除当前节点
        currentPath.remove(currentPath.size() - 1);
    }
    
    /**
     * 构建路径字符串
     */
    private static String buildChainString(List<CallNode> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i == path.size() - 1) {
                // 叶子节点放在首位
                sb.insert(0, path.get(i).content);
                break;
            }
            if (i > 0) sb.append(SPLIT);
            sb.append(path.get(i).content);
        }
        return sb.toString();
    }
    
    /**
     * 调用链节点类
     */
    static class CallNode {
        int level;
        String content;
        CallNode parent;
        List<CallNode> children;
        
        CallNode(int level, String content) {
            this.level = level;
            this.content = content;
            this.children = new ArrayList<>();
        }
        
        @Override
        public String toString() {
            return "[" + level + "]" + content;
        }
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 模拟输入数据
        List<String> inputLines = Arrays.asList(
            "[0]#HubAssignTaskMapper:saveTask",
            "[1]#  HubAssignTaskServiceImpl:taskMerging",
            "[2]#    IHubAssignTaskService:taskMerging",
            "[3]#      HubAssignTaskController:taskMerging(/admin/b4/apple/hubAssignTask/taskMerging)",
            "[1]#  HubWaybillPoolTaskServiceImpl:grantTaskHandlerV2",
            "[2]#    HubWaybillPoolTaskServiceImpl:waybillPollTaskSubmitV2",
            "[3]#      IHubWaybillPoolHandlerService:waybillPollTaskSubmitV2",
            "[4]#        HubWaybillPoolController:waybillPollTaskSubmitV2",
            "[1]#  HubAssignTaskServiceImpl:taskMergingCrossArea",
            "[2]#    IHubAssignTaskService:taskMergingCrossArea",
            "[3]#      HubAssignTaskController:taskMergingCrossArea"
        );
        
        // 处理调用链
        List<String> result = processCallChainsString(inputLines);
        
        // 输出结果
        System.out.println("叶子节点的完整路径:");
        for (String chain : result) {
            System.out.println(chain);
        }
    }
}