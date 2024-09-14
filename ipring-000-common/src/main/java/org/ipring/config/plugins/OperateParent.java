package org.ipring.config.plugins;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.common.ResponseContainerType;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.util.List;
import java.util.Set;

/**
 * 插件父类
 *
 * @author lgj
 * @date 8/2/2023
 */
@Slf4j
public class OperateParent {

    /**
     * 获取SwaggerNotes中的返回类型
     *
     * @return
     */
    protected static Function<StlApiOperation, ResolvedType> resolveTypeFromSwaggerNotes(final TypeResolver typeResolver,
                                                                                         ResolvedType defaultType) {
        return input -> getResolvedType(input, typeResolver, defaultType);
    }


    /**
     * 获取返回类型并包装
     *
     * @param annotation
     * @param resolver
     * @param defaultType
     * @return
     */
    private static ResolvedType getResolvedType(StlApiOperation annotation, TypeResolver resolver, ResolvedType defaultType) {
        if (annotation == null) {
            return defaultType;
        }
        Class<?> response = annotation.response();
        ResponseContainerType responseContainerType = annotation.responseContainer();
        Optional<ResolvedType> resolvedType = resolvedType(resolver, response, responseContainerType);
        if (resolvedType.isPresent()) {
            return resolvedType.get();
        } else {
            return defaultType;
        }
    }

    /**
     * 解析返回类型
     *
     * @param resolver
     * @param response
     * @param responseContainerType
     * @return
     */
    protected static Optional<ResolvedType> resolvedType(TypeResolver resolver, Class<?> response, ResponseContainerType responseContainerType) {
        if (!isNotVoid(response)) {
            return Optional.absent();
        }
        //设置返回容器类型
        switch (responseContainerType) {
            case LIST:
                return Optional.of(resolver.resolve(List.class, response));
            case SET:
                return Optional.of(resolver.resolve(Set.class, response));
            default:
                return Optional.of(resolver.resolve(response));
        }
    }

    /**
     * 判断是否无返回
     *
     * @param response
     * @return
     */
    private static boolean isNotVoid(Class<?> response) {
        return Void.class != response && void.class != response;
    }

    protected boolean canSkip(OperationContext context, ResolvedType returnType) {
        return context.getIgnorableParameterTypes().contains(returnType.getErasedType());
    }
}
