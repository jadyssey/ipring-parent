package org.ipring.model;

import lombok.Data;

@Data
public class ImageExplanationRequest {

    private String type;
    private ImageReq image_url;
    private String text;
}
