package org.ipring.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;

public class ImageDownloader {
    public static void downloadImage(String imageUrl, String imgPath) {
        try {
            // 解析 URL 获取文件名
            URL url = new URL(imageUrl);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());
            String filename = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);

            // 构造保存路径（这里示例保存到当前目录的 downloads 文件夹）
            String savePath = imgPath + "/" + filename;
            File file1 = new File(imgPath);// 创建保存目录
            if (!file1.exists()) {
                file1.mkdir();
            }
            // 读取图片并保存
            BufferedImage bufferedImage = ImageIO.read(url);
            if (bufferedImage != null) {
                String formatName = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                File file = new File(savePath);
                ImageIO.write(bufferedImage, formatName, file);
                System.out.println("图片已保存到：" + savePath);
            } else {
                System.out.println("无法读取图片内容");
            }
        } catch (Exception e) {
            System.out.println("下载失败：" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String imageUrl = "https://gofo-sys-admin.s3.us-west-2.amazonaws.com/sys-mod-file/2025-03-19/app-file/17424428753057E47FECF-4B30-4730-BA43-7550D8C9731E-8186-0000085CE773D3F9.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250320T112305Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604800&X-Amz-Credential=AKIAR234HW752KIISC4O%2F20250320%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Signature=7ef37792a9c2d705193fce4f5a0e7c201438decfd0c1ed81e1a452ee802abd4a";
        downloadImage(imageUrl, "img");
    }
}