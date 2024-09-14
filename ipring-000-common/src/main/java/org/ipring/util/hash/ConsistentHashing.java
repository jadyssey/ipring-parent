package org.ipring.util.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing {
    private final int numberOfReplicas;
    private final SortedMap<Integer, String> circle = new TreeMap<>();

    public ConsistentHashing(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public void add(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node + i), node);
        }
    }

    public void remove(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hash(node + i));
        }
    }

    public String get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            return ((digest[3] & 0xFF) << 24) | ((digest[2] & 0xFF) << 16)
                | ((digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static void main(String[] args) {
        ConsistentHashing hash = new ConsistentHashing(3);

        hash.add("Node1");
        hash.add("Node2");
        hash.add("Node3");

        System.out.println("Node for key 'A': " + hash.get("A"));
        System.out.println("Node for key 'B': " + hash.get("B"));
        System.out.println("Node for key 'C': " + hash.get("C"));

        hash.remove("Node2");

        System.out.println("Node for key 'A' after removing Node2: " + hash.get("A"));
        System.out.println("Node for key 'B' after removing Node2: " + hash.get("B"));
        System.out.println("Node for key 'C' after removing Node2: " + hash.get("C"));
    }
}
