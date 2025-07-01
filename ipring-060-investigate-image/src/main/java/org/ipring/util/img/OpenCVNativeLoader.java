package org.ipring.util.img;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * @author liuguangjin
 * @date 2025/3/13
 */
@Configuration
@Slf4j
public class OpenCVNativeLoader {

    private static final String Win_NATIVE_LIB_DIR = "opencv/natives/";
    private static final String NATIVE_LIB_DIR = "opencv/natives/linux/";
    private static final String LIB_PATTERN = "classpath*:" + NATIVE_LIB_DIR + "*.so*";

    // 预定义的顺序列表
    private static final List<String> targetOrder = Arrays.asList(
            /*========== 核心模块 ==========*/
            "libopencv_core.so.4.5.3",
            "libopencv_flann.so.4.5.3",
            "libopencv_imgproc.so.4.5.3",
            "libopencv_ml.so.4.5.3",

            /*========== I/O 相关 ==========*/
            "libjpeg.so.62.1.0",
            "libwebp.so.4.0.2",
            "libopencv_imgcodecs.so.4.5.3",
            "libopencv_videoio.so.4.5.3",

            /*========== 通用功能模块 ==========*/
            "libopencv_features2d.so.4.5.3",
            "libopencv_highgui.so.4.5.3",
            "libopencv_objdetect.so.4.5.3",
            "libopencv_dnn.so.4.5.3",
            "libopencv_calib3d.so.4.5.3",
            "libopencv_video.so.4.5.3",
            "libopencv_photo.so.4.5.3",
            "libopencv_gapi.so.4.5.3",

            /*========== 扩展功能模块 ==========*/
            "libopencv_text.so.4.5.3",
            "libopencv_xfeatures2d.so.4.5.3",
            "libopencv_ximgproc.so.4.5.3",
            "libopencv_xobjdetect.so.4.5.3",
            "libopencv_xphoto.so.4.5.3",

            /*========== 图像处理增强模块 ==========*/
            "libopencv_freetype.so.4.5.3",
            "libopencv_img_hash.so.4.5.3",
            "libopencv_intensity_transform.so.4.5.3",
            "libopencv_phase_unwrapping.so.4.5.3",
            "libopencv_quality.so.4.5.3",
            "libopencv_reg.so.4.5.3",
            "libopencv_surface_matching.so.4.5.3",

            /*========== 视频与动态分析 ==========*/
            "libopencv_bgsegm.so.4.5.3",
            "libopencv_optflow.so.4.5.3",
            "libopencv_tracking.so.4.5.3",
            "libopencv_videostab.so.4.5.3",

            /*========== 3D 与标定模块 ==========*/
            "libopencv_ccalib.so.4.5.3",
            "libopencv_rgbd.so.4.5.3",
            "libopencv_stereo.so.4.5.3",
            "libopencv_structured_light.so.4.5.3",

            /*========== 深度学习与检测 ==========*/
            "libopencv_dnn_objdetect.so.4.5.3",
            "libopencv_dnn_superres.so.4.5.3",
            "libopencv_wechat_qrcode.so.4.5.3",

            /*========== 应用专用模块 ==========*/
            "libopencv_aruco.so.4.5.3",
            "libopencv_barcode.so.4.5.3",
            "libopencv_face.so.4.5.3",
            "libopencv_hfs.so.4.5.3",
            "libopencv_line_descriptor.so.4.5.3",
            "libopencv_mcc.so.4.5.3",
            "libopencv_rapid.so.4.5.3",
            "libopencv_saliency.so.4.5.3",
            "libopencv_shape.so.4.5.3",
            "libopencv_stitching.so.4.5.3",
            "libopencv_superres.so.4.5.3",

            /*========== 其他工具模块 ==========*/
            "libopencv_datasets.so.4.5.3",
            "libopencv_dpm.so.4.5.3",
            "libopencv_fuzzy.so.4.5.3",
            "libopencv_bioinspired.so.4.5.3",
            "libopencv_plot.so.4.5.3",
            "libopencv_alphamat.so.4.5.3",

            /*========== Java 绑定 ==========*/
            "libopencv_java453.so"
    );

    public static void configureNativeAccess(Path libPath) throws ReflectiveOperationException {
        String originalPath = System.getProperty("java.library.path");
        String newPath = originalPath + File.pathSeparator + libPath;
        System.setProperty("java.library.path", newPath);
        resetLibraryPathCache();

        log.info("Updated library path: {}", newPath);
    }

    private static void resetLibraryPathCache() throws ReflectiveOperationException {
        Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }

    private static void loadResources(List<Resource> resources) throws IOException {
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !filename.contains(".so")) continue;

            String[] split = filename.split("\\.so");
            String suffix = ".so";
            if (split.length > 1) {
                suffix = suffix + split[1];
            }

            // 创建临时文件（保留原始文件名）
            Path tempFile = Files.createTempFile(split[0], suffix);
            tempFile.toFile().deleteOnExit();


            // 复制资源内容到临时文件
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("load opencv lib：{}, fileName: {}", tempFile, filename);
            // 4. 加载库文件（注意顺序！先加载基础库如core、再加载其他）
            System.load(tempFile.toAbsolutePath().toString());
        }
    }

    // @PostConstruct
    public void loadNativeLibraries() throws Exception {
        Path tempDirPath = Paths.get(System.getProperty("java.io.tmpdir"));
        // 显式将临时文件路径加入JVM库搜索路径
        configureNativeAccess(tempDirPath);

        log.info("开始加载opencv依赖库");
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.contains("linux")) {
            // 1. 获取资源解析器
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            // 2. 匹配所有.so文件（包括带版本后缀的文件）
            Resource[] resources = resolver.getResources(LIB_PATTERN);
            // 提取每个资源的文件名
            Map<Resource, String> resourceToName = new HashMap<>();
            for (Resource res : resources) {
                String fileName = res.getFilename(); // 通过资源对象获取文件名
                resourceToName.put(res, fileName);
            }
            List<Resource> list = Arrays.asList(resources);
            list.sort((r1, r2) -> {
                // 比较文件名在顺序列表中的索引
                int index1 = targetOrder.indexOf(resourceToName.get(r1));
                int index2 = targetOrder.indexOf(resourceToName.get(r2));
                if (index1 == -1) index1 = Integer.MAX_VALUE; // 不在列表中的排在末尾
                if (index2 == -1) index2 = Integer.MAX_VALUE;
                return Integer.compare(index1, index2);
            });

            // 3. 遍历资源并提取到临时目录
            loadResources(list);
        } else if (osName.contains("win")) {
            try {
                // 1. 从资源目录读取DLL文件
                String libName = Core.NATIVE_LIBRARY_NAME + ".dll";
                InputStream inputStream = getClass().getClassLoader()
                        .getResourceAsStream(Win_NATIVE_LIB_DIR + libName);
                if (inputStream == null) {
                    throw new RuntimeException("opencv文件未找到");
                }

                // 创建临时文件（需保留.so扩展名）
                String[] split = libName.split("\\.");
                Path tempFile = Files.createTempFile(split[0], "." + split[1]);
                tempFile.toFile().deleteOnExit(); // JVM退出时删除

                // 复制资源内容到临时文件
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                inputStream.close();

                // 4. 加载临时DLL文件
                System.load(tempFile.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new RuntimeException("加载OpenCV DLL失败", e);
            }
        }
        log.info("opencv依赖库加载完毕");
    }
}