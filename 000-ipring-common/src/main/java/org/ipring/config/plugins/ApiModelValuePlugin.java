package org.ipring.config.plugins;

import com.google.common.base.Optional;
import org.ipring.anno.EnumValue;
import org.ipring.util.EnumValueUtils;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.Annotations;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.schema.ApiModelProperties;

/**
 * ApiProperty注解value属性插件
 *
 * @author lgj
 * @date 8/2/2023
 */
@ConditionalOnProperty(value = "stl.swagger.enable", havingValue = "true")
@Component
public class ApiModelValuePlugin implements ModelPropertyBuilderPlugin {

    @Override
    public void apply(ModelPropertyContext context) {
        Optional<ApiModelProperty> annotation = Optional.absent();
        if (context.getAnnotatedElement().isPresent()) {
            annotation = annotation.or(ApiModelProperties.findApiModePropertyAnnotation(context.getAnnotatedElement().get()));
        }
        if (context.getBeanPropertyDefinition().isPresent()) {
            annotation = annotation.or(Annotations.findPropertyAnnotation(context.getBeanPropertyDefinition().get(), ApiModelProperty.class));
        }
        //@ApiModelProperty注解不存在则不去拼枚举值
        if (!annotation.isPresent()) {
            return;
        }
        String notes = annotation.get().value();
        EnumValue enumValue = context.getBeanPropertyDefinition().get().getField().getAnnotation(EnumValue.class);
        if (null != enumValue) {
            notes = EnumValueUtils.appendEnumValue(enumValue, notes);
        }
        context.getBuilder().description(notes);
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
