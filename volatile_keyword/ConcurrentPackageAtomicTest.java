package volatile_keyword;

import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentPackageAtomicTest {
    static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        WriteThread thread1 = new WriteThread();
        WriteThread thread2 = new WriteThread();
        WriteThread thread3 = new WriteThread();
        WriteThread thread4 = new WriteThread();
        WriteThread thread5 = new WriteThread();

        thread1.start(); thread2.start(); thread3.start(); thread4.start(); thread5.start();
        Thread.sleep(2000);
        System.out.println("total count: " + count);
    }

    static class WriteThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 10_000; i++) {
                ConcurrentPackageAtomicTest.count.getAndAdd(1);
            }
            System.out.println(this + "===> END");
        }
    }
}
