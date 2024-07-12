package org.ipring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.mapper.UserMapper;
import org.ipring.model.param.UserDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @Author lgj
 * @Date 2024/7/11
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DemoServiceImpl implements DemoService {
    private final UserMapper userMapper;
    @Override
    public void getUserNameById() {
        int res = userMapper.insertUser(UserDTO.of("小宝", "775"));
        String userNameById = userMapper.getUserNameById(1L);
        String userNameByUser = userMapper.getUserByUser(UserDTO.of(1L, ""));
        List<String> userList = userMapper.getUserByUserList(Collections.singletonList(UserDTO.of(1L, "")));
        log.info("查询到的用户姓名为：{}", userNameById);
        log.info("查询到的用户姓名为：{}", userNameByUser);

    }
}
