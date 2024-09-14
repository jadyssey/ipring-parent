package org.ipring.creational.singleton;

/**
 * @Author lgj
 * @Date 2024/4/25
 */
public class DemoMultiThread2 {
    public static void main(String[] args) {
        Thread t1 = new Thread(new ThreadFirst());
        Thread t2 = new Thread(new ThreadTwo());
        t1.start();
        t2.start();
        /*
        ----- result ---
        first.value = first
        two.value = first
         */
    }

    static class ThreadFirst implements Runnable {
        @Override
        public void run() {
            SingletonDoubleCheck first = SingletonDoubleCheck.getInstance("first");
            System.out.println("first.value = " + first.value);
        }
    }

    static class ThreadTwo implements Runnable {
        @Override
        public void run() {
            SingletonDoubleCheck two = SingletonDoubleCheck.getInstance("two");
            System.out.println("two.value = " + two.value);
        }
    }
}
