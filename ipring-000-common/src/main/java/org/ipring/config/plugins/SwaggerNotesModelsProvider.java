package org.ipring.config.plugins;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationModelsProviderPlugin;
import springfox.documentation.spi.service.contexts.RequestMappingContext;

import static springfox.documentation.swagger.common.SwaggerPluginSupport.pluginDoesApply;

/**
 * 加载swagger model插件
 *
 * @author lgj
 * @date 8/2/2023
 */
@ConditionalOnProperty(value = "ipring.swagger.enable", havingValue = "true")
@Component
@RequiredArgsConstructor
public class SwaggerNotesModelsProvider extends OperateParent implements OperationModelsProviderPlugin {

    private final TypeResolver typeResolver;

    @Override
    public void apply(RequestMappingContext context) {
        collectFromSwaggerNotes(context);
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return pluginDoesApply(delimiter);
    }

    private void collectFromSwaggerNotes(RequestMappingContext context) {
        ResolvedType returnType = context.getReturnType();
        returnType = context.alternateFor(returnType);
        //加载StlApiOperation的返回类型
        Optional<ResolvedType> returnParameter = context.findAnnotation(StlApiOperation.class)
                .transform(resolveTypeFromSwaggerNotes(typeResolver, returnType));
        if (returnParameter.isPresent() && returnParameter.get() != returnType) {
            context.operationModelsBuilder().addReturn(returnParameter.get());
        }
    }
}
