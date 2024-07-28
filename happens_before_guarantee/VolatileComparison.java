package happens_before_guarantee;

public class VolatileComparison {
    private boolean ready;
    private volatile boolean volatileReady;
    private int[] data = new int[10];

    public void writer() {
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }
        ready = true;
        volatileReady = true;
    }

    public void nonVolatileReader() {
        while (!ready) {
            Thread.yield(); // Give other threads a chance to run
        }
        System.out.println("Non-volatile read: " + java.util.Arrays.toString(data));
    }

    public void volatileReader() {
        while (!volatileReady) {
            Thread.yield(); // Give other threads a chance to run
        }
        System.out.println("Volatile read: " + java.util.Arrays.toString(data));
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            VolatileComparison example = new VolatileComparison();

            Thread writerThread = new Thread(example::writer);
            Thread nonVolatileReaderThread = new Thread(example::nonVolatileReader);
            Thread volatileReaderThread = new Thread(example::volatileReader);

            nonVolatileReaderThread.start();
            volatileReaderThread.start();

            // Give readers a chance to start waiting
            Thread.sleep(10);

            writerThread.start();

            writerThread.join();
            nonVolatileReaderThread.join();
            volatileReaderThread.join();

            System.out.println("--------------------");
        }
    }
}
