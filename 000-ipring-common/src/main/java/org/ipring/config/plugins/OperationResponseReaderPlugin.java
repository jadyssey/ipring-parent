package org.ipring.config.plugins;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.ipring.anno.StlApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

/**
 * swagger注解response返回类型补丁
 *
 * @author lgj
 * @date 8/2/2023
 */
@ConditionalOnProperty(value = "stl.swagger.enable", havingValue = "true")
@Component
@RequiredArgsConstructor
public class OperationResponseReaderPlugin extends OperateParent implements OperationBuilderPlugin {

    private final TypeResolver typeResolver;

    @Override
    public void apply(OperationContext context) {
        ResolvedType returnType = context.alternateFor(context.getReturnType());
        //获取返回类型
        returnType = context.findAnnotation(StlApiOperation.class)
                .transform(resolveTypeFromSwaggerNotes(typeResolver, returnType))
                .or(returnType);
        if (canSkip(context, returnType)) {
            return;
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
