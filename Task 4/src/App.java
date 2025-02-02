import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

class FileSearch implements Runnable {
    private final Path directory;
    private final String searchWord;
    private final Path mergedFile;

    public FileSearch(Path directory, String searchWord, Path mergedFile) {
        this.directory = directory;
        this.searchWord = searchWord;
        this.mergedFile = mergedFile;
    }

    @Override
    public void run() {
        try {
            if (!Files.exists(mergedFile)) {
                Files.createFile(mergedFile);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(mergedFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                Files.walk(directory)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                List<String> lines = Files.readAllLines(file);
                                for (String line : lines) {
                                    if (line.contains(searchWord)) {
                                        writer.write(line);
                                        writer.newLine();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
            System.out.println("File search and merge complete. Output saved to " + mergedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class WordFilter implements Runnable {
    private final Path inputFile;
    private final Path outputFile;
    private final Set<String> forbiddenWords;
    private final CountDownLatch latch;

    public WordFilter(Path inputFile, Path outputFile, Set<String> forbiddenWords, CountDownLatch latch) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.forbiddenWords = forbiddenWords;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            if (!Files.exists(outputFile)) {
                Files.createFile(outputFile);
            }

            try (BufferedReader reader = Files.newBufferedReader(inputFile);
                 BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String filteredLine = filterWords(line);
                    writer.write(filteredLine);
                    writer.newLine();
                }
            }
            System.out.println("Word filtering complete. Output saved to " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    private String filterWords(String line) {
        for (String word : forbiddenWords) {
            line = line.replaceAll("\\b" + word + "\\b", "");
        }
        return line;
    }
}

public class App {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the path to the directory: ");
            String directoryPath = scanner.nextLine();
            System.out.print("Enter the word to search: ");
            String searchWord = scanner.nextLine();
            System.out.print("Enter the path to the forbidden words file: ");
            String forbiddenWordsFilePath = scanner.nextLine();

            Path directory = Paths.get(directoryPath);
            Path mergedFile = Paths.get("merged.txt");
            Path filteredFile = Paths.get("filtered_output.txt");

            Set<String> forbiddenWords = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(forbiddenWordsFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    forbiddenWords.add(line.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            CountDownLatch latch = new CountDownLatch(1);

            Thread searchThread = new Thread(new FileSearch(directory, searchWord, mergedFile));
            Thread filterThread = new Thread(new WordFilter(mergedFile, filteredFile, forbiddenWords, latch));

            searchThread.start();
            searchThread.join();
            filterThread.start();

            latch.await();
            System.out.println("Processing completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
