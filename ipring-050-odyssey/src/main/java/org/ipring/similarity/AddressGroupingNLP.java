package org.ipring.similarity;

import com.github.houbb.nlp.keyword.similarity.util.SimilarityHelper;
import com.hankcs.hanlp.HanLP;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AddressGroupingNLP {

    // 中文分词与停用词过滤（可扩展）
    private static String preprocess(String address) {
        List<String> terms = HanLP.segment(address).stream()
                .map(term -> term.word)
                .collect(Collectors.toList());
        return String.join(" ", terms);
    }

    // 分组主逻辑
    public static List<Set<String>> groupAddresses(List<String> addresses, double threshold) {
        List<String> processedList = addresses.stream()
                .map(AddressGroupingNLP::preprocess)
                .collect(Collectors.toList());

        UnionFind uf = new UnionFind(addresses.size());
        for (int i = 0; i < processedList.size(); i++) {
            for (int j = i + 1; j < processedList.size(); j++) {
                double sim = SimilarityHelper.similarity(
                        processedList.get(i), 
                        processedList.get(j)
                );
                if (sim >= threshold) {
                    uf.union(i, j);
                }
            }
        }
        return uf.getGroups(addresses);
    }

    // 分组主逻辑（多线程优化版）
    public static List<Set<String>> parallelGroupAddresses(List<String> addresses, double threshold) {
        // 并行预处理地址
        List<String> processedList = addresses.parallelStream()
                .map(AddressGroupingNLP::preprocess)
                .collect(Collectors.toList());

        UnionFind uf = new UnionFind(addresses.size());
        Set<Pair<Integer, Integer>> unionPairs = ConcurrentHashMap.newKeySet(); // 线程安全集合

        // 并行计算相似度
        IntStream.range(0, processedList.size()).parallel().forEach(i -> {
            for (int j = i + 1; j < processedList.size(); j++) {
                double sim = SimilarityHelper.similarity(processedList.get(i), processedList.get(j));
                if (sim >= threshold) {
                    unionPairs.add(Pair.of(i, j));
                }
            }
            System.out.println("正在匹配计算中：i = " + i);
        });

        // 合并操作（单线程保证顺序）
        unionPairs.forEach(pair -> uf.union(pair.getLeft(), pair.getRight()));
        return uf.getGroups(addresses);
    }

    // 并查集辅助类
    static class UnionFind {
        int[] parent;
        public UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }
        public int find(int x) {
            return parent[x] == x ? x : (parent[x] = find(parent[x]));
        }
        public void union(int x, int y) {
            parent[find(x)] = find(y);
        }
        public List<Set<String>> getGroups(List<String> addresses) {
            Map<Integer, Set<String>> groups = new HashMap<>();
            for (int i = 0; i < addresses.size(); i++) {
                int root = find(i);
                groups.computeIfAbsent(root, k -> new HashSet<>()).add(addresses.get(i));
            }
            return new ArrayList<>(groups.values());
        }
    }


    // 测试示例
    public static void main(String[] args) {
        List<String> addresses = Arrays.asList(
                "北京市海淀区中关村大街1号",
                "北京海淀中关村大街一号",
                "上海市浦东新区陆家嘴环路100号",
                "上海浦东陆家嘴环路100号"
        );

        List<Set<String>> groups = groupAddresses(addresses, 0.75);
        System.out.println("分组结果：");
        groups.forEach(System.out::println); 
        // 输出：
        // [北京市海淀区中关村大街1号, 北京海淀中关村大街一号]
        // [上海市浦东新区陆家嘴环路100号, 上海浦东陆家嘴环路100号]
    }
}