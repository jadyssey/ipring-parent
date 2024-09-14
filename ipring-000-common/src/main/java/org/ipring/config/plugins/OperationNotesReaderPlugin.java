package org.ipring.config.plugins;

import com.google.common.base.Optional;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.ipring.anno.StlApiOperation;
import org.ipring.anno.StlServiceCode;
import org.ipring.constant.CommonConstants;
import org.ipring.enums.SubCode;
import org.ipring.enums.subcode.SystemServiceCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.util.*;

/**
 * swagger注解notes值补丁
 *
 * @author lgj
 * @date 8/2/2023
 */
@ConditionalOnProperty(value = "ipring.swagger.enable", havingValue = "true")
@Component
@RequiredArgsConstructor
public class OperationNotesReaderPlugin implements OperationBuilderPlugin {

    private final DescriptionResolver descriptions;

    /**
     * subcode顺序分隔符
     */
    private static final String SUBCODE_SEQUENCE_SPLIT_CHAR = "、";


    @Override
    public void apply(OperationContext context) {
        Optional<ApiOperation> methodAnnotation = context.findAnnotation(ApiOperation.class);
        StringBuilder builder = new StringBuilder();
        if (methodAnnotation.isPresent()) {
            String apiNotes = this.descriptions.resolve(methodAnnotation.get().notes());
            if (StringUtils.hasText(apiNotes)) {
                builder.append(apiNotes).append("\n");
            }
        }
        //拼接subCode说明
        Optional<StlApiOperation> stlApiOperationOptional = context.findAnnotation(StlApiOperation.class);
        if (stlApiOperationOptional.isPresent()) {
            //解析notes字段
            String subCodeStr = this.resolveSubCode(stlApiOperationOptional.get());
            if (StringUtils.hasText(subCodeStr)) {
                builder.append(subCodeStr);
            }
        }
        //Optional<Decrypt> decryptOptional = context.findAnnotation(Decrypt.class);
        //if (decryptOptional.isPresent()) {
        //    builder.append("\n <b style='color:blue'>- 此接口需入参加密传入</b><br/>");
        //}
        //拼接subCode描述
        context.operationBuilder().notes(builder.toString());
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }

    /**
     * 拼接subcode
     *
     * @param stlApiOperation 自定义subCode注解
     * @return
     */
    @SneakyThrows
    private String resolveSubCode(StlApiOperation stlApiOperation) {
        StringBuilder builder = new StringBuilder();
        String tip = stlApiOperation.tip();
        if (!StringUtils.isEmpty(tip)) {
            builder.append(tip).append("</br>");
        }
        Class<? extends SubCode>[] classes = stlApiOperation.subCodeType();

        String[] codeTypeArr = stlApiOperation.codePrefix();
        String[] excludeArr = stlApiOperation.excludeSubCode();
        //先拼接通用的成功subCode
        builder.append(1).append(SUBCODE_SEQUENCE_SPLIT_CHAR).append(SystemServiceCode.SystemApi.SUCCESS.getSwaggerMsg()).append("</br>");
        //前面有一个通用成功的，其他的从2开始计数
        int seq = 2;
        for (Class<? extends SubCode> clazz : classes) {
            if (CommonConstants.NONE.equals(codeTypeArr[0])) {
                // 获取外部类StlServiceCode
                String[] split = StringUtils.split(clazz.getName(), CommonConstants.OUTER_CLASS_SPLIT);
                if (split == null || split.length != 2) {
                    continue;
                }
                Class<?> outerClass = Class.forName(split[0]);
                if (!outerClass.isAnnotationPresent(StlServiceCode.class)) {
                    continue;
                }
                StlServiceCode stlServiceCode = outerClass.getAnnotation(StlServiceCode.class);

                codeTypeArr[0] = stlServiceCode.code().getType();
            }
            SubCode[] sce = clazz.getEnumConstants();
            for (String code : codeTypeArr) {
                Map<String, String> msgMap = new HashMap<>(8);
                //单独将subCode和提示语扫描一次，如果有相同的subCode，提示语采取拼接的方式展示在同一行
                for (SubCode item : sce) {
                    //匹配前缀
                    String subCode = item.getSubCode();
                    if (!subCode.startsWith(code) || Arrays.stream(excludeArr).anyMatch(subCode::equalsIgnoreCase)) {
                        continue;
                    }
                    msgMap.compute(item.getSubCode(), (k, v) -> v == null ? item.getDesc() : v + " <font color='#FF1122'>或者</font> " + item.getDesc());
                }
                Set<String> existSubCode = new HashSet<>();
                for (SubCode item : sce) {
                    //匹配前缀
                    String subCode = item.getSubCode();
                    if (!subCode.startsWith(code) || Arrays.stream(excludeArr).anyMatch(subCode::equalsIgnoreCase)) {
                        continue;
                    }
                    //已经拼接过描述的subCode不继续拼接了
                    if (existSubCode.contains(subCode)) {
                        continue;
                    }
                    //拼接提示语
                    builder.append(seq)
                            .append(SUBCODE_SEQUENCE_SPLIT_CHAR)
                            .append(subCode)
                            .append("：")
                            .append(msgMap.get(subCode));
                    builder.append("</br>");
                    existSubCode.add(subCode);
                    seq++;
                }
            }
        }
        return builder.toString();
    }
}
