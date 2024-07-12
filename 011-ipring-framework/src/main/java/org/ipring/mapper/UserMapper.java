package org.ipring.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ipring.anno.Unusual;
import org.ipring.model.param.UserDTO;

import java.util.List;

/**
 * @Author lgj
 * @Date 2024/7/11
 */
@Mapper
public interface UserMapper {

    @Unusual
    String getUserNameById(Long id);

    @Unusual
    String getUserByUser(UserDTO userDTO);

    @Unusual
    List<String> getUserByUserList(List<UserDTO> list);
}
