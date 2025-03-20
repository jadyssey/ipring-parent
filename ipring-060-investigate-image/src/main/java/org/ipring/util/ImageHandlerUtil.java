package org.ipring.util;

import cn.hutool.core.collection.CollectionUtil;
import lombok.SneakyThrows;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ImageHandlerUtil {
    private static final String IMAGE_PATH = "D:\\img\\";
    private static final int BIGGER_TIMES = 2;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // 打印OpenCV版本信息以确认加载成功
//        System.out.println("OpenCV version: " + Core.VERSION);
    }

    /**
     * 二维码识别与截取
     *
     * @param src
     */
    public static Mat findQRCodeAndCut(Mat src) {
        List<Mat> captureResp = new ArrayList<>();

        Mat src_gray = new Mat();
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
            if (rate < 1.3 && w < (double) src_gray.cols() / 4 && h < (double) src_gray.rows() / 4
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
            // 直接返回
            return null;
        } else if (markContours.size() == 1) {
            captureResp.add(capture(markContours.get(0), src));
        } else if (markContours.size() == 2) {
            List<MatOfPoint> threePointList = new ArrayList<>();
            threePointList.add(markContours.get(0));
            threePointList.add(markContours.get(1));
            captureResp.add(capture(threePointList, src));
        } else {
            for (int i = 0; i < markContours.size() - 2; i++) {
                List<MatOfPoint> threePointList = new ArrayList<>();
                for (int j = i + 1; j < markContours.size() - 1; j++) {
                    for (int k = j + 1; k < markContours.size(); k++) {
                        threePointList.add(markContours.get(i));
                        threePointList.add(markContours.get(j));
                        threePointList.add(markContours.get(k));
                        Mat capture = capture(threePointList, src, i + "-" + j + "-" + k);
                        if (Objects.nonNull(capture)) captureResp.add(capture);
                        threePointList.clear();
                    }
                }
            }
        }
        if (CollectionUtil.isEmpty(captureResp)) return null;

        Mat mat = captureResp.get(0);
        Imgcodecs.imwrite(IMAGE_PATH + UUIDUtil.generateTraceId() + ".jpg", mat);
        return mat;
    }


    /**
     * 图像预处理与二值化函数
     * 模糊核尺寸（建议奇数）
     * 中值滤波核尺寸（建议奇数）
     * 阈值方法（默认OTSU）
     *
     * @return 处理后的Mat对象
     */
    public static Mat processAndThresholdImage(Mat src) {
        // 读取图像并转为灰度图
        Mat grayMat = new Mat();
        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_RGB2GRAY);

        // 非破坏性处理链：中值滤波 -> 高斯模糊
        Mat blurredMat = new Mat();
        Imgproc.medianBlur(grayMat, blurredMat, 3); // 调整核大小为3x3
        Imgproc.GaussianBlur(blurredMat, blurredMat, new Size(3, 3), 0);

        // 自适应阈值（替代OTSU，适用于光照不均场景）
        Mat binaryMat = new Mat();
        Imgproc.adaptiveThreshold(
                blurredMat, binaryMat, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 11, 2
        );

        // 释放中间变量
        grayMat.release();
        blurredMat.release();
        Imgcodecs.imwrite(IMAGE_PATH + UUIDUtil.generateTraceId() + ".jpg", binaryMat);
        return binaryMat;
    }

    @SneakyThrows
    public static void createCLAHE(Mat src) {
        if (src.empty()) {
            throw new IllegalArgumentException("Input image cannot be empty");
        }

        Mat processed = new Mat();
        Mat gray = new Mat(); // 新增灰度转换中间Mat
        try {
            // 强制转换为灰度图像（关键修复）
            if (src.channels() > 1) {
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                src.copyTo(gray);
            }

            // 预处理优化
            Imgproc.medianBlur(gray, processed, 5);

            // 对比度拉伸修正：确保输出类型为CV_8UC1
            processed.convertTo(processed, CvType.CV_32F);
            Core.normalize(processed, processed, 0, 255, Core.NORM_MINMAX);
            processed.convertTo(processed, CvType.CV_8UC1); // 转换回8位整型

            // CLAHE参数动态配置
            Scalar mean = Core.mean(processed);
            double autoClip = 2.5 + (mean.val[0] / 255.0) * 1.5;
            CLAHE clahe = Imgproc.createCLAHE(autoClip, new Size(8, 8));

            Mat claheMat = new Mat();
            // 显式验证输入类型（调试用）
//            System.out.println("CLAHE input type: " + CvType.typeToString(processed.type()));
            clahe.apply(processed, claheMat);

            // 后处理优化
            Mat sharpened = new Mat();
            try {
                Imgproc.GaussianBlur(claheMat, sharpened, new Size(0, 0), 2.0);
                Core.addWeighted(claheMat, 1.5, sharpened, -0.5, 0, sharpened);

                // 输出类型验证
//                System.out.println("Output type: " + CvType.typeToString(sharpened.type()));
                String outputPath = IMAGE_PATH + UUIDUtil.generateTraceId() + ".jpg";
                if (!Imgcodecs.imwrite(outputPath, sharpened)) {
                    throw new IOException("Failed to write image to: " + outputPath);
                }
            } finally {
                sharpened.release();
            }
        } finally {
            processed.release();
            gray.release();
        }
    }


    /**
     * 针对对比度不高的图片，只能识别到一个角的，直接以该点为中心截取
     *
     * @param matOfPoint
     * @param src
     */
    private static Mat capture(MatOfPoint matOfPoint, Mat src) {
        Point centerPoint = centerCal(matOfPoint);
        int width = 200;
        Rect roiArea = new Rect(Math.max((int) (centerPoint.x - width), 0),
                Math.max((int) (centerPoint.y - width), 0), (2 * width), (2 * width));
        // 截取二维码
        Mat dstRoi = new Mat(src, roiArea);
        // 放大图片
        Imgproc.resize(dstRoi, dstRoi, new Size(BIGGER_TIMES * width, BIGGER_TIMES * width));
        // 保存到临时存放的文件中
        // Imgcodecs.imwrite(TEMP_PATH, dstRoi);
        return dstRoi;
    }

    /**
     * 当只识别到二维码的两个定位点时，根据两个点的中点进行定位
     *
     * @param threePointList
     * @param src
     */
    private static Mat capture(List<MatOfPoint> threePointList, Mat src) {
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
        // Imgcodecs.imwrite(TEMP_PATH, dstRoi);
        return dstRoi;
    }

    /**
     * 对图片进行矫正，裁剪
     *
     * @param contours
     * @param src
     * @param idx
     */
    private static Mat capture(List<MatOfPoint> contours, Mat src, String idx) {
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
            return null;
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
            return null;
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
        // 保存到本地
        // Imgcodecs.imwrite(TEMP_PATH, dstRoi);
        return dstRoi;
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
}
