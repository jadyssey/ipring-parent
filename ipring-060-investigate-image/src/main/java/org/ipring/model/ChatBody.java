package org.ipring.model;

import lombok.Data;

import java.util.List;

/**
 * @author liuguangjin
 * @date 2024/12/26
 */
@Data
public class ChatBody {
    private String model;

    /**
     * 1-chatgpt
     * 2-azure
     */
    private Integer supplier;

    private String systemSetup;

    private String text;
    private String jsonResponseFormat;

    private String imageUrl;
    private List<String> imageList;
}
