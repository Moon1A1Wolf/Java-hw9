import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class DirectoryCopyTask implements Runnable {
    private final Path source;
    private final Path target;
    private static final AtomicInteger fileCount = new AtomicInteger(0);

    public DirectoryCopyTask(Path source, Path target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = target.resolve(source.relativize(dir));
                    Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = target.resolve(source.relativize(file));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    fileCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to copy " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getFileCount() {
        return fileCount.get();
    }
}

public class DirectoryCopyApp {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the path to the source directory: ");
            String sourceDirPath = scanner.nextLine();
            System.out.print("Enter the path to the target directory: ");
            String targetDirPath = scanner.nextLine();

            Path sourceDir = Paths.get(sourceDirPath);
            Path targetDir = Paths.get(targetDirPath);

            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                System.out.println("Source directory does not exist or is not a directory.");
                return;
            }

            if (Files.exists(targetDir)) {
                System.out.println("Target directory already exists. Proceeding with copy...");
            } else {
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    System.err.println("Failed to create target directory.");
                    e.printStackTrace();
                    return;
                }
            }

            DirectoryCopyTask task = new DirectoryCopyTask(sourceDir, targetDir);
            Thread copyThread = new Thread(task);
            copyThread.start();

            try {
                copyThread.join();
                System.out.println("Directory copy completed.");
                System.out.println("Total files copied: " + DirectoryCopyTask.getFileCount());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
