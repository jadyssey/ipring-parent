package org.ipring.service;


import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
public class TaskService implements Serializable {
    private final static String USER_CODE = "wangwu-task";

    public String getAssignee() {
        log.info("获取经办人姓名");
        return USER_CODE;
    }
}
