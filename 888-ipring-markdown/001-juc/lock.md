## 1 ReentrantLock

![img](https://oss.javaguide.cn/github/javaguide/java/concurrent/reentrantlock-class-diagram.png)

`ReentrantLock` 里面有一个内部类 `Sync`，`Sync` 继承 AQS（`AbstractQueuedSynchronizer`），添加锁和释放锁的大部分操作实际上都是在 `Sync` 中实现的。`Sync` 有公平锁 `FairSync` 和非公平锁 `NonfairSync` 两个子类。

### [公平锁和非公平锁有什么区别？](#公平锁和非公平锁有什么区别)

- **公平锁** : 锁被释放之后，先申请的线程先得到锁。性能较差一些，因为公平锁为了保证时间上的绝对顺序，上下文切换更频繁。
- **非公平锁**：锁被释放之后，后申请的线程可能会先获取到锁，是随机或者按照其他优先级排序的。性能更好，但可能会导致某些线程永远无法获取到锁。





## 2 Future的作用

`Future` 接口主要用于表示一个异步计算的结果。它提供了一种方式，可以在任务执行完成后获得其结果或者状态。`Future` 通常与 `ExecutorService` 一起使用，`ExecutorService` 可以异步地执行任务，并返回一个 `Future` 对象，用于获取任务的结果。

在 Java 中，`Future` 接口主要用于表示一个异步计算的结果。它提供了一种方式，可以在任务执行完成后获得其结果或者状态。`Future` 通常与 `ExecutorService` 一起使用，`ExecutorService` 可以异步地执行任务，并返回一个 `Future` 对象，用于获取任务的结果。

`Future` 接口提供了几种关键方法：

1. **`get()`**: 用于获取计算结果。如果计算尚未完成，此方法会阻塞直到任务完成并返回结果。
2. **`get(long timeout, TimeUnit unit)`**: 与 `get()` 类似，但如果等待时间超过指定的 `timeout`，则会抛出 `TimeoutException`。
3. **`isDone()`**: 返回 `true` 如果任务已完成（无论是正常完成还是异常完成）。
4. **`isCancelled()`**: 返回 `true` 如果任务已被取消。
5. **`cancel(boolean mayInterruptIfRunning)`**: 尝试取消任务。