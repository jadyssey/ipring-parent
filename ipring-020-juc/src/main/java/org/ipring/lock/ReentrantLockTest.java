package org.ipring.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lgj
 * @date 2024/5/22
 **/
public class ReentrantLockTest {

    static int num = 0;
    static ReentrantLock reentrantLock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(ReentrantLockTest::inc);
        Thread thread2 = new Thread(ReentrantLockTest::inc);
        thread1.start();
        thread2.start();

        // main等待两个线程执行完
        thread1.join();
        thread2.join();
        System.out.println("num = " + num);
    }

    static void inc() {
        try {
            reentrantLock.lock();
            for (int i = 0; i < 10000; i++) {
                num++;
            }
        } finally {
            reentrantLock.unlock();
        }
    }
}
