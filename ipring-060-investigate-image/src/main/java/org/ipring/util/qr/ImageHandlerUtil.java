package org.ipring.util.qr;

import cn.hutool.core.collection.CollectionUtil;
import lombok.SneakyThrows;
import org.opencv.core.*;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ImageHandlerUtil {

    private static final int BIGGER_TIMES = 2;
    // 针对图像尺寸小于此值时进行放大（面积小于300*300）
    private static final int MIN_IMAGE_AREA = 90000;


    /**
     * 二维码识别与截取（优化后的实现）
     * 1. 对图像进行预处理（放大、灰度转换、平滑、边缘检测）。
     * 2. 使用轮廓检测并筛选出疑似二维码定位角的轮廓。
     * 3. 根据检测到的定位角数量调用不同的截取策略。
     *
     * @param src 输入图像
     * @return 截取后的二维码区域，若未检测到则返回null
     */
    public static Mat findQRCodeAndCut(Mat src) {
        List<Mat> captureResp = new ArrayList<>();
        Mat srcGray = new Mat();
        try {
            // 图片放大（如果面积较小）
            resizeIfNeeded(src);
            // 转换为灰度图并预处理
            preprocessGray(src, srcGray);
            // 进行边缘检测
            performCannyEdgeDetection(srcGray);

            // 查找轮廓
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            try {
                Imgproc.findContours(srcGray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
                // 筛选疑似二维码定位角的轮廓
                List<MatOfPoint> markContours = filterQRCodeContours(contours, hierarchy, srcGray);
                if (markContours.isEmpty()) {
                    return null;
                }

                // 根据定位角个数选择截取策略
                handleMarkContours(markContours, src, captureResp);
            } finally {
                hierarchy.release();
            }

            if (CollectionUtil.isEmpty(captureResp)) {
                return null;
            }
            // 默认返回第一个识别结果
            Mat result = captureResp.get(0);
            saveImage(result);
            return result;
        } finally {
            srcGray.release();
        }
    }

    private static void resizeIfNeeded(Mat src) {
        if (src.width() * src.height() < MIN_IMAGE_AREA) {
            Imgproc.resize(src, src, new Size(800, 600));
        }
    }

    private static void preprocessGray(Mat src, Mat dstGray) {
        Imgproc.cvtColor(src, dstGray, Imgproc.COLOR_RGB2GRAY);
        // 高斯平滑减少噪声
        Imgproc.GaussianBlur(dstGray, dstGray, new Size(3, 3), 0);
    }

    private static void performCannyEdgeDetection(Mat srcGray) {
        Imgproc.Canny(srcGray, srcGray, 112, 255);
    }

    private static List<MatOfPoint> filterQRCodeContours(List<MatOfPoint> contours, Mat hierarchy, Mat gray) {
        List<MatOfPoint> markContours = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            RotatedRect rotRect = Imgproc.minAreaRect(contour2f);
            double w = rotRect.size.width;
            double h = rotRect.size.height;
            double ratio = Math.max(w, h) / Math.min(w, h);
            // 条件：长短轴比小于1.3、尺寸限制、面积大于60
            if (ratio < 1.3 && w < gray.cols() / 4.0 && h < gray.rows() / 4.0
                    && Imgproc.contourArea(contours.get(i)) > 60) {
                double[] ds = hierarchy.get(0, i);
                if (ds != null && ds.length > 3) {
                    // 排除最外层轮廓
                    if (ds[3] == -1) {
                        continue;
                    }
                    int count = 0;
                    // 统计子轮廓数
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
        return markContours;
    }

    private static void handleMarkContours(List<MatOfPoint> markContours, Mat src, List<Mat> captureResp) {
        if (markContours.size() == 1) {
            captureResp.add(captureBySinglePoint(markContours.get(0), src));
        } else if (markContours.size() == 2) {
            captureResp.add(captureByTwoPoints(markContours.get(0), markContours.get(1), src));
        } else {
            // 多于两个定位角时，尝试所有三个点组合
            for (int i = 0; i < markContours.size() - 2; i++) {
                for (int j = i + 1; j < markContours.size() - 1; j++) {
                    for (int k = j + 1; k < markContours.size(); k++) {
                        List<MatOfPoint> threePointList = Arrays.asList(
                                markContours.get(i),
                                markContours.get(j),
                                markContours.get(k)
                        );
                        Mat capture = captureByThreePoints(threePointList, src);
                        if (Objects.nonNull(capture)) {
                            captureResp.add(capture);
                        }
                    }
                }
            }
        }
    }

    /**
     * 针对只检测到一个定位角时的截取策略
     *
     * @param contour 定位角轮廓
     * @param src     原图
     * @return 截取区域
     */
    private static Mat captureBySinglePoint(MatOfPoint contour, Mat src) {
        Point centerPoint = calcCenter(contour);
        int width = 200;
        Rect roi = createROI(centerPoint, width);
        Mat roiMat = new Mat(src, roi);
        Imgproc.resize(roiMat, roiMat, new Size(BIGGER_TIMES * width, BIGGER_TIMES * width));
        return roiMat;
    }

    /**
     * 针对检测到两个定位角时，根据两点中点进行截取
     *
     * @param contour1 定位角1
     * @param contour2 定位角2
     * @param src      原图
     * @return 截取区域
     */
    private static Mat captureByTwoPoints(MatOfPoint contour1, MatOfPoint contour2, Mat src) {
        Point p1 = calcCenter(contour1);
        Point p2 = calcCenter(contour2);
        Point centerPoint = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        double width = Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y) + 50;
        Rect roi = createROI(centerPoint, width);
        Mat roiMat = new Mat(src, roi);
        Imgproc.resize(roiMat, roiMat, new Size(BIGGER_TIMES * width, BIGGER_TIMES * width));
        return roiMat;
    }

    private static Rect createROI(Point centerPoint, double width) {
        return new Rect(
                Math.max((int) (centerPoint.x - width), 0),
                Math.max((int) (centerPoint.y - width), 0),
                (int) (2 * width), (int) (2 * width));
    }

    /**
     * 针对检测到三个定位角时的截取与透视矫正
     *
     * @param contours 三个定位角轮廓列表
     * @param src      原图
     * @return 截取区域，若不满足条件返回null
     */
    private static Mat captureByThreePoints(List<MatOfPoint> contours, Mat src) {
        // 计算三个点的中心坐标
        Point[] points = new Point[3];
        for (int i = 0; i < 3; i++) {
            points[i] = calcCenter(contours.get(i));
        }
        // 根据三个点构造候选多边形
        Point[] poly = getTransformedPolygon(points);
        if (poly == null) {
            return null;
        }
        // 定义目标四边形（这里固定输出200x200区域，可根据实际需求调整）
        Point[] targetPoints = new Point[]{
                new Point(50, 50),
                new Point(50, 150),
                new Point(150, 150),
                new Point(150, 50)
        };
        Mat perspectiveM = Imgproc.getPerspectiveTransform(
                Converters.vector_Point_to_Mat(Arrays.asList(poly), CvType.CV_32F),
                Converters.vector_Point_to_Mat(Arrays.asList(targetPoints), CvType.CV_32F)
        );
        Mat dst = new Mat();
        try {
            Imgproc.warpPerspective(src, dst, perspectiveM, src.size(), Imgproc.INTER_LINEAR);
            Rect roi = new Rect(0, 0, 200, 200);
            Mat roiMat = new Mat(dst, roi);
            Imgproc.resize(roiMat, roiMat, new Size(2 * roi.width, 2 * roi.height));
            return roiMat;
        } finally {
            dst.release();
        }
    }

    /**
     * 根据三个点计算构造目标四边形，用于透视变换
     * 若三个点角度不符合二维码特征则返回null
     *
     * @param points 三个定位角中心
     * @return 四边形的四个顶点数组
     */
    private static Point[] getTransformedPolygon(Point[] points) {
        double angle1 = calcAngle(points[1], points[0], points[2]);
        double angle2 = calcAngle(points[0], points[1], points[2]);
        double angle3 = calcAngle(points[0], points[2], points[1]);
        double maxAngle = Math.max(angle1, Math.max(angle2, angle3));
        if (maxAngle < 75 || maxAngle > 115) {
            return null;
        }
        // 根据最大角对应的位置确定四边形构造
        Point[] poly = new Point[4];
        if (maxAngle == angle3) {
            // 假设points[2]为直角顶点
            poly[0] = points[2];
            poly[1] = points[0];
            poly[3] = points[1];
        } else if (maxAngle == angle2) {
            poly[0] = points[1];
            poly[1] = points[0];
            poly[3] = points[2];
        } else {
            poly[0] = points[0];
            poly[1] = points[1];
            poly[3] = points[2];
        }
        // 根据两点推断第四点
        poly[2] = new Point(poly[1].x + poly[3].x - poly[0].x, poly[1].y + poly[3].y - poly[0].y);
        return poly;
    }

    /**
     * 计算三个点构成的夹角（以p2为顶点）
     *
     * @param p1 边点1
     * @param p2 顶点
     * @param p3 边点2
     * @return 角度值
     */
    private static double calcAngle(Point p1, Point p2, Point p3) {
        double[] vec1 = {p1.x - p2.x, p1.y - p2.y};
        double[] vec2 = {p3.x - p2.x, p3.y - p2.y};
        double dot = vec1[0] * vec2[0] + vec1[1] * vec2[1];
        double norm1 = Math.sqrt(vec1[0] * vec1[0] + vec1[1] * vec1[1]);
        double norm2 = Math.sqrt(vec2[0] * vec2[0] + vec2[1] * vec2[1]);
        return Math.acos(dot / (norm1 * norm2)) * 180 / Math.PI;
    }

    /**
     * 计算轮廓的中心点（基于最小外接矩形）
     *
     * @param contour 轮廓
     * @return 中心坐标
     */
    private static Point calcCenter(MatOfPoint contour) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        RotatedRect rect = Imgproc.minAreaRect(contour2f);
        Point[] vertices = new Point[4];
        rect.points(vertices);
        double centerX = (vertices[0].x + vertices[1].x + vertices[2].x + vertices[3].x) / 4.0;
        double centerY = (vertices[0].y + vertices[1].y + vertices[2].y + vertices[3].y) / 4.0;
        return new Point(centerX, centerY);
    }

    /**
     * 图像预处理与二值化函数（适用于光照不均场景）
     *
     * @param src 输入图像
     * @return 二值化后的图像
     */
    public static Mat processAndThresholdImage(Mat src) {
        Mat grayMat = new Mat();
        Mat blurredMat = new Mat();
        Mat binaryMat = new Mat();
        try {
            Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.medianBlur(grayMat, blurredMat, 3);
            Imgproc.GaussianBlur(blurredMat, blurredMat, new Size(3, 3), 0);
            Imgproc.adaptiveThreshold(
                    blurredMat, binaryMat, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 11, 2
            );
            saveImage(binaryMat);
            return binaryMat;
        } finally {
            grayMat.release();
            blurredMat.release();
        }
    }

    /**
     * 使用CLAHE进行图像对比度增强（针对低对比度图像）
     *
     * @param src 输入图像
     */
    @SneakyThrows
    public static void createCLAHE(Mat src) {
        if (src.empty()) {
            throw new IllegalArgumentException("输入图像不能为空");
        }
        Mat processed = new Mat();
        Mat gray = new Mat();
        Mat claheMat = new Mat();
        Mat sharpened = new Mat();
        try {
            if (src.channels() > 1) {
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                src.copyTo(gray);
            }
            Imgproc.medianBlur(gray, processed, 5);
            processed.convertTo(processed, CvType.CV_32F);
            Core.normalize(processed, processed, 0, 255, Core.NORM_MINMAX);
            processed.convertTo(processed, CvType.CV_8UC1);

            Scalar mean = Core.mean(processed);
            double autoClip = 2.5 + (mean.val[0] / 255.0) * 1.5;
            CLAHE clahe = Imgproc.createCLAHE(autoClip, new Size(8, 8));

            clahe.apply(processed, claheMat);

            Imgproc.GaussianBlur(claheMat, sharpened, new Size(0, 0), 2.0);
            Core.addWeighted(claheMat, 1.5, sharpened, -0.5, 0, sharpened);
            saveImage(sharpened);
        } finally {
            processed.release();
            gray.release();
            claheMat.release();
            sharpened.release();
        }
    }

    private static boolean saveImage(Mat mat) {
        return true;
//        return Imgcodecs.imwrite(IMAGE_PATH + UUIDUtil.generateTraceId() + ".jpg", mat);
    }
}