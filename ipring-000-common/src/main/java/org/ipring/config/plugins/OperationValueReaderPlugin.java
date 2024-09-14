package org.ipring.config.plugins;

import com.google.common.base.Optional;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

/**
 * swagger注解value值补丁
 *
 * @author lgj
 * @date 8/2/2023
 */
@ConditionalOnProperty(value = "ipring.swagger.enable", havingValue = "true")
@Component
@RequiredArgsConstructor
public class OperationValueReaderPlugin implements OperationBuilderPlugin {

    private final DescriptionResolver descriptions;

    @Override
    public void apply(OperationContext context) {
        Optional<ApiOperation> methodAnnotation = context.findAnnotation(ApiOperation.class);
        StringBuilder titleBuffer = new StringBuilder();
        if (methodAnnotation.isPresent()) {
            String apiValue = this.descriptions.resolve(methodAnnotation.get().value());
            if (!StringUtils.isEmpty(apiValue)) {
                titleBuffer.append(apiValue);
            }
        }
        Optional<StlApiOperation> swaggerNotesOptional = context.findAnnotation(StlApiOperation.class);
        if (swaggerNotesOptional.isPresent()) {
            String title = swaggerNotesOptional.get().title();
            if (StringUtils.hasText(title)) {
                if (StringUtils.hasText(titleBuffer.toString())) {
                    titleBuffer.append("--").append(title);
                } else {
                    titleBuffer.append(title);
                }
            }
        }
        context.operationBuilder().summary(titleBuffer.toString());
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
