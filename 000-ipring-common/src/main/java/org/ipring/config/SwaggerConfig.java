package org.ipring.config;

import org.ipring.anno.StlApiOperation;
import org.ipring.constant.AuthConstant;
import org.ipring.constant.properties.EnvProperties;
import org.ipring.constant.properties.SwaggerProperties;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.util.EnumValueUtils;
import lombok.RequiredArgsConstructor;
import org.ipring.enums.common.ClientTypeInt;
import org.ipring.enums.common.LangTypeStr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.schema.ApiModelProperties;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lgj
 * @date 8/2/2023
 **/
@RequiredArgsConstructor
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;

    private final EnvProperties envProperties;

    private static final String TITLE = "%s API，当前环境：%s";

    private static final String HEADER_NAME = "header";
    private static final String STRING_NAME = "string";
    private static final String INTEGER_NAME = "int";
    private static final String TRUE = "true";

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(TRUE.equalsIgnoreCase(swaggerProperties.getEnable()))
                .groupName(swaggerProperties.getName())
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(StlApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(this.setHeaderToken());

    }

    private ApiInfo apiInfo() {
        String title = String.format(TITLE, swaggerProperties.getName(), envProperties.getName());
        return new ApiInfoBuilder().title(title)
                .description("接口文档")
                .version(swaggerProperties.getVersion())
                .build();
    }

    private List<Parameter> setHeaderToken() {
        List<Parameter> pars = new ArrayList<>();

        pars.add(new ParameterBuilder().name(AuthConstant.CLIENT_TYPE_HEADER).description(EnumValueUtils.appendEnumValue(ClientTypeInt.class, "客户端类型"))
                .order(Ordered.LOWEST_PRECEDENCE)
                .defaultValue(String.valueOf(ClientTypeInt.OTHER.getType()))
                .modelRef(new ModelRef(STRING_NAME)).parameterType(HEADER_NAME)
                .allowableValues(
                        ApiModelProperties.allowableValueFromString(
                                EnumValueUtils.getEnumAllowableValues(ClientTypeInt.class)
                        )
                )
                .required(true).build());

        //pars.add(new ParameterBuilder().name(AuthConstant.PRODUCT_TYPE_HEADER).description(EnumValueUtils.appendEnumValue(ProductTypeInt.class, "产品类型"))
        //        .order(Ordered.LOWEST_PRECEDENCE)
        //        .defaultValue(String.valueOf(ProductTypeInt.BROKERS_VIEW.getType()))
        //        .modelRef(new ModelRef(STRING_NAME)).parameterType(HEADER_NAME)
        //        .required(true)
        //        .allowableValues(
        //                ApiModelProperties.allowableValueFromString(
        //                        EnumValueUtils.getEnumAllowableValues(ProductTypeInt.class)
        //                )
        //        )
        //        .build());

        pars.add(new ParameterBuilder().name(AuthConstant.LANGUAGE_HEADER).description(EnumValueUtils.appendEnumValue(LangTypeStr.class, "语言类型（大小写不敏感）"))
                .order(Ordered.LOWEST_PRECEDENCE)
                .defaultValue(LangTypeStr.ZH_CN.getType())
                .modelRef(new ModelRef(STRING_NAME)).parameterType(HEADER_NAME)
                .allowableValues(
                        ApiModelProperties.allowableValueFromString(
                                EnumValueUtils.getEnumAllowableValues(LangTypeStr.class)
                        )
                )
                .required(true).build());

        pars.add(new ParameterBuilder().name(AuthConstant.FROM_SWAGGER).description(EnumValueUtils.appendEnumValue(BoolTypeInt.class, "是否来自Swagger访问"))
                .order(Ordered.LOWEST_PRECEDENCE)
                .defaultValue(String.valueOf(BoolTypeInt.YES.getType()))
                .modelRef(new ModelRef(STRING_NAME)).parameterType(HEADER_NAME)
                .allowableValues(
                        ApiModelProperties.allowableValueFromString(
                                EnumValueUtils.getEnumAllowableValues(BoolTypeInt.class)
                        )
                )
                .required(false).build());
        pars.add(new ParameterBuilder().name(AuthConstant.HEADER_UID).description("用户Id")
                .order(Ordered.LOWEST_PRECEDENCE)
                .defaultValue("0")
                .modelRef(new ModelRef(INTEGER_NAME)).parameterType(HEADER_NAME)
                .required(false).build());
        return pars;
    }
}
