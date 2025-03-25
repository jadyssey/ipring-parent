package org.ipring.util.qr;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.wechat_qrcode.WeChatQRCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 微信二维码扫描工具类[1,3](@ref)
 * 优化点：内存管理、异常处理、跨平台路径支持
 */
@Slf4j
public class WeChatQRUtil {
    private static final String MODEL_DIR = "wechatqrcode";
    private static final int MAX_IMAGE_SIZE = 1920;
    private static WeChatQRUtil INSTANCE;

    static {
        try {
            INSTANCE = new WeChatQRUtil();
        } catch (Exception e) {
            throw new RuntimeException("WeChatQRCode initialization failed", e);
        }
    }

    private final WeChatQRCode detector;

    /**
     * 私有构造器实现单例模式
     */
    @SneakyThrows
    private WeChatQRUtil() {
        log.info("WeChatQRUtil开始初始化");
        validateOpenCVVersion();
        log.info("WeChatQRUtil加载依赖完成");
        this.detector = initDetector();
    }

    /**
     * 获取单例实例（静态内部类方式实现线程安全）
     */
    public static WeChatQRUtil getInstance() {
        if (INSTANCE == null) {
            synchronized (WeChatQRUtil.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WeChatQRUtil();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Mat转BufferedImage（优化颜色空间转换）[2](@ref)
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

        BufferedImage image = new BufferedImage(
                rgbMat.cols(), rgbMat.rows(), BufferedImage.TYPE_3BYTE_BGR
        );
        rgbMat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        rgbMat.release();
        return image;
    }

    @SneakyThrows
    public static void main(String[] args) {
        BufferedImage image = ImageIO.read(new File("qrcode.jpg"));
        String result = WeChatQRUtil.getInstance().decode(image);
        System.out.println("Decoded: " + result);
    }

    /**
     * 初始化检测器[1,3](@ref)
     */
    /*private WeChatQRCode initDetector() throws IOException {
        ClassLoader cl = WeChatQRUtil.class.getClassLoader();
        Path modelPath = getModelPath(cl);

        return new WeChatQRCode(
                resolveModelPath(modelPath, "detect.prototxt"),
                resolveModelPath(modelPath, "detect.caffemodel"),
                resolveModelPath(modelPath, "sr.prototxt"),
                resolveModelPath(modelPath, "sr.caffemodel")
        );
    }*/
    private WeChatQRCode initDetector() throws IOException {
        ClassLoader cl = WeChatQRUtil.class.getClassLoader();

        // 创建临时目录存放模型文件
        Path tempDir = Files.createTempDirectory("wechatqrcode_");
        tempDir.toFile().deleteOnExit();

        return new WeChatQRCode(
                extractModel(cl, "detect.prototxt", tempDir),
                extractModel(cl, "detect.caffemodel", tempDir),
                extractModel(cl, "sr.prototxt", tempDir),
                extractModel(cl, "sr.caffemodel", tempDir)
        );
    }

    private String extractModel(ClassLoader cl, String fileName, Path tempDir) throws IOException {
        // 从resources加载流
        try (InputStream is = cl.getResourceAsStream(MODEL_DIR + "/" + fileName)) {
            if (is == null) {
                throw new IOException("Model file not found: " + fileName);
            }

            // 写入临时文件
            Path target = tempDir.resolve(fileName);
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        }
    }

    /**
     * 跨平台路径解析[3](@ref)
     */
    private Path getModelPath(ClassLoader cl) throws IOException {
        URL resource = cl.getResource(MODEL_DIR);
        if (resource == null) {
            throw new IOException("Model directory not found: " + MODEL_DIR);
        }
        return Paths.get(resource.getPath().substring(1));
    }

    private String resolveModelPath(Path basePath, String fileName) {
        return basePath.resolve(fileName).toString();
    }

    /**
     * 二维码识别主方法[1,2](@ref)
     */
    public String decode(BufferedImage srcImage) {
        Mat image = null;
        try {
            BufferedImage processedImage = preprocessImage(srcImage);
            image = bufferedImageToMat(processedImage);
            return detectAndDecode(image);
        } finally {
            if (image != null) image.release();
        }
    }

    /**
     * 图像预处理（尺寸调整+格式转换）
     */
    private BufferedImage preprocessImage(BufferedImage src) {
        BufferedImage converted = convertToCompatibleFormat(src);
        return resizeIfNeeded(converted);
    }

    /**
     * 转换为OpenCV兼容的BGR格式[2](@ref)
     */
    private BufferedImage convertToCompatibleFormat(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_3BYTE_BGR) return src;

        BufferedImage converted = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(src, 0, 0, null);
        return converted;
    }

    /**
     * 智能尺寸调整（保持宽高比）[1](@ref)
     */
    private BufferedImage resizeIfNeeded(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return original;
        }

        double ratio = Math.min(
                (double) MAX_IMAGE_SIZE / width,
                (double) MAX_IMAGE_SIZE / height
        );

        Mat srcMat = bufferedImageToMat(original);
        Mat dstMat = new Mat();
        Imgproc.resize(
                srcMat, dstMat,
                new Size(width * ratio, height * ratio),
                0, 0, Imgproc.INTER_AREA
        );
        srcMat.release();

        BufferedImage resized = matToBufferedImage(dstMat);
        dstMat.release();
        return resized;
    }

    /**
     * BufferedImage转Mat对象（带内存管理）[2](@ref)
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        int cvtype = CvType.CV_8UC3;
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            cvtype = CvType.CV_8UC1;
        }
        Mat mat = new Mat(image.getHeight(), image.getWidth(), cvtype);
        mat.put(0, 0, pixels);
        return mat;
    }

    /**
     * 核心识别方法[1,3](@ref)
     */
    private String detectAndDecode(Mat image) {
        try {
            List<String> results = detector.detectAndDecode(image);
            if (results.isEmpty()) return null;

            return results.stream()
                    .filter(s -> !s.isEmpty())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("QRCode detection failed | Size:{}x{} | Type:{}",
                    image.cols(), image.rows(), CvType.typeToString(image.type()), e);
            return null;
        }
    }

    /**
     * OpenCV版本校验[3](@ref)
     */
    private void validateOpenCVVersion() {
        if (Core.VERSION_MAJOR < 4 || (Core.VERSION_MAJOR == 4 && Core.VERSION_MINOR < 5)) {
            throw new UnsupportedOperationException(
                    "Requires OpenCV 4.5+, current version: " + Core.VERSION
            );
        }
    }
}