package happens_before_guarantee;

public class NoGuaranteeExample {
    private boolean flag = false;
    private int data = 0;

    public void writer() {
        data = 42;
        flag = true;
    }

    public void reader() {
        if (flag && data != 42) {
            System.out.println("Flag is true. Data: " + data);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10_000; i++) {
            NoGuaranteeExample example = new NoGuaranteeExample();

            Thread writerThread = new Thread(example::writer);
            Thread readerThread = new Thread(example::reader);

            writerThread.start();
            readerThread.start();

            writerThread.join();
            readerThread.join();

            Thread.sleep(100); // Small delay between iterations
        }
        System.out.println("--------------------");
    }
}
