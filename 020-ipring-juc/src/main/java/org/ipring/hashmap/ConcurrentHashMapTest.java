package org.ipring.hashmap;


import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lgj
 * @date 2024/4/16
 **/
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        // 创建一个 ConcurrentHashMap
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // 添加一些键值对
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);

        // 获取所有键的 Set 视图
        Set<String> keySet = map.keySet();

        // 打印所有键
        for (String key : keySet) {
            System.out.println("Key: " + key);
        }
    }
}
