package org.ipring.model;

import lombok.Data;

/**
 * @author liuguangjin
 * @date 2/26/2025
 */
@Data
public class ImgDecodeResp {
    private String weChatQRCodeTool;
    private String googleZxing;
    private String customDecodeUtil;
    private String customDecodeUtilV2;
}
