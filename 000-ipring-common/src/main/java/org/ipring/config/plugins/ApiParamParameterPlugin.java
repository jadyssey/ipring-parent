package org.ipring.config.plugins;

import com.fasterxml.classmate.ResolvedType;
import com.google.common.base.Optional;
import org.ipring.anno.EnumValue;
import org.ipring.util.EnumValueUtils;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.schema.Collections;
import springfox.documentation.schema.Enums;
import springfox.documentation.schema.Example;
import springfox.documentation.service.AllowableValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.EnumTypeDeterminer;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.schema.ApiModelProperties;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.pluginDoesApply;
import static springfox.documentation.swagger.readers.parameter.Examples.examples;

/**
 * 注解支持枚举插件
 *
 * @author lgj
 * @date 8/2/2023
 * @see ApiParam
 */
@ConditionalOnProperty(value = "stl.swagger.enable", havingValue = "true")
@Configuration
@RequiredArgsConstructor
public class ApiParamParameterPlugin implements ParameterBuilderPlugin {

    private final DescriptionResolver descriptions;
    private final EnumTypeDeterminer enumTypeDeterminer;

    @Override
    public void apply(ParameterContext context) {
        Optional<EnumValue> enumValueOptional = context.resolvedMethodParameter().findAnnotation(EnumValue.class);
        if (!enumValueOptional.isPresent()) {
            return;
        }
        Optional<ApiParam> apiParam = context.resolvedMethodParameter().findAnnotation(ApiParam.class);
        context.parameterBuilder()
                .allowableValues(allowableValues(
                        context.alternateFor(context.resolvedMethodParameter().getParameterType()),
                        apiParam.transform(ApiParam::allowableValues).or("")));
        if (apiParam.isPresent()) {
            ApiParam annotation = apiParam.get();
            String notes = EnumValueUtils.appendEnumValue(enumValueOptional.get(), descriptions.resolve(annotation.value()));
            context.parameterBuilder().name(emptyToNull(annotation.name()))
                    .description(notes)
                    .parameterAccess(emptyToNull(annotation.access()))
                    .defaultValue(emptyToNull(annotation.defaultValue()))
                    .allowMultiple(annotation.allowMultiple())
                    .allowEmptyValue(annotation.allowEmptyValue())
                    .required(annotation.required())
                    .scalarExample(new Example(annotation.example()))
                    .complexExamples(examples(annotation.examples()))
                    .hidden(annotation.hidden())
                    .collectionFormat(annotation.collectionFormat())
                    .order(SWAGGER_PLUGIN_ORDER);
        }
    }

    private AllowableValues allowableValues(ResolvedType parameterType, String allowableValueString) {
        AllowableValues allowableValues = null;
        if (!isNullOrEmpty(allowableValueString)) {
            allowableValues = ApiModelProperties.allowableValueFromString(allowableValueString);
        } else {
            if (enumTypeDeterminer.isEnum(parameterType.getErasedType())) {
                allowableValues = Enums.allowableValues(parameterType.getErasedType());
            }
            if (Collections.isContainerType(parameterType)) {
                allowableValues = Enums.allowableValues(Collections.collectionElementType(parameterType).getErasedType());
            }
        }
        return allowableValues;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return pluginDoesApply(delimiter);
    }
}
