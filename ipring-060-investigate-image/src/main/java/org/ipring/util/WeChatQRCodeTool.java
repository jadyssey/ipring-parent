package org.ipring.util;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.net.URL;
import java.util.List;

/**
 * @program: test-wechatqrcode
 * @description: 微信二维码扫描opencv库方法调用
 * @author: test
 * @create: 221-08-21 17:05 * 原创点赞：java+opencv4.5.3+wechatqrcode代码细节和 自编译类库(带下载地址) - 断舍离-重学JAVA之路 - 博客园 (cnblogs.com)
 **/
public class WeChatQRCodeTool {
    private static volatile WeChatQRCodeTool instance;
    private static volatile org.opencv.wechat_qrcode.WeChatQRCode detector;

    private WeChatQRCodeTool() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // 微信提供的4个模型配置文件放在resource/wechatqrcode文件夹里,　　　　　　//下载地址：WeChatCV/opencv_3rdparty: OpenCV - 3rdparty (github.com)
        ClassLoader cl = WeChatQRCodeTool.class.getClassLoader();
        URL detectprototxt = cl.getResource("wechatqrcode/detect.prototxt");
        URL detectcaffemodel = cl.getResource("wechatqrcode/detect.caffemodel");
        URL srprototxt = cl.getResource("wechatqrcode/sr.prototxt");
        URL srcaffemodel = cl.getResource("wechatqrcode/sr.caffemodel");        // 实例化微信二维码扫描对象　　　　　//如果打成jar，那么路径需要换到外部磁盘存储目录。
        detector = new org.opencv.wechat_qrcode.WeChatQRCode(detectprototxt.getPath().substring(1),
                // 因为使用的getResource方法获取的是URL对象，而这个构造方法里需要传入File的路径，所以substring1去掉/D:/xx开头的/
                detectcaffemodel.getPath().substring(1), srprototxt.getPath().substring(1), srcaffemodel.getPath().substring(1));

    }

    public static WeChatQRCodeTool getInstance() {
        if (instance == null) {
            synchronized (WeChatQRCodeTool.class) {
                if (instance == null) {
                    instance = new WeChatQRCodeTool();
                }
            }
        }
        return instance;
    }

    /**
     * 这里提供一个bufferimage的入参。如果是File对象，可以用ImageIO.read()获取一下bufferimage.    *原创点赞：
     */
    public String decode(BufferedImage srcImage) {
        int cvtype = CvType.CV_8UC3;
        if (srcImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            cvtype = CvType.CV_8UC1;
        }
        Mat image = bufImg2Mat(srcImage, srcImage.getType(), cvtype);       // 返回解析的字符串，如果图片有多个二维码，则会返回多个。
        List<String> result2 = detector.detectAndDecode(image);
        if (result2 != null && result2.size() > 0) {
            return result2.get(0);
        }
        return null;
    }


    /**
     * BufferedImage转换成Mat     *
     *
     * @param original 要转换的BufferedImage
     * @param imgType  bufferedImage的类型 如 BufferedImage.TYPE_3BYTE_BGR
     * @param matType  转换成mat的type 如 CvType.CV_8UC3　　　* 这里如果不懂这些参数的话，建议写死：BufferedImage.TYPE_3BYTE_BGR, CvType.CV_8UC3
     */
    public static Mat bufImg2Mat(BufferedImage original, int imgType, int matType) {
        if (original == null) {
            throw new IllegalArgumentException("original == null");
        }
        byte[] pixels = null;
        // Don't convert if it already has correct type
        if (original.getType() != imgType) {

            // Create a buffered image
            BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), imgType);

            // Draw the image onto the new buffer
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.drawImage(original, 0, 0, null);
                pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            } finally {
                g.dispose();
            }
        } else {
            pixels = ((DataBufferByte) original.getRaster().getDataBuffer()).getData();
        }
        Mat mat = Mat.eye(original.getHeight(), original.getWidth(), matType);
        mat.put(0, 0, pixels);
        return mat;
    }

    /**
     * Mat转换为BufferedImage
     */
    public static BufferedImage mat2img(Mat mat) {
        int dataSize = mat.cols() * mat.rows() * (int) mat.elemSize();
        byte[] data = new byte[dataSize];
        mat.get(0, 0, data);
        int type = mat.channels() == 1 ? 10 : 5;
        if (type == 5) {
            for (int i = 0; i < dataSize; i += 3) {
                byte blue = data[(i)];
                data[(i)] = data[(i + 2)];
                data[(i + 2)] = blue;
            }
        }
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    /**
     * Mat转换为BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat.height() > 0 && mat.width() > 0) {
            BufferedImage image = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);
            WritableRaster raster = image.getRaster();
            DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
            byte[] data = dataBuffer.getData();
            mat.get(0, 0, data);
            return image;
        }

        return null;
    }

}