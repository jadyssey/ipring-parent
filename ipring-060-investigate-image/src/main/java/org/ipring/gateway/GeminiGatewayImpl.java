package org.ipring.gateway;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import lombok.extern.slf4j.Slf4j;
import org.ipring.model.ChatBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liuguangjin
 * @date 2025/1/8
 */
@Component
@Slf4j
public class GeminiGatewayImpl implements GeminiGateway {
    public static final String PROJECT_ID = "global-terrain-445809-f1";
    /**
     * 香港
     */
    public static final String LOCATION = "asia-east2";
    /**
     * 美国
     */
    public static final String US_LOCATION = "us-central1";

    @Override
    public GenerateContentResponse gemini15pro(ChatBody chatBody) {
        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {
            GenerativeModel model = new GenerativeModel("gemini-1.5-pro", vertexAi);
            List<Content> contents = new ArrayList<>();

            /**
             * 图片仅支持以下两种格式
             *
             * image/png
             * image/jpeg
             */
            String imageFormat = getImageFormat(chatBody.getImageUrl());
            contents.add(ContentMaker.fromMultiModalData(PartMaker.fromMimeTypeAndData("image/" + imageFormat, chatBody.getImageUrl()),
                    chatBody.getText()));
            return model.generateContent(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从给定的URL中提取图片格式。
     *
     * @param urlString 图片的URL字符串
     * @return 图片格式（不含点号），如果无法提取则返回null
     */
    public static String getImageFormat(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath(); // 获取路径部分
            // 获取最后一个斜杠之后的部分
            String filename = path.substring(path.lastIndexOf('/') + 1);
            // 获取最后一个点之后的扩展名
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < filename.length() - 1) {
                return filename.substring(lastDotIndex + 1).toLowerCase();
            } else {
                // 没有找到扩展名
                return "jpeg";
            }
        } catch (MalformedURLException e) {
            // 处理URL格式错误的情况
            e.printStackTrace();
            return "jpeg";
        }
    }
}
