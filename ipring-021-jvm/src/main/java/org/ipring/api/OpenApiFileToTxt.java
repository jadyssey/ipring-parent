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
public class OpenApiFileToTxt {
    public static void main(String[] args) {
        String openApiFilePath = "C:\\Users\\14308\\Downloads\\openapi.json"; // OpenAPI文件路径
        String txtFilePath = "paths_from_file.txt"; // 输出TXT路径

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(openApiFilePath));
            JsonNode pathsNode = rootNode.path("paths");

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            Files.newOutputStream(Paths.get(txtFilePath)),
                            StandardCharsets.UTF_8
                    )
            )) {
                writer.write("从OpenAPI文件解析的paths路径：");
                writer.newLine();
                writer.write("-------------------------------");
                writer.newLine();

                pathsNode.fields().forEachRemaining(entry -> {
                    try {
                        writer.write(entry.getKey());
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                System.out.println("路径已写入TXT：" + txtFilePath);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}