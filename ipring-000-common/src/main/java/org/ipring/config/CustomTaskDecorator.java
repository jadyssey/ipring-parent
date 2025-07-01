package org.ipring.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

public class CustomTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 捕获主线程的请求上下文
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        // 可选：捕获MDC日志上下文（如traceId）
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                // 2. 将主线程数据注入子线程
                RequestContextHolder.setRequestAttributes(attributes);
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext); // 传递日志跟踪ID
                }
                // 3. 执行异步任务
                runnable.run();
            } finally {
                // 4. 清理子线程数据，避免内存泄漏
                RequestContextHolder.resetRequestAttributes();
                MDC.clear();
            }
        };
    }
}