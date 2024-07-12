package org.ipring.aspect;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.ipring.anno.Unusual;
import org.ipring.model.param.UserDTO;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rainful
 * @date 2022/12/16 9:32
 */
@Component
@Aspect
@Slf4j
public class UnusualAspect {

    @AfterReturning("@annotation(unusual)")
    public void UnusualAfter(JoinPoint joinPoint, Unusual unusual) {
        log.info("Executing method annotated with @Unusual");
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UserDTO) {
                UserDTO userDTO = (UserDTO) arg;
                System.out.println(userDTO);
            }
            if (arg instanceof List) {
                List<UserDTO> userDTOS = JsonUtils.toList(JsonUtils.toJson(arg), UserDTO.class);
            }
            System.out.println("Argument: " + arg);
        }
    }
}
