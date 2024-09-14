package org.ipring.model.param;

import lombok.Data;

/**
 * @Author lgj
 * @Date 2024/7/12
 */
@Data
public class UserDTO {
    private Long id;
    private String name;
    private String phone;

    public static UserDTO of(long l, String s) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(l);
        userDTO.setName(s);
        return userDTO;
    }

    public static UserDTO of(String name, String phone) {
        UserDTO userDTO = new UserDTO();
        userDTO.setName(name);
        userDTO.setPhone(phone);
        return userDTO;
    }
}
