package org.ipring.util.img;

import cn.hutool.extra.qrcode.QrCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ipring.util.qr.ImageHandlerUtil;
import org.ipring.util.qr.WeChatQRUtil;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

/**
 * @author liuguangjin
 * @date 2025/3/13
 */
@Slf4j
public class QrComplexDecodeUtil {
    /**
     * 多轮识别
     */
    public static String decode(BufferedImage bufferedImage) {
        Mat image = null;
        Mat mat = null;
        Mat mat2 = null;
        try {
            image = WeChatQRUtil.bufferedImageToMat(bufferedImage);

            // 第一次识别
            String qrCode = multiDecodeQRCode(image);

            if (StringUtils.isNotBlank(qrCode)) {
                return qrCode;
            }
            mat = ImageHandlerUtil.findQRCodeAndCut(image);
            if (mat == null) {
                log.debug("无法定位和识别到二维码");
                return null;
            }
            // 第二次识别
            qrCode = multiDecodeQRCode(mat);
            if (StringUtils.isNotBlank(qrCode)) {
                return qrCode;
            }
            mat2 = ImageHandlerUtil.processAndThresholdImage(mat);
            // 第三次识别
            qrCode = multiDecodeQRCode(mat2);
            if (StringUtils.isNotBlank(qrCode)) {
                return qrCode;
            }

            // ImageHandlerUtil.createCLAHE(mat);
            // 第四次识别
            // qrCode = multiDecodeQRCode(mat);
            log.info("多次都无法识别到有效二维码");
            return qrCode;
        } catch (Exception e) {
            log.error("识别报错：", e);
            return null;
        } finally {
            // 确保所有资源最终被释放
            if (image != null) image.release();
            if (mat != null) mat.release();
            if (mat2 != null) mat2.release();
        }
    }

    /**
     * 解析读取二维码
     * 先使用ZXING二维码识别，若失败，使用OPENCV自带的二维码识别
     *
     * @param matImg 二维码图片数据
     * @return 成功返回二维码识别结果，失败返回null
     */
    private static String multiDecodeQRCode(Mat matImg) {
        if (matImg == null) return null;

        String qrCodeText;
        BufferedImage bufferedImage = WeChatQRUtil.matToBufferedImage(matImg);
        qrCodeText = QrCodeUtil.decode(bufferedImage);
        if (StringUtils.isBlank(qrCodeText)) {
            qrCodeText = WeChatQRUtil.getInstance().decode(bufferedImage);
        }
        return qrCodeText;
    }
}
