# 1 MyBatis

## 1.1 SqlSession 

### 1.1.1 创建和使用

- **创建**：`SqlSession`通常由`SqlSessionFactory`创建。`SqlSessionFactory`是通过读取MyBatis配置文件并初始化相应的资源来生成的。`SqlSession`实例提供了执行映射语句、提交、回滚事务的方法。

  ```
  SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
  SqlSession sqlSession = sqlSessionFactory.openSession();
  ```

- **使用**：一旦创建了`SqlSession`，可以用它来执行SQL语句。通常会使用mapper接口或直接使用`sqlSession`对象的`select`, `insert`, `update`, `delete`方法。

  ```
  MyMapper mapper = sqlSession.getMapper(MyMapper.class);
  List<MyObject> list = mapper.selectAll();
  ```

### 1.1.2 SqlSession 的生命周期管理

`SqlSession`的生命周期通常由事务的生命周期管理。这意味着每个事务对应一个`SqlSession`实例。以下是生命周期管理的几个要点：

- **打开会话**：在每个数据库操作之前，创建一个新的`SqlSession`实例。
- **提交/回滚**：在事务结束时，提交或回滚事务。
- **关闭会话**：确保`SqlSession`在使用完后关闭，以释放数据库连接和其他资源。

```java
SqlSession sqlSession = sqlSessionFactory.openSession();
try {
    // 执行数据库操作
    MyMapper mapper = sqlSession.getMapper(MyMapper.class);
    mapper.insert(new MyObject());
    
    // 提交事务
    sqlSession.commit();
} catch (Exception e) {
    // 回滚事务
    sqlSession.rollback();
    throw e;
} finally {
    // 关闭会话
    sqlSession.close();
}

```

## 1.2 BaseExecutor

* 声明了两个缓存对象，并在此类中使用

```
protected PerpetualCache localCache; // 一级缓存
protected PerpetualCache localOutputParameterCache;  // 存储过程的缓存
```

* 在`query`方法执行的最后，会判断一级缓存级别是否是`STATEMENT`级别，如果是的话，就清空缓存，这也就是`STATEMENT`级别的一级缓存无法共享`localCache`的原因。
* 如果是`insert/delete/update`方法，缓存就会刷新

## 1.3 一级缓存

* [聊聊MyBatis缓存机制 - 美团技术团队 (meituan.com)](https://tech.meituan.com/2018/01/19/mybatis-cache.html)
* [【不懂就问】MyBatis的一级缓存竟然还会引来麻烦？ - 掘金 (juejin.cn)](https://juejin.cn/post/6844904201244377095)



* MyBatis 的一级缓存默认开启，属于 `SqlSession` 作用范围。在事务开启的期间，同样的数据库查询请求只会查询一次数据库，之后重复查询会从一级缓存中获取】
* MyBatis一级缓存内部设计简单，只是一个没有容量限定的HashMap，在缓存的功能性上有所欠缺。
* MyBatis的一级缓存最大范围是SqlSession内部，有多个SqlSession或者分布式的环境下，数据库写操作会引起脏数据，建议设定缓存级别为Statement。



### 1.2.2 CacheKey

CacheKey的构成：将`MappedStatement`的Id、SQL的offset、SQL的limit、SQL本身以及SQL中的参数传入了CacheKey这个类。

除去hashcode、checksum和count的比较外，只要updatelist中的元素一一对应相等，那么就可以认为是CacheKey相等。只要两条SQL的下列五个值相同，即可以认为是相同的SQL。

> Statement Id + Offset + Limmit + Sql + Params

## 1.4 二级缓存

在上文中提到的一级缓存中，其最大的共享范围就是一个SqlSession内部，如果多个SqlSession之间需要共享缓存，则需要使用到二级缓存。

开启二级缓存后，会使用CachingExecutor装饰Executor，进入一级缓存的查询流程前，先在CachingExecutor进行二级缓存的查询，具体的工作流程如下所示。

![img](https://awps-assets.meituan.net/mit-x/blog-images-bundle-2018a/28399eba.png)

1. MyBatis的二级缓存相对于一级缓存来说，实现了`SqlSession`之间缓存数据的共享，同时粒度更加的细，能够到`namespace`级别，通过Cache接口实现类不同的组合，对Cache的可控性也更强。
2. MyBatis在多表查询时，极大可能会出现脏数据，有设计上的缺陷，安全使用二级缓存的条件比较苛刻。
3. 在分布式环境下，由于默认的MyBatis Cache实现都是基于本地的，分布式环境下必然会出现读取到脏数据，需要使用集中式缓存将MyBatis的Cache接口实现，有一定的开发成本，直接使用Redis、Memcached等分布式缓存可能成本更低，安全性也更高。