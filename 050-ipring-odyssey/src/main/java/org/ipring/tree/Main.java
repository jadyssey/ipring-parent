package org.ipring.tree;

import java.util.ArrayList;

/**
 * 树状数组
 */
class FenwickTree {
    private final ArrayList<Integer> tree;

    public FenwickTree(int size) {
        tree = new ArrayList<>(size + 1);
        for (int i = 0; i <= size; i++) {
            tree.add(0); // 初始化为0
        }
    }

    public ArrayList<Integer> getTree() {
        return tree;
    }

    // 单点更新操作：将索引为index的元素增加value
    public void update(int index, int value) {
        while (index < tree.size()) {
            tree.set(index, tree.get(index) + value);
            index += index & -index; // 更新后继节点
        }
    }

    // 查询前缀和：计算从1到index的元素和
    public int query(int index) {
        int sum = 0;
        while (index > 0) {
            sum += tree.get(index);
            index -= index & -index; // 跳转到前一个子树
        }
        return sum;
    }

    // 查询区间和：计算从start到end的元素和
    public int queryRange(int start, int end) {
        return query(end) - query(start - 1);
    }

    // 添加新元素
    public void addElement(int value) {
        int newSize = tree.size();
        tree.add(0); // 在末尾添加一个新的元素，初始值为0
        update(newSize, value); // 更新新添加的元素
    }
}

public class Main {
    public static void main(String[] args) {
        int[] arr = {3, 2, -1, 6, 5, 4, -3, 3, 7, 2, 3};
        FenwickTree fenwickTree = new FenwickTree(arr.length);

        // 初始化树状数组
        for (int i = 0; i < arr.length; i++) {
            fenwickTree.update(i + 1, arr[i]);
        }

        // todo 这里有问题
        fenwickTree.addElement(5); // 添加值为5的新元素
        fenwickTree.addElement(-2); // 添加值为-2的新元素

        // 查询前缀和和区间和
        System.out.println("Prefix Sum:");
        for (int i = 1; i < fenwickTree.getTree().size(); i++) {
            System.out.println("Sum from 1 to " + i + ": " + fenwickTree.query(i));
        }

        System.out.println("\nRange Sum:");
        System.out.println("Sum from 3 to 7: " + fenwickTree.queryRange(3, 7));
    }
}
