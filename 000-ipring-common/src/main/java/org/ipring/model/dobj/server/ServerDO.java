package org.ipring.model.dobj.server;

import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:36
 * @description:
 */
@Data
public class ServerDO {

    private Integer brokerId;
    private String brokerName;
    private Integer serverId;
    private String serverName;
    private String addr;
    private String logo;
}
