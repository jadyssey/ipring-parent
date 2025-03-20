package org.ipring.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.opencv.utils.Converters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;


/**
 * @author liuguangjin
 * @date 2/26/2025
 */
public class CustomFileDecodeUtil {
    private static final int BIGGER_TIMES = 2;
    private static final String TEMP_PATH = "D:\\img\\" + "temp.jpg";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // 打印OpenCV版本信息以确认加载成功
//        System.out.println("OpenCV version: " + Core.VERSION);
    }

    /**
     * 解析读取二维码
     * 先使用ZXING二维码识别，若失败，使用OPENCV自带的二维码识别
     * 但不进行图像优化效果都不怎么好）
     *
     * @param filename 二维码图片路径
     * @return 成功返回二维码识别结果，失败返回null
     */
    @SneakyThrows
    public static String decodeQRcode(QRCodeDetector detector, String filename) {
        String qrCodeText = null;
        BufferedImage image = ImageIO.read(new File(filename));
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            qrCodeText = result.getText();
        } catch (Exception e) {
            qrCodeText = detector.detectAndDecode(Imgcodecs.imread(filename, 1));
            if (StringUtils.isBlank(qrCodeText)) {
                WeChatQRCodeTool tool = WeChatQRCodeTool.getInstance();
                //  WeChatQRCodeTool 解码二维码
                qrCodeText = tool.decode(image);
            }
        }
        return qrCodeText;
    }

    /**
     * 二维码识别与截取
     *
     * @param filename
     */
    public static void findQRcodeAndCut(String filename) {
        Mat src_gray = new Mat();
        Mat src = Imgcodecs.imread(filename, 1);
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> markContours = new ArrayList<>();
        // 图片放大
        if (src.width() * src.height() < 90000) {
            Imgproc.resize(src, src, new Size(800, 600));
        }
        // 彩色图转灰度图
        Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);
        // 对图像进行平滑处理
        Imgproc.GaussianBlur(src_gray, src_gray, new Size(3, 3), 0);
        Imgproc.Canny(src_gray, src_gray, 112, 255);

        Mat hierarchy = new Mat();
        Imgproc.findContours(src_gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f newMtx = new MatOfPoint2f(contours.get(i).toArray());
            RotatedRect rotRect = Imgproc.minAreaRect(newMtx);
            double w = rotRect.size.width;
            double h = rotRect.size.height;
            double rate = Math.max(w, h) / Math.min(w, h);
            // 长短轴比小于1.3，总面积大于60
            if (rate < 1.3 && w < src_gray.cols() / 4 && h < src_gray.rows() / 4
                    && Imgproc.contourArea(contours.get(i)) > 60) {
                // 计算层数，二维码角框有五层轮廓（或六层），这里不计自己这一层，有4个以上子轮廓则标记这一点
                double[] ds = hierarchy.get(0, i);
                if (ds != null && ds.length > 3) {
                    int count = 0;
                    // 排除最外层轮廓
                    if (ds[3] == -1) {
                        continue;
                    }
                    // 计算所有子轮廓数量
                    while ((int) ds[2] != -1) {
                        ++count;
                        ds = hierarchy.get(0, (int) ds[2]);
                    }
                    if (count >= 4) {
                        markContours.add(contours.get(i));
                    }
                }
            }
        }

        /***
         * 二维码有三个角轮廓，正常需要定位三个角才能确定坐标，但在这里当识别到两个点的时候也将二维码定位出来：
         *  当识别到三个点时,根据三个点定位可以确定二维码位置和形状，根据三个点组成三角形形状最大角角度判断是不是二维码的三个角
         *  当识别到两个点时，取两个点中间点，往四周扩散截取 当小于两个点时，直接返回
         */
        if (markContours.size() == 0) {
        } else if (markContours.size() == 1) {
            capture(markContours.get(0), src);
        } else if (markContours.size() == 2) {
            List<MatOfPoint> threePointList = new ArrayList<>();
            threePointList.add(markContours.get(0));
            threePointList.add(markContours.get(1));
            capture(threePointList, src);
        } else {
            for (int i = 0; i < markContours.size() - 2; i++) {
                List<MatOfPoint> threePointList = new ArrayList<>();
                for (int j = i + 1; j < markContours.size() - 1; j++) {
                    for (int k = j + 1; k < markContours.size(); k++) {
                        threePointList.add(markContours.get(i));
                        threePointList.add(markContours.get(j));
                        threePointList.add(markContours.get(k));
                        capture(threePointList, src, i + "-" + j + "-" + k);
                        threePointList.clear();
                    }
                }
            }
        }
    }

    /**
     * 针对对比度不高的图片，只能识别到一个角的，直接以该点为中心截取
     *
     * @param matOfPoint
     * @param src
     */
    private static void capture(MatOfPoint matOfPoint, Mat src) {
        Point centerPoint = centerCal(matOfPoint);
        int width = 200;
        Rect roiArea = new Rect(Math.max((int) (centerPoint.x - width), 0),
                Math.max((int) (centerPoint.y - width), 0), (2 * width), (2 * width));
        // 截取二维码
        Mat dstRoi = new Mat(src, roiArea);
        // 放大图片
        Imgproc.resize(dstRoi, dstRoi, new Size(BIGGER_TIMES * width, BIGGER_TIMES * width));
        // 保存到临时存放的文件中
        Imgcodecs.imwrite(TEMP_PATH, dstRoi);
    }

    /**
     * 当只识别到二维码的两个定位点时，根据两个点的中点进行定位
     *
     * @param threePointList
     * @param src
     */
    private static void capture(List<MatOfPoint> threePointList, Mat src) {
        Point p1 = centerCal(threePointList.get(0));
        Point p2 = centerCal(threePointList.get(1));
        Point centerPoint = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        double width = Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y) + 50;
        // 设置截取规则
        Rect roiArea = new Rect(Math.max((int) (centerPoint.x - width), 0),
                Math.max((int) (centerPoint.y - width), 0), (int) (2 * width),
                (int) (2 * width));
        // 截取二维码
        Mat dstRoi = new Mat(src, roiArea);
        // 放大图片
        Imgproc.resize(dstRoi, dstRoi, new Size(BIGGER_TIMES * width, BIGGER_TIMES * width));
        // 保持到临时文件中
        Imgcodecs.imwrite(TEMP_PATH, dstRoi);
    }


    /**
     * 对图片进行矫正，裁剪
     *
     * @param contours
     * @param src
     * @param idx
     */
    private static void capture(List<MatOfPoint> contours, Mat src, String idx) {
        Point[] pointthree = new Point[3];
        for (int i = 0; i < 3; i++) {
            pointthree[i] = centerCal(contours.get(i));
        }
        double[] ca = new double[2];
        double[] cb = new double[2];

        ca[0] = pointthree[1].x - pointthree[0].x;
        ca[1] = pointthree[1].y - pointthree[0].y;
        cb[0] = pointthree[2].x - pointthree[0].x;
        cb[1] = pointthree[2].y - pointthree[0].y;
        // angle1，angle2，angle3分别对应识别到的二维码定位角的三个点所组成三角形的三个角
        double angle1 = 180 / 3.1415 * Math.acos((ca[0] * cb[0] + ca[1] * cb[1])
                / (Math.sqrt(ca[0] * ca[0] + ca[1] * ca[1]) * Math.sqrt(cb[0] * cb[0] + cb[1] * cb[1])));
        double ccw1;
        if (ca[0] * cb[1] - ca[1] * cb[0] > 0) {
            ccw1 = 0;
        } else {
            ccw1 = 1;
        }
        ca[0] = pointthree[0].x - pointthree[1].x;
        ca[1] = pointthree[0].y - pointthree[1].y;
        cb[0] = pointthree[2].x - pointthree[1].x;
        cb[1] = pointthree[2].y - pointthree[1].y;
        double angle2 = 180 / 3.1415 * Math.acos((ca[0] * cb[0] + ca[1] * cb[1])
                / (Math.sqrt(ca[0] * ca[0] + ca[1] * ca[1]) * Math.sqrt(cb[0] * cb[0] + cb[1] * cb[1])));
        double ccw2;
        if (ca[0] * cb[1] - ca[1] * cb[0] > 0) {
            ccw2 = 0;
        } else {
            ccw2 = 1;
        }

        ca[0] = pointthree[1].x - pointthree[2].x;
        ca[1] = pointthree[1].y - pointthree[2].y;
        cb[0] = pointthree[0].x - pointthree[2].x;
        cb[1] = pointthree[0].y - pointthree[2].y;
        double angle3 = 180 / 3.1415 * Math.acos((ca[0] * cb[0] + ca[1] * cb[1])
                / (Math.sqrt(ca[0] * ca[0] + ca[1] * ca[1]) * Math.sqrt(cb[0] * cb[0] + cb[1] * cb[1])));
        int ccw3;
        if (ca[0] * cb[1] - ca[1] * cb[0] > 0) {
            ccw3 = 0;
        } else {
            ccw3 = 1;
        }
        if (Double.isNaN(angle1) || Double.isNaN(angle2) || Double.isNaN(angle3)) {
            return;
        }
        Point[] poly = new Point[4];
        if (angle3 > angle2 && angle3 > angle1) {
            if (ccw3 == 1) {
                poly[1] = pointthree[1];
                poly[3] = pointthree[0];
            } else {
                poly[1] = pointthree[0];
                poly[3] = pointthree[1];
            }
            poly[0] = pointthree[2];
            Point temp = new Point(pointthree[0].x + pointthree[1].x - pointthree[2].x,
                    pointthree[0].y + pointthree[1].y - pointthree[2].y);
            poly[2] = temp;
        } else if (angle2 > angle1 && angle2 > angle3) {
            if (ccw2 == 1) {
                poly[1] = pointthree[0];
                poly[3] = pointthree[2];
            } else {
                poly[1] = pointthree[2];
                poly[3] = pointthree[0];
            }
            poly[0] = pointthree[1];
            Point temp = new Point(pointthree[0].x + pointthree[2].x - pointthree[1].x,
                    pointthree[0].y + pointthree[2].y - pointthree[1].y);
            poly[2] = temp;
        } else if (angle1 > angle2 && angle1 > angle3) {
            if (ccw1 == 1) {
                poly[1] = pointthree[1];
                poly[3] = pointthree[2];
            } else {
                poly[1] = pointthree[2];
                poly[3] = pointthree[1];
            }
            poly[0] = pointthree[0];
            Point temp = new Point(pointthree[1].x + pointthree[2].x - pointthree[0].x,
                    pointthree[1].y + pointthree[2].y - pointthree[0].y);
            poly[2] = temp;
        }

        Point[] trans = new Point[4];

        int temp = 50;
        trans[0] = new Point(temp, temp);
        trans[1] = new Point(temp, 100 + temp);
        trans[2] = new Point(100 + temp, 100 + temp);
        trans[3] = new Point(100 + temp, temp);

        double maxAngle = Math.max(angle3, Math.max(angle1, angle2));
        // 二维码为直角，最大角过大或者过小都判断为不是二维码
        if (maxAngle < 75 || maxAngle > 115) {
            return;
        }
        Mat perspectiveMmat = Imgproc.getPerspectiveTransform(
                Converters.vector_Point_to_Mat(Arrays.asList(poly), CvType.CV_32F),
                Converters.vector_Point_to_Mat(Arrays.asList(trans), CvType.CV_32F)); // warp_mat
        Mat dst = new Mat();
        // 计算透视变换结果
        Imgproc.warpPerspective(src, dst, perspectiveMmat, src.size(), Imgproc.INTER_LINEAR);
        Rect roiArea = new Rect(0, 0, 200, 200);
        Mat dstRoi = new Mat(dst, roiArea);
        // 放大图片
        Imgproc.resize(dstRoi, dstRoi, new Size(2 * dstRoi.width(), 2 * dstRoi.height()));
        //
        Imgcodecs.imwrite(TEMP_PATH, dstRoi);
    }

    /**
     * 获取轮廓的中心坐标
     *
     * @param matOfPoint
     * @return
     */
    private static Point centerCal(MatOfPoint matOfPoint) {
        double centerx = 0, centery = 0;
        MatOfPoint2f mat2f = new MatOfPoint2f(matOfPoint.toArray());
        RotatedRect rect = Imgproc.minAreaRect(mat2f);
        Point[] vertices = new Point[4];
        rect.points(vertices);
        centerx = ((vertices[0].x + vertices[1].x) / 2 + (vertices[2].x + vertices[3].x) / 2) / 2;
        centery = ((vertices[0].y + vertices[1].y) / 2 + (vertices[2].y + vertices[3].y) / 2) / 2;
        Point point = new Point(centerx, centery);
        return point;
    }

    /**
     * 扫描整个目录中的文件
     */
    public static String decode(String resourceDirectory) {
        String result = null;

        File file = new File(resourceDirectory);
        File[] vouchers = file.listFiles();
        QRCodeDetector detector = new QRCodeDetector();
        if (vouchers == null) {
            System.out.println("目录扫描失败");
            return result;
        }
        for (File voucher : vouchers) {
            // 1. 第一次识别
            String qrCode = decodeQRcode(detector, voucher.getAbsolutePath());
            if (qrCode == null || "".equals(qrCode)) {
                /**
                 * 对图像进行处理，定位图像中的二维码，进行截取
                 */
                findQRcodeAndCut(voucher.getAbsolutePath());
                File file1 = new File(TEMP_PATH);
                if (file1.exists()) {
                    // 2. 第二次识别
                    qrCode = decodeQRcode(detector, TEMP_PATH);
                    if (qrCode == null || "".equals(qrCode)) {
                        /**
                         * 对图形进行数值优化
                         */
                        Mat mat = Imgcodecs.imread(TEMP_PATH, 1);
                        // 彩色图转灰度图
                        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
                        // 对图像进行平滑处理
                        Imgproc.blur(mat, mat, new Size(3, 3));
                        // 中值去噪
                        Imgproc.medianBlur(mat, mat, 5);
                        // 这里定义一个新的Mat对象，主要是为了保留原图，未下次处理做准备
                        Mat mat2 = new Mat();
                        // 根据 OTSU 算法进行二值化
                        Imgproc.threshold(mat, mat2, 0, 255, Imgproc.THRESH_OTSU);
                        // 生成二值化后的图像
                        Imgcodecs.imwrite(TEMP_PATH, mat2);
                        // 3. 第三次识别
                        qrCode = decodeQRcode(detector, TEMP_PATH);
                        if (qrCode == null || "".equals(qrCode)) {
                            /**
                             * 限制对比度的自适应直方图均衡化
                             */
                            CLAHE clahe = Imgproc.createCLAHE(2, new Size(8, 8));
                            clahe.apply(mat, mat);
                            Imgcodecs.imwrite(TEMP_PATH, mat);
                            // 4. 第四次识别
                            qrCode = decodeQRcode(detector, TEMP_PATH);
                            if (qrCode == null || "".equals(qrCode)) {
                                System.err.println(voucher.getName() + " recognition fail");
                            } else {
                                System.out.println(voucher.getName() + "---4---" + qrCode);
                                result = qrCode;
                            }
                        } else {
                            System.out.println(voucher.getName() + "---3---" + qrCode);
                            result = qrCode;
                        }
                    } else {
                        System.out.println(voucher.getName() + "---2---" + qrCode);
                        result = qrCode;
                    }
                } else {
                    System.out.println(voucher.getName() + " not find qrCode");
                }
            } else {
                System.out.println(voucher.getName() + "---1---" + qrCode);
                result = qrCode;
            }
            // 每次检查处理图片时是否有生成图片，若存在，则删除，避免干扰下一次图像识别结果
            File file2 = new File(TEMP_PATH);
            if (file2.exists()) {
                file2.delete();
            }
        }
        return result;
    }
}
