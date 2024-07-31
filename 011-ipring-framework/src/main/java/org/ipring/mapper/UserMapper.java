package org.ipring.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ipring.anno.Scan;
import org.ipring.model.param.UserDTO;

import java.util.List;

/**
 * @Author lgj
 * @Date 2024/7/11
 */
@Mapper
public interface UserMapper {

    @Scan
    String getUserNameById(Long id);

    @Scan
    String getUserByUser(UserDTO userDTO);

    /**
     * AfterReturning的切面可以拿到插入后的模型，并且模型中含有主键id
     * @param userDTO
     * @return
     */
    @Scan
    int insertUser(UserDTO userDTO);

    @Scan
    List<String> getUserByUserList(List<UserDTO> list);
}
