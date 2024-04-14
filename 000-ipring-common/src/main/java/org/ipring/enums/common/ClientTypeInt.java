package org.ipring.enums.common;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lgj
 * @date 8/2/2023
 **/

@Getter
@AllArgsConstructor
public enum ClientTypeInt implements IntEnumType {
    // 其他
    OTHER(0, "error"),
    IOS(1, "IOS"),
    ANDROID(2, "Android"),
    ADMIN_SYS(3, "后台管理"),
    WEB(4, "Web端"),
    H5(5, "H5"),
    SERVER(10, "server服务端"),
    ;

    private static final Map<Integer, ClientTypeInt> ALL_CLIENT_MAP = new HashMap<>();

    public static ClientTypeInt getByType(int type) {
        return ALL_CLIENT_MAP.get(type);
    }
    static {
        for (ClientTypeInt client : ClientTypeInt.values()) {
            ALL_CLIENT_MAP.put(client.getType(), client);
        }
    }


    private final Integer type;

    private final String description;
}
