package org.ipring.util;

import org.opencv.core.Core;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class OpenCVLoader {
    static {
        loadOpenCV();
    }

    public static void loadOpenCV() {
        try {
            // 1. 获取项目基目录（假设代码在模块根目录下的 src/main/java 中）
            String baseDir = System.getProperty("user.dir"); // 项目基目录，如 C:\aCodezt\dbu-mod-ai\dbu-mod-ai-provider
            String libDir = baseDir + File.separator + "ipring-060-investigate-image" + File.separator + "lib"; // lib 目录绝对路径

            // 2. 动态修改 java.library.path
            addLibraryPath(libDir);

            // 3. 根据操作系统选择库文件名
            String os = System.getProperty("os.name").toLowerCase();
            String libName = "opencv_java453"; // 基础库名（不带扩展名）
            if (os.contains("win")) {
                libName += ".dll";
            } else if (os.contains("linux") || os.contains("mac")) {
                libName = "lib" + libName + ".so";
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            // 4. 加载库
            System.loadLibrary(libName.split("\\.")[0]); // 去掉扩展名，如 opencv_java453
            System.out.println("成功加载 OpenCV 库: " + libName);
        } catch (Exception e) {
            throw new RuntimeException("加载 OpenCV 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过反射将路径添加到 java.library.path
     */
    private static void addLibraryPath(String path) throws Exception {
        String currentPath = System.getProperty("java.library.path");
        String newPath = currentPath + File.pathSeparator + path;
        System.setProperty("java.library.path", newPath);

        // 强制刷新 ClassLoader 的路径缓存
        Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
    }

    // 测试代码
    public static void main(String[] args) {
        // 调用 OpenCV 功能（示例）
        System.out.println("OpenCV 版本: " + Core.VERSION);
        System.out.println("Project Name: " + getProjectName());

    }

    public static String getProjectName() {
        try (InputStream input = OpenCVLoader.class.getClassLoader().getResourceAsStream("project-info.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("project.name");
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
}