package org.ipring.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.ipring.anno.Scan;
import org.ipring.model.param.ScanMqEntity;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author Rainful
 * @date 2022/12/16 9:32
 */
@Component
@Aspect
public class ScanAspect {

    @AfterReturning("@annotation(scan)")
    public void UnusualAfter(JoinPoint joinPoint, Scan scan) {
        Object[] args = joinPoint.getArgs();
        if (Objects.isNull(args) || args.length == 0) return;
        for (Object arg : args) {
            if (arg instanceof ScanMqEntity) {
                ScanMqEntity scanMq = (ScanMqEntity) arg;
                // todo 发送MQ消息
            } else if (arg instanceof List) {
                List<ScanMqEntity> list = JsonUtils.toList(JsonUtils.toJson(arg), ScanMqEntity.class);
                // todo 发送MQ消息
            }
        }
    }
}
