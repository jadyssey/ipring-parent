package org.ipring.backtrack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 回溯 全排列
 *
 * @Author lgj
 * @Date 2024/5/1
 */
public class Permutation {

    static final List<List<Integer>> res = new ArrayList<>();

    public static void main(String[] args) {
        String s = "abcbda";
        int[] nums = new int[]{1, 2, 3};

        List<List<Integer>> permute = permute(nums);
        System.out.println(permute);
    }

    /* 主函数，输入一组不重复的数字，返回它们的全排列 */
    static List<List<Integer>> permute(int[] nums) {
        LinkedList<Integer> trace = new LinkedList<>();
        boolean[] used = new boolean[nums.length];
        backtrack(used, trace, nums);
        return res;
    }

    static void backtrack(boolean[] used, LinkedList<Integer> trace, int[] nums) {
        if (trace.size() == nums.length) { // 满足结束条件
            res.add(new ArrayList<>(trace));
            return;
        }

        for (int i = 0; i < nums.length; i++) { // i 是选择，nums 是选择列表
            // 当前选择是否已做过，不可做重复选择
            if (used[i]) continue;

            used[i] = true; // 增加记录已做过的选择
            trace.addLast(i); // 做选择
            backtrack(used, trace, nums);
            trace.removeLast(); // 取消选择
            used[i] = false;  // 删除记录已做过的选择
        }
    }
}
