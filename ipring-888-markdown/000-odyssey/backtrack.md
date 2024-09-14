# 一 基本框架思维

回溯算法和我们常说的 DFS 算法非常类似，本质上就是一种暴力穷举算法。回溯算法和 DFS 算法的细微差别是：回溯算法是在遍历「树枝」，DFS 算法是在遍历「节点」

**解决一个回溯问题，实际上就是遍历一棵决策树的过程，树的每个叶子节点存放着一个合法答案。你把整棵树遍历一遍，把叶子节点上的答案都收集起来，就能得到所有的合法答案**。





站在回溯树的一个节点上，你只需要思考 3 个问题：

1、路径：也就是已经做出的选择。

2、选择列表：也就是你当前可以做的选择。

3、结束条件：也就是到达决策树底层，无法再做选择的条件。



```python
# 回溯算法框架
result = []
def backtrack(路径, 选择列表):
    if 满足结束条件:
        result.add(路径)
        return
    for 选择 in 选择列表:
        做选择
        backtrack(路径, 选择列表)
        撤销选择

# 其核心就是 for 循环里面的递归，在递归调用之前「做选择」，在递归调用之后「撤销选择」。
```



**我们不妨把这棵树称为回溯算法的「决策树」**。比如说你站在下图的红色节点上：

