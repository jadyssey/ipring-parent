package org.ipring;

/**
 * @Author lgj
 * @Date 2024/5/2
 */
public interface Question {
    // todo Redis的常用数据结构
    // todo Redis的常用数据结构的底层数据结构

    // todo Redis的大key问题，什么影响
    // todo Redis怎么实现分布式锁，怎么利用数据结构实现限流
    
    // todo GeoHash如何实现搜索附近商户？ redis好像有这个结构吧？

    /*
    Redis雪崩:
        使用随机的过期时间，避免大范围的key在同一时间过期，造成雪崩
    */

    /*
    Redis穿透:
        查询不到有效数据时，也可以将空数据缓存到redis中，防止无效数据一直被查询
     */

    /*
    Redis击穿:
        使用双检锁/分布式锁保证同一时间内只有一个线程去查询数据库并更新缓存，单一Key过期被大量流量击穿数据库
     */
}
