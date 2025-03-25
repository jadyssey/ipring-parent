package org.ipring.util.qr;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import static java.security.AccessController.doPrivileged;

@Configuration
@Slf4j
public class OpenCVNativeLoader {

    private static final String Win_NATIVE_LIB_DIR = "natives/";
    private static final String NATIVE_LIB_DIR = "natives/linux/";
    private static final String LIB_PATTERN = "classpath*:" + NATIVE_LIB_DIR + "*.so*";

    private static void loadResources(Resource[] resources) throws IOException {
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

    @PostConstruct
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

            // 3. 遍历资源并提取到临时目录
            loadResources(resources);
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