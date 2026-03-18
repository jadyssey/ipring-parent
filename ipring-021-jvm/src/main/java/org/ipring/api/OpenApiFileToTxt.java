package org.ipring.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 解析OpenApi文件并输出所有接口URI
 */

/**
 * 解析JSON数组，提取所有接口name字段并输出到TXT
 */
public class OpenApiFileToTxt {
    public static void main(String[] args) {
        // 你的JSON文件路径
        String jsonFilePath = "D:\\downloads\\response-api.json";
        // 输出文件路径
        String txtFilePath = "paths_from_file.txt";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 读取根节点（JSON数组）
            JsonNode rootArray = objectMapper.readTree(new File(jsonFilePath));

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            Files.newOutputStream(Paths.get(txtFilePath)),
                            StandardCharsets.UTF_8
                    )
            )) {
                writer.write("从JSON文件解析的接口URI：");
                writer.newLine();
                writer.write("-------------------------------");
                writer.newLine();

                // 遍历数组每一项
                for (JsonNode node : rootArray) {
                    // 提取 name 字段
                    String name = node.path("name").asText();
                    if (name.contains(".") || name.contains("*") || name.startsWith("URI")) continue;
                    writer.write(name);
                    writer.newLine();
                }

                System.out.println("解析完成！接口URI已写入文件：" + txtFilePath);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}