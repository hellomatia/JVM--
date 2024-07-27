package volatile_keyword;

public class VolatileFiled extends Thread {
    public volatile boolean flag = true;

    @Override
    public void run() {
        long count = 0;
        while (flag) {
            count++;
        }
        System.out.println("Tread terminated. " + count);
    }

    public static void main(String[] args) throws InterruptedException {
        VolatileFiled volatileFiled = new VolatileFiled();
        volatileFiled.start();
        Thread.sleep(1000);
        System.out.println("after sleep in main");

        volatileFiled.flag = false;
        volatileFiled.join();
        System.out.println("main Thread End... ");
    }
}
