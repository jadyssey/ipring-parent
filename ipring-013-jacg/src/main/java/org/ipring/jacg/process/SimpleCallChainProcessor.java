package org.ipring.jacg.process;

import com.adrninistrator.jacg.annotation.util.AnnotationAttributesParseUtil;
import com.adrninistrator.jacg.dto.annotation.ListStringAnnotationAttribute;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import com.adrninistrator.jacg.util.JACGJsonUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ipring.jacg.mapper.ClassAnnotationMapper;
import org.ipring.jacg.mapper.po.JacgClassAnnotationPO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SimpleCallChainProcessor {

    private final ClassAnnotationMapper classAnnotationMapper;



    private static final String SPLIT = "&";
    private static final List<String> CONTROLLER_ANNOTATION_LIST = Arrays.asList("org.springframework.web.bind.annotation.PostMapping", "org.springframework.web.bind.annotation.GetMapping", "org.springframework.web.bind.annotation.PutMapping", "org.springframework.web.bind.annotation.DeleteMapping");

    private static final String REQUEST_ANNO = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String ANNO_VALUE = "value";
    private static final String PACKAGE = "com.cds";

    // 类和方法名分隔符号
    private static final String CONTROLLER_SPLIT = ":";
    private static final Gson gson = new Gson();

    public List<String> extractLeafPathsByModel(List<MethodCallLineData4Ee> methodCallLineData4Ees) {
        List<String> result = new ArrayList<>();
        Stack<CallNode> stack = new Stack<>();

        for (MethodCallLineData4Ee line : methodCallLineData4Ees) {
            if (Objects.isNull(line)) continue;
            if (!line.getActualFullMethod().startsWith(PACKAGE)) continue;

            // 解析层级和内容
            int level = line.getMethodCallLevel();
            String content = line.getActualFullMethod().trim();
            if (!CollectionUtils.isEmpty(line.getMethodAnnotationMap())) {
                String uri = CONTROLLER_ANNOTATION_LIST.stream().map(anno -> Optional.ofNullable(line.getMethodAnnotationMap())
                        .map(map -> map.get(anno)).map(map -> AnnotationAttributesParseUtil.getAttributeValueFromMap(map, ANNO_VALUE, ListStringAnnotationAttribute.class))
                        .map(ListStringAnnotationAttribute::getAttributeList).filter(CollectionUtils::isNotEmpty).map(list -> String.join(",", list)).orElse(null)).filter(Objects::nonNull).collect(Collectors.joining(","));
                if (StringUtils.isNotBlank(uri)) {
                    content = formatPath(uri);
                    JacgClassAnnotationPO jacgClassAnnotationPO = classAnnotationMapper.selectByClassAndAnno(line.getCallerSimpleClassName(), REQUEST_ANNO);
                    if (Objects.nonNull(jacgClassAnnotationPO)) {
                        List<String> attrValue = JACGJsonUtil.getObjFromJsonStr(jacgClassAnnotationPO.getAttributeValue(), new TypeReference<List<String>>() {
                        });
                        content = formatPath(attrValue.stream().findFirst().orElse("")) + formatPath(uri);
                    }
                }
            }

            // 调整栈大小
            while (stack.size() > level) {
                // 如果栈顶元素是叶子节点，保存路径
                if (stack.peek().isLeaf) {
                    savePath(stack, result);
                }
                stack.pop();
            }

            // 压入新节点
            CallNode node = new CallNode(level, content);
            if (!stack.isEmpty()) {
                stack.peek().isLeaf = false; // 父节点不再是叶子节点
            }
            stack.push(node);
        }

        // 处理栈中剩余的叶子节点
        while (!stack.isEmpty()) {
            if (stack.peek().isLeaf) {
                savePath(stack, result);
            }
            stack.pop();
        }

        return result;
    }


    public static List<String> extractLeafPaths(List<String> input) {
        List<String> result = new ArrayList<>();
        Stack<CallNode> stack = new Stack<>();

        for (String line : input) {
            if (line.trim().isEmpty()) continue;

            // 解析层级和内容
            int levelStart = line.indexOf('[');
            int levelEnd = line.indexOf(']');
            if (levelStart == -1 || levelEnd == -1) continue;

            int level = Integer.parseInt(line.substring(levelStart + 1, levelEnd));
            String content = line.substring(levelEnd + 2).trim();

            // 清理内容
            int parenIndex = content.indexOf('(');
            if (parenIndex != -1) {
                content = content.substring(0, parenIndex).trim();
            }

            // 调整栈大小
            while (stack.size() > level) {
                // 如果栈顶元素是叶子节点，保存路径
                if (stack.peek().isLeaf) {
                    savePath(stack, result);
                }
                stack.pop();
            }

            // 压入新节点
            CallNode node = new CallNode(level, content);
            if (!stack.isEmpty()) {
                stack.peek().isLeaf = false; // 父节点不再是叶子节点
            }
            stack.push(node);
        }

        // 处理栈中剩余的叶子节点
        while (!stack.isEmpty()) {
            if (stack.peek().isLeaf) {
                savePath(stack, result);
            }
            stack.pop();
        }

        return result;
    }

    private static void savePath(Stack<CallNode> stack, List<String> result) {
        List<String> path = new ArrayList<>();
        for (CallNode node : stack) {
            if (node.isLeaf) {
                // 叶子节点放在首位
                path.add(0, node.content);
                break;
            }
            path.add(node.content);
        }
        result.add(String.join(SPLIT, path));
    }

    public static String formatPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 补开头的/
        StringBuilder formattedPath = new StringBuilder();
        if (!path.startsWith("/")) {
            formattedPath.append("/");
        }
        formattedPath.append(path);

        // 去掉结尾的/（避免只剩一个/的情况）
        if (formattedPath.length() > 1 && formattedPath.charAt(formattedPath.length() - 1) == '/') {
            formattedPath.deleteCharAt(formattedPath.length() - 1);
        }

        return formattedPath.toString();
    }


    static class CallNode {
        int level;
        String content;
        boolean isLeaf = true;

        CallNode(int level, String content) {
            this.level = level;
            this.content = content;
        }
    }

    public static void main(String[] args) {
        List<String> input = Arrays.asList(
                "[0]#HubAssignTaskMapper:saveTask",
                "[1]#  HubAssignTaskServiceImpl:taskMerging",
                "[2]#    IHubAssignTaskService:taskMerging",
                "[3]#      HubAssignTaskController:taskMerging",
                "[1]#  HubWaybillPoolTaskServiceImpl:grantTaskHandlerV2",
                "[2]#    HubWaybillPoolTaskServiceImpl:waybillPollTaskSubmitV2",
                "[3]#      IHubWaybillPoolHandlerService:waybillPollTaskSubmitV2",
                "[4]#        HubWaybillPoolController:waybillPollTaskSubmitV2",
                "[1]#  HubAssignTaskServiceImpl:taskMergingCrossArea",
                "[2]#    IHubAssignTaskService:taskMergingCrossArea",
                "[3]#      HubAssignTaskController:taskMergingCrossArea"
        );

        List<String> leafPaths = extractLeafPaths(input);
        System.out.println("叶子节点路径:");
        leafPaths.forEach(System.out::println);
    }
}