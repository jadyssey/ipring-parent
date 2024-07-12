package org.ipring.model.param;

import lombok.Data;

/**
 * @Author lgj
 * @Date 2024/7/12
 */
@Data
public class UserDTO {
    private Long userId;
    private String userName;
    private String pswd;

    public static UserDTO of(long l, String s) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(l);
        userDTO.setUserName(s);
        return userDTO;
    }
}
