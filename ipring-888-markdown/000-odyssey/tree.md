[树 - 基础和Overview ](https://pdai.tech/md/algorithm/alg-basic-tree.html)

![img](https://pdai.tech/images/alg/alg-tree-0.png)

## 1 基础

- **层次**: 根节点为第一层，其余节点的层次等于其双亲节点的层次加1.
- **树的高度**: 也称为树的深度，树中节点的最大层次。

- **二叉树**: 最多有两棵子树的树被称为二叉树
- **斜树**: 所有节点都只有左子树的二叉树叫做左斜树，所有节点都只有右子树的二叉树叫做右斜树。(本质就是链表)
- **满二叉树**: 二叉树中所有非叶子结点的度都是2，且叶子结点都在同一层次上
- **完全二叉树**: 如果一个二叉树与满二叉树前m个节点的结构相同，这样的二叉树被称为完全二叉树





## 2 二叉搜索树 BST(Binary Search Tree)

**定义：**

- 若任意节点的左子树不空，则左子树上所有节点的值均小于它的根节点的值；
- 若任意节点的右子树不空，则右子树上所有节点的值均大于它的根节点的值；
- 任意节点的左、右子树也分别为二叉查找树；
- 没有键值相等的节点。

特性：

* **BST 的中序遍历结果是有序的** ：先遍历左节点再遍历右节点（升序）
* 二叉查找树相比于其他数据结构的优势在于查找、插入的时间复杂度较低为 O ( log ⁡ n ) 。二叉查找树是基础性数据结构，用于构建更为抽象的数据结构，如集合、多重集、关联数组等。



## 3 平衡二叉树 - AVL

平衡二叉树是一种特殊的二叉搜索树 (BST)，它保持树的高度尽可能低，从而使基本的操作（如搜索、插入和删除）的时间复杂度保持在 𝑂(log⁡𝑛)*O*(log*n*)。AVL 树是第一种自平衡二叉搜索树，由 Adelson-Velsky 和 Landis 在 1962 年发明，故名 AVL 树。

**定义**：

- 对于每个节点，左子树和右子树的高度差不超过1。
- 高度差又称为平衡因子（balance factor），其值只能是 -1, 0, 1。

> 平衡因子定义为节点的左子树高度减去右子树高度
>
> balance factor=height(left subtree)−height(right subtree)



为了保持 AVL 树的平衡，插入和删除操作后可能需要进行旋转调整树的结构。常见的旋转操作包括：

1. **单右旋 (Right Rotation)**:
2. **单左旋 (Left Rotation)**:
3. **左右双旋 (Left-Right Rotation)**:
4. **右左双旋 (Right-Left Rotation)**:





## 4 红黑树

红黑树也是一种自平衡的二叉查找树。

**定义**：

- 每个结点要么是红的要么是黑的。(红或黑)
- 根结点是黑的。  (根黑)
- 每个叶结点(叶结点即指树尾端NIL指针或NULL结点)都是黑的。 (叶黑)
- 如果一个结点是红的，那么它的两个儿子都是黑的。 (红子黑)
- 对于任意结点而言，其到叶结点树尾端NIL指针的每条路径都包含相同数目的黑结点。(路径下黑相同)

![img](https://pdai.tech/images/alg/alg-tree-14.png)

用法最广:

- Java ConcurrentHashMap & TreeMap
- C++ STL: map & set
- linux进程调度Completely Fair Scheduler,用红黑树管理进程控制块
- epoll在内核中的实现，用红黑树管理事件块
- nginx中，用红黑树管理timer等

## 5 哈弗曼树

## 6 B树

树(英语: B-tree)是一种自平衡的树，能够保持数据有序。这种数据结构能够让查找数据、顺序访问、插入数据及删除的动作，都在对数时间内完成。B树，概括来说是一种自平衡的m阶树，与自平衡二叉查找树不同，B树适用于读写相对大的数据块的存储系统，例如磁盘。

* 根结点至少有两个子女。

* 每个中间节点都包含k-1个元素和k个孩子，其中 m/2 <= k <= m

* 每一个叶子节点都包含k-1个元素，其中 m/2 <= k <= m

* 所有的叶子结点都位于同一层。

* 每个节点中的元素从小到大排列，节点当中k-1个元素正好是k个孩子包含的元素的值域分划。

B-Tree中的每个节点根据实际情况可以包含大量的关键字信息和分支，如下图所示为一个3阶的B-Tree:

<img src="https://pdai.tech/images/alg/alg-tree-15.png" style="transform: scale(1)," />



## 7 B+树





## 8 线段树

[树状数组(详细分析+应用)，看不懂打死我!-CSDN博客](https://blog.csdn.net/TheWayForDream/article/details/118436732)

[五分钟丝滑动画讲解 | 树状数组_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1ce411u7qP/?spm_id_from=333.337.search-card.all.click&vd_source=71d5857ff5a77dfd27c7ab5d01560a6c)