package org.ipring.similarity;

import com.hankcs.hanlp.HanLP;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AddressCluster {

    // 地址预处理
    private static String preprocess(String address) {
        // 1. 去除特殊字符
        String cleaned = address.replaceAll("[!@#%^&*()]", "");
        
        // 2. 统一大小写
        cleaned = cleaned.toLowerCase();
        
        // 3. 替换别名
        Map<String, String> replacements = new HashMap<>();
        replacements.put("沪", "上海");
        replacements.put("京", "北京");
        replacements.put("no.", "号");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            cleaned = cleaned.replace(entry.getKey(), entry.getValue());
        }
        
        return cleaned;
    }

    // 生成TF-IDF向量
    private static Map<CharSequence, Integer> tfIdfVector(String text) {
        List<String> words = HanLP.segment(text).stream()
            .map(term -> term.word)
            .filter(word -> word.length() > 1) // 过滤单字
            .collect(Collectors.toList());

        // 计算TF
        Map<CharSequence, Integer> tf = new HashMap<>();
        for (String word : words) {
            tf.put(word, tf.getOrDefault(word, 0) + 1);
        }
        tf.replaceAll((k, v) -> v / words.size());

        return tf;
    }

    // 构建相似度矩阵
    private static double[][] buildSimilarityMatrix(List<String> addresses) {
        int size = addresses.size();
        double[][] matrix = new double[size][size];
        
        List<Map<CharSequence, Integer>> vectors = addresses.stream()
            .map(AddressCluster::tfIdfVector)
            .collect(Collectors.toList());

        CosineSimilarity cosine = new CosineSimilarity();
        
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                double sim = cosine.cosineSimilarity(
                    vectors.get(i), 
                    vectors.get(j)
                );
                matrix[i][j] = sim;
                matrix[j][i] = sim;
            }
        }
        return matrix;
    }

    // DBSCAN聚类
    private static List<List<String>> clusterAddresses(List<String> addresses, double eps, int minPts) {
        double[][] simMatrix = buildSimilarityMatrix(addresses);
        List<DoublePoint> points = new ArrayList<>();
        
        for (int i = 0; i < addresses.size(); i++) {
            double[] vec = simMatrix[i];
            points.add(new DoublePoint(vec));
        }

        DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(eps, minPts, new EuclideanDistance());
        List<org.apache.commons.math3.ml.clustering.Cluster<DoublePoint>> clusters = clusterer.cluster(points);

        List<List<String>> result = new ArrayList<>();
        for (org.apache.commons.math3.ml.clustering.Cluster<DoublePoint> c : clusters) {
            List<String> group = c.getPoints().stream()
                .map(p -> addresses.get(points.indexOf(p)))
                .collect(Collectors.toList());
            result.add(group);
        }
        return result;
    }

    public static void main(String[] args) {
        List<String> addresses = Arrays.asList(
            "上海市浦东新区张江路123号",
            "上海浦东新区张江路123号",
            "北京市朝阳区国贸大厦A座",
            "北京朝阳区国贸大厦A座"
        );

        // 预处理
        List<String> cleaned = addresses.stream()
            .map(AddressCluster::preprocess)
            .collect(Collectors.toList());

        // 聚类
        List<List<String>> groups = clusterAddresses(cleaned, 0.5, 1);

        // 输出结果
        for (int i = 0; i < groups.size(); i++) {
            System.out.println("Group " + (i+1) + ":");
            groups.get(i).forEach(System.out::println);
            System.out.println();
        }
    }
}