package org.ipring;

import java.util.concurrent.TimeUnit;

/**
 * @Author lgj
 * @Date 2024/5/2
 */
public interface Question {
    /**
     * 1 多线程中暂停线程的方式有哪些？ wait() join() interrupt(),
     */
    static void main(String[] args) throws InterruptedException {
        new Object().wait(); // 1 释放锁
        Thread.currentThread().interrupt(); // 2 中断
        TimeUnit.SECONDS.sleep(5); // 3 没有释放锁
        Thread.yield();// 4 让出CPU时间片 没有释放锁
        /*
        5 join()
        join()的作用是：“等待该线程终止” 这里需要理解的就是该线程是指的主线程等待子线程的终止。
        也就是在子线程调用了join()方法后面的代码，只有等到子线程结束了才能执行。
         */
        Thread subThread = new Thread(() -> {
            System.out.println("Thread start = " + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread end = " + Thread.currentThread().getName());
        });
        subThread.start();
        subThread.join(); // 等待“subThread”线程终止
        System.out.println("Last println = " + Thread.currentThread().getName());
    }

    // todo AQS

    // todo ConcurrentHashMap

    // todo synchronized的await notify、Lock的Condition.await signal以及LockSupport的park unpark 三种方式的区别？
    // todo 多个线程交替打印 1~1000
}
