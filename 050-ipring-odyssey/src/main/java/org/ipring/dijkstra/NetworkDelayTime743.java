package org.ipring.dijkstra;

import java.util.Arrays;

class NetworkDelayTime743 {
    public int networkDelayTime(int[][] times, int n, int k) {
        /*
        邻接矩阵表示图
         */
        int INF = Integer.MAX_VALUE / 2; // 除二防溢出
        int[][] g = new int[n][n];
        for (int[] temp : g) {
            Arrays.fill(temp, INF);
        }

        for (int[] temp : times) {
            int x = temp[0] - 1;
            int y = temp[1] - 1;
            g[x][y] = temp[2]; // 邻接矩阵赋值
        }

        /*
        增加辅助列表记录
        */
        boolean[] done = new boolean[n]; // 确定节点列表
        int[] dist = new int[n]; // 距离列表
        Arrays.fill(dist, INF);
        dist[k - 1] = 0; // 起点距离起点初始化为0
        int maxDist = 0;

        /*
        dijkstra
        */
        for (int i = 0; i < n; i++) { // 一次更新一个节点到路径中，总共需要更新所有节点
            int cur = -1;
            // 1. 从未确定的节点中选取dist值最小的，收录进来
            for (int j = 0; j < n; j++) {
                if (!done[j] && (cur == -1 || dist[cur] > dist[j])) {
                    cur = j;
                }
            }
            if (dist[cur] == INF) return -1; // 有节点无法到达

            done[cur] = true; // 确定当前点
            maxDist = dist[cur]; // 求出的最短路会越来越大

            // 2. 以当前点为起点，更新它能触及的未确定的所有节点
            for (int t = 0; t < n; t++) {
                // 此处由于不知道 cur 能触及哪些节点，所以需要从头到尾更新一遍
                // 不能触及的点自然是最大值，不会更新
                dist[t] = Math.min(dist[t], dist[cur] + g[cur][t]);
            }
        }
        return maxDist;
    }
}