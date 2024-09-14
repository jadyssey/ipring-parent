package org.ipring.config.redisson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.util.RedissonUtil;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 为注解了 {@link Lock} 的方法加锁的切面
 *
 * @author lgj
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LockAspect {

    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser parser = new SpelExpressionParser();

    private final RedissonUtil redissonUtil;

    /**
     * 为方法加锁
     */
    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        String key;
        if (StringUtils.hasText(lock.suffix())) {
            // 有后缀, 需要解析 el 表达式
            // 获取方法的形参名称
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] params = discoverer.getParameterNames(signature.getMethod());
            // 获取方法的实际参数值
            Object[] arguments = joinPoint.getArgs();
            // 设置解析 SpEL 所需的数据上下文
            EvaluationContext context = new StandardEvaluationContext();
            if (Objects.nonNull(params) && Objects.nonNull(arguments)) {
                for (int i = 0; i < params.length; i++) {
                    context.setVariable(params[i], arguments[i]);
                }
            }
            // 解析表达式并获取 SpEL 的值
            Expression expression = parser.parseExpression(lock.suffix());
            Object suffix = expression.getValue(context);
            // 得到锁的键值
            key = lock.value() + suffix;
        } else {
            // 只有前缀, 无需解析 el 表达式
            key = lock.value();
        }
        if (!redissonUtil.tryLock(key, lock.waitTime(), lock.leaseTime())) {
            throw new ServiceException(SystemServiceCode.SystemApi.LOCK_FREQUENTLY_MIN);
        }
        try {
            return joinPoint.proceed();
        } finally {
            redissonUtil.unlock(key);
        }
    }
}
