package org.ipring.sender.impl;

import org.ipring.sender.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: lgj
 * @date: 2024/04/03 16:07
 * @description:
 */
@Slf4j
@Component
public class DingDingNoticeService implements NoticeService<String> {

    @Override
    public void notice(String msg) {
        log.info("钉钉 msg:{}", msg);
    }
}
