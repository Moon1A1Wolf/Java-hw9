import java.io.*;
import java.util.*;
import java.math.BigInteger;

class FileHandler {
    private final String filePath;
    private boolean isFilled = false;

    public FileHandler(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void fillFile(int count) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            Random random = new Random();
            for (int i = 0; i < count; i++) {
                writer.println(random.nextInt(201) - 100);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isFilled = true;
        notifyAll();
    }

    public synchronized List<Integer> readFile() {
        while (!isFilled) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        List<Integer> numbers = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextInt()) {
                numbers.add(scanner.nextInt());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return numbers;
    }
}


class PrimeFinder extends Thread {
    private final FileHandler fileHandler;
    private final String outputFilePath;

    public PrimeFinder(FileHandler fileHandler, String outputFilePath) {
        this.fileHandler = fileHandler;
        this.outputFilePath = outputFilePath;
    }

    private List<Integer> sieveOfEratosthenes(int max) {
        boolean[] isPrime = new boolean[max + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;

        for (int i = 2; i * i <= max; i++) {
            if (isPrime[i]) {
                for (int j = i * i; j <= max; j += i) {
                    isPrime[j] = false;
                }
            }
        }

        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= max; i++) {
            if (isPrime[i]) {
                primes.add(i);
            }
        }
        return primes;
    }

    @Override
    public void run() {
        List<Integer> numbers = fileHandler.readFile();
        if (numbers.isEmpty()) return;

        int max = Collections.max(numbers);
        List<Integer> primes = sieveOfEratosthenes(max);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            for (int num : numbers) {
                if (primes.contains(num)) {
                    writer.println(num);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class FactorialCalculator extends Thread {
    private final FileHandler fileHandler;
    private final String outputFilePath;

    public FactorialCalculator(FileHandler fileHandler, String outputFilePath) {
        this.fileHandler = fileHandler;
        this.outputFilePath = outputFilePath;
    }

    private BigInteger factorial(int num) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= num; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    @Override
    public void run() {
        List<Integer> numbers = fileHandler.readFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            for (int num : numbers) {
                if (num >= 0) {
                writer.println(num + "! = " + factorial(num));
            } else {
                writer.println(num + "! = undefined (negative number)");
            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class App {
    public static void main(String[] args) {
        String filePath;
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the path to the file: ");
            filePath = scanner.nextLine();
        }

        FileHandler fileHandler = new FileHandler(filePath);
        String primeOutputFile = "primes.txt";
        String factorialOutputFile = "factorials.txt";

        Thread fillerThread = new Thread(() -> fileHandler.fillFile(10));
        PrimeFinder primeThread = new PrimeFinder(fileHandler, primeOutputFile);
        FactorialCalculator factorialThread = new FactorialCalculator(fileHandler, factorialOutputFile);

        fillerThread.start();
        primeThread.start();
        factorialThread.start();

        try {
            fillerThread.join();
            primeThread.join();
            factorialThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Processing completed. Results written to files: " + primeOutputFile + " and " + factorialOutputFile);
    }
}
