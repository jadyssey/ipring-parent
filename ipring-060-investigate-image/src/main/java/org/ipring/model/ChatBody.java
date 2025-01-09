package org.ipring.model;

import lombok.Data;

import java.util.List;

/**
 * @author liuguangjin
 * @date 2024/12/26
 */
@Data
public class ChatBody {
    private String text;
    private String imageUrl;
    private List<String> imageList;
}
