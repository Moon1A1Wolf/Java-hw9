import java.util.Random;

class SharedArray {
    private final int[] array;
    private boolean isFilled = false;

    public SharedArray(int size) {
        this.array = new int[size];
    }

    public synchronized void fillArray() {
        Random random = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt(100);
        }
        isFilled = true;
        notifyAll();
    }

    public synchronized int[] getArray() {
        while (!isFilled) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return array;
    }
}

class SumCalculator extends Thread {
    private final SharedArray sharedArray;
    private int sum;

    public SumCalculator(SharedArray sharedArray) {
        this.sharedArray = sharedArray;
    }

    @Override
    public void run() {
        int[] array = sharedArray.getArray();
        sum = 0;
        for (int num : array) {
            sum += num;
        }
    }

    public int getSum() {
        return sum;
    }
}

class AverageCalculator extends Thread {
    private final SharedArray sharedArray;
    private double average;

    public AverageCalculator(SharedArray sharedArray) {
        this.sharedArray = sharedArray;
    }

    @Override
    public void run() {
        int[] array = sharedArray.getArray();
        int sum = 0;
        for (int num : array) {
            sum += num;
        }
        average = (double) sum / array.length;
    }

    public double getAverage() {
        return average;
    }
}

public class Main {
    public static void main(String[] args) {
        int size = 15;
        SharedArray sharedArray = new SharedArray(size);

        Thread fillerThread = new Thread(sharedArray::fillArray);
        SumCalculator sumThread = new SumCalculator(sharedArray);
        AverageCalculator averageThread = new AverageCalculator(sharedArray);

        fillerThread.start();
        sumThread.start();
        averageThread.start();

        try {
            fillerThread.join();
            sumThread.join();
            averageThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int[] array = sharedArray.getArray();
        System.out.println("Generated array: ");
        for (int num : array) {
            System.out.print(num + " ");
        }
        System.out.println("\nSum: " + sumThread.getSum());
        System.out.println("Average: " + sumThread.getSum() / (double) size);
    }
}