![img](https://labuladong.online/algo/images/backtracking/2.jpg)

你现在就在做决策，可以选择 1 那条树枝，也可以选择 3 那条树枝。为啥只能在 1 和 3 之中选择呢？因为 2 这个树枝在你身后，这个选择你之前做过了，而全排列是不允许重复使用数字的。

**`[2]` 就是「路径」，记录你已经做过的选择；`[1,3]` 就是「选择列表」，表示你当前可以做出的选择；「结束条件」就是遍历到树的底层叶子节点，这里也就是选择列表为空的时候**。

可以把「路径」和「选择列表」作为决策树上每个节点的属性，函数在树上游走要正确处理节点的属性，那么就要在这两个特殊时间点搞点动作

![img](https://labuladong.online/algo/images/backtracking/5.jpg)

```python
for 选择 in 选择列表:
    # 做选择
    将该选择从选择列表移除
    路径.add(选择)
    backtrack(路径, 选择列表)
    # 撤销选择
    路径.remove(选择)
    将该选择再加入选择列表
```



# 二 排列、组合、子集系列问题

记住如下组合/子集问题和排列问题的回溯树，就可以解决所有排列组合子集相关的问题：

![img](https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/1.jpeg)

![img](https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/2.jpeg)



* **子集问题**：集合中的元素不用考虑顺序，**通过保证元素之间的相对顺序不变来防止出现重复的子集**。如果想计算所有子集，那只要遍历这棵多叉树，把所有节点的值收集起来



## **2.1 元素无重不可复选**

### 2.1.1 子集

<img src="https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/5.jpeg" alt="img" style="zoom:50%;" />

**如果把根节点作为第 0 层，将每个节点和根节点之间树枝上的元素作为该节点的值，那么第 `n` 层的所有节点就是大小为 `n` 的所有子集**。

```java
// start 代表当前遍历开始的位置
// trace 为已走过的路径
void backtrack(LinkedList<Integer> trace, int[] nums, int start) {
    res.add(new ArrayList(trace)); // 这里记录所有走过的路径
    for (int i = start; i < nums.length; i++) {
        trace.addLast(nums[i]);
        backtrack(trace, nums, i + 1); // i 选过了，下一个元素不能回头且不重复选，所以给到 i + 1
        trace.removeLast();
    }
}
```

### 2.1.2  组合

**组合和子集是一样的：大小为 `k` 的组合就是大小为 `k` 的子集**。

```java
// 给你输入一个数组 nums = [1,2..,n] 和一个正整数 k，请你生成所有大小为 k 的子集。
void backtrack(LinkedList<Integer> trace, int n, int k, int start) {
    if (trace.size() == k) {  // 筛选大小为2的子集
        res.add(new ArrayList<>(trace));
    }
    for (int i = start; i <= n; i++) {
        trace.addLast(i);
        backtrack(trace, n, k, i + 1);
        trace.removeLast();
    }
}
```

### 2.1.3 排列

与子集/组合不同之处为当前选过的路径不能重复选即可，每一个路径都必须全部选上，不能剪枝

```java
 // 回溯算法核心函数
void backtrack(int[] nums) {
    // base case，到达叶子节点
    if (track.size() == nums.length) {
        // 收集叶子节点上的值
        res.add(new LinkedList(track));
        return;
    }

    // 回溯算法标准框架
    for (int i = 0; i < nums.length; i++) {
        // 已经存在 track 中的元素，不能重复选择
        if (used[i]) {
            continue;
        }
        // 做选择
        used[i] = true;
        track.addLast(nums[i]);
        // 进入下一层回溯树
        backtrack(nums);
        // 取消选择
        track.removeLast();
        used[i] = false;
    }
}
```



## **2.2 元素可重不可复选**（排序+剪枝逻辑）

### 2.2.1 子集[90. 子集 II](https://leetcode.cn/problems/subsets-ii/)

以 `nums = [1,2,2]` 为例，为了区别两个 `2` 是不同元素，后面我们写作 `nums = [1,2,2']`

按照之前的思路画出子集的树形结构，显然，两条值相同的相邻树枝会产生重复：

<img src="https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/8.jpeg" alt="img" style="zoom:50%;" />

```text
[ 
    [],
    [1],[2],[2'],
    [1,2],[1,2'],[2,2'],
    [1,2,2']
]
```

如果一个节点有多条值相同的树枝相邻，则只遍历第一条，剩下的都剪掉，不要去遍历

<img src="https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/9.jpeg" alt="img" style="zoom:50%;" />

**先进行排序，让相同的元素靠在一起，如果发现 `nums[i] == nums[i-1]`，则跳过**：

```java
Arrays.sort(nums); // 排序

void backtrack(LinkedList<Integer> trace, int[] nums, int start) {
    res.add(new ArrayList<>(trace));
    for (int i = start; i < nums.length; i++) {
        if (i > start && nums[i] == nums[i - 1]) {
            continue; // 剪枝逻辑，相邻枝叶元素重复
        }
        trace.addLast(nums[i]); 
        backtrack(trace, nums, i + 1);
        trace.removeLast();   
    }
}
```

### 2.2.2 组合 [40. 组合总和 II](https://leetcode.cn/problems/combination-sum-ii/)

给你输入 `candidates` 和一个目标和 `target`，从 `candidates` 中找出中所有和为 `target` 的组合。`candidates` 可能存在重复元素，且其中的每个数字最多只能使用一次。



说这是一个组合问题，其实换个问法就变成子集问题了：请你计算 `candidates` 中所有和为 `target` 的子集。

```java
class Solution {
    List<List<Integer>> res = new ArrayList<>();
    int sum = 0;
    public List<List<Integer>> combinationSum2(int[] candidates, int target) {
        LinkedList<Integer> trace = new LinkedList<>();    
        Arrays.sort(candidates);
        backtrack(candidates, target, trace, 0);
        return res;
    }

    void backtrack(int[] cds, int target, LinkedList<Integer> trace, int start) {
        // 组合问题无非是在子集问题上按要求筛符合条件的子集而已
        if (sum == target) {
            res.add(new ArrayList<>(trace));
            return;
        }
        if (sum > target) {
            return;
        }
        
        for (int i = start; i < cds.length; i++) {
            if (i > start && cds[i] == cds[i - 1]) continue; // 相同枝叶上的剪枝

            sum += cds[i];
            trace.add(cds[i]);
            backtrack(cds, target, trace, i + 1);
            trace.removeLast();
            sum -= cds[i];
        }
    }
}
```

### 2.2.3 排列 [47. 全排列 II](https://leetcode.cn/problems/permutations-ii/)

给你输入一个可包含重复数字的序列 `nums`，请你写一个算法，返回所有可能的全排列



```java
Arrays.sort(nums);
/* 排列问题回溯算法框架 */
void backtrack(int[] nums) {
    for (int i = 0; i < nums.length; i++) {
        // 剪枝逻辑
        if (used[i]) {
            continue;
        }
        // 剪枝逻辑，固定相同的元素在排列中的相对位置
        if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) {
            continue; // 如果前面的相邻相等元素没有用过，则跳过
            /*
            当出现重复元素时，比如输入 nums = [1,2,2',2'']，2' 只有在 2 已经被使用的情况下才会被选择，同理，2'' 只有在 2' 已经被使用的情况下才会被选择，这就保证了相同元素在排列中的相对位置保证固定。
            */
        }
        // 做选择
        used[i] = true;
        track.addLast(nums[i]);

        backtrack(nums);
        // 撤销选择
        track.removeLast();
        used[i] = false;
    }
}
```



## **2.3 元素无重可复选**

### 2.3.1 子集/组合[39. 组合总和](https://leetcode.cn/problems/combination-sum/)

想解决这种类型的问题，也得回到回溯树上，**我们不妨先思考思考，标准的子集/组合问题是如何保证不重复使用元素的**？

```java
// 无重组合的回溯算法框架
void backtrack(int[] nums, int start) {
    for (int i = start; i < nums.length; i++) {
        // ...
        // 递归遍历下一层回溯树，注意参数
        backtrack(nums, i + 1);
        // ...
    }
}
```

这个 `i` 从 `start` 开始，那么下一层回溯树就是从 `start + 1` 开始，从而保证 `nums[start]` 这个元素不会被重复使用：

那么反过来，如果我想让每个元素被重复使用，我只要把 `i + 1` 改成 `i` 即可：

```java
// 可重组合的回溯算法框架
void backtrack(int[] nums, int start) {
    for (int i = start; i < nums.length; i++) {
        // ...
        // 递归遍历下一层回溯树，注意参数
        backtrack(nums, i);
        // ...
    }
}
```

这相当于给之前的回溯树添加了一条树枝，在遍历这棵树的过程中，一个元素可以被无限次使用：

<img src="https://labuladong.online/algo/images/%E6%8E%92%E5%88%97%E7%BB%84%E5%90%88/10.jpeg" alt="img" style="zoom:50%;" />

当然，这样这棵回溯树会永远生长下去，所以我们的递归函数需要设置合适的 base case 以结束算法

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯的路径
    LinkedList<Integer> track = new LinkedList<>();
    // 记录 track 中的路径和
    int trackSum = 0;

    public List<List<Integer>> combinationSum(int[] candidates, int target) {
        if (candidates.length == 0) {
            return res;
        }
        backtrack(candidates, 0, target);
        return res;
    }

    // 回溯算法主函数
    void backtrack(int[] nums, int start, int target) {
        // base case，找到目标和，记录结果
        if (trackSum == target) {
            res.add(new LinkedList<>(track));
            return;
        }
        // base case，超过目标和，停止向下遍历
        if (trackSum > target) {
            return;
        }

        // 回溯算法标准框架
        for (int i = start; i < nums.length; i++) {
            // 选择 nums[i]
            trackSum += nums[i];
            track.add(nums[i]);
            // 递归遍历下一层回溯树
            // 同一元素可重复使用，注意参数
            backtrack(nums, i, target);
            // 撤销选择 nums[i]
            trackSum -= nums[i];
            track.removeLast();
        }
    }
}
```



### 2.3.2 排列

直接去掉used数组即可

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    LinkedList<Integer> track = new LinkedList<>();

    public List<List<Integer>> permuteRepeat(int[] nums) {
        backtrack(nums);
        return res;
    }

    // 回溯算法核心函数
    void backtrack(int[] nums) {
        // base case，到达叶子节点
        if (track.size() == nums.length) {
            // 收集叶子节点上的值
            res.add(new LinkedList(track));
            return;
        }

        // 回溯算法标准框架
        for (int i = 0; i < nums.length; i++) {
            // 做选择
            track.add(nums[i]);
            // 进入下一层回溯树
            backtrack(nums);
            // 取消选择
            track.removeLast();
        }
    }
}
```

