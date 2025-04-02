package org.ipring.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AliImgTokenTest {

    // 自定义类存储调整后的尺寸
    public static class ResizedSize {
        public final int height;
        public final int width;

        public ResizedSize(int height, int width) {
            this.height = height;
            this.width = width;
        }
    }

    public static ResizedSize smartResize(String imagePath) throws IOException {
        // 1. 加载图像
        BufferedImage image = ImageIO.read(new File(imagePath));
        if (image == null) {
            throw new IOException("无法加载图像文件: " + imagePath);
        }

        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();

        final int minPixels = 28 * 28 * 4;
        final int maxPixels = 1280 * 28 * 28;
        // 2. 初始调整为28的倍数
        int hBar = (int) (Math.round(originalHeight / 28.0) * 28);
        int wBar = (int) (Math.round(originalWidth / 28.0) * 28);
        int currentPixels = hBar * wBar;

        // 3. 根据条件调整尺寸
        if (currentPixels > maxPixels) {
            // 当前像素超过最大值，需要缩小
            double beta = Math.sqrt(
                    (originalHeight * (double) originalWidth) / maxPixels
            );
            double scaledHeight = originalHeight / beta;
            double scaledWidth = originalWidth / beta;

            hBar = (int) (Math.floor(scaledHeight / 28) * 28);
            wBar = (int) (Math.floor(scaledWidth / 28) * 28);
        } else if (currentPixels < minPixels) {
            // 当前像素低于最小值，需要放大
            double beta = Math.sqrt(
                    (double) minPixels / (originalHeight * originalWidth)
            );
            double scaledHeight = originalHeight * beta;
            double scaledWidth = originalWidth * beta;

            hBar = (int) (Math.ceil(scaledHeight / 28) * 28);
            wBar = (int) (Math.ceil(scaledWidth / 28) * 28);
        }

        return new ResizedSize(hBar, wBar);
    }

    public static void main(String[] args) {
        try {
            ResizedSize size = smartResize(
                    // xxx/test.png替换为你的图像路径
                    "D:\\img\\imgs\\1742435411260BB759159-E6F0-4956-BA1F-6DFB77D3A746.jpg"
            );

            System.out.printf("缩放后的图像尺寸：高度 %d，宽度 %d%n", size.height, size.width);

            // 计算 Token（总像素 / 28×28 + 2）
            int token = (size.height * size.width) / (28 * 28) + 2;
            System.out.printf("图像总 Token 数：%d%n", token);

        } catch (IOException e) {
            System.err.println("错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
}