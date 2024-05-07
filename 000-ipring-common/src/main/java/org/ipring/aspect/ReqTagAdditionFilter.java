package org.ipring.aspect;

import org.ipring.util.HttpUtils;
import org.ipring.util.UUIDUtil;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;

import static org.ipring.constant.LogConstant.REQ_IP;
import static org.ipring.constant.LogConstant.TRACE_ID;

/**
 * @author: lgj
 * @date: 2023/05/11 17:52
 * @description:
 */

@Component
@Order(10)
public class ReqTagAdditionFilter extends OncePerRequestFilter implements Filter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put(TRACE_ID, UUIDUtil.generateTraceId());
            MDC.put(REQ_IP, HttpUtils.getReqIp());

            filterChain.doFilter(new ContentCachingRequestWrapper(request), response);
        } finally {
            MDC.clear();
        }
    }
}
