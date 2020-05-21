package search_engine.indexer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class ScanningFileVisitor implements FileVisitor<Path> {
    private IndexedDirectoriesMonitor monitor;
    private boolean notifyIndex = false;

    ScanningFileVisitor (IndexedDirectoriesMonitor monitor, boolean notifyIndex) {
        super();
        this.monitor = monitor;
        this.notifyIndex = notifyIndex;
    }

    @Override
    public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
        if (notifyIndex)
            monitor.notifyIndex(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        if (notifyIndex)
            System.err.printf("Error visiting file %s: %s%n", file.toAbsolutePath(), e.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        if (e != null)
            System.err.printf("Error registering directory %s: %s%n", dir.toAbsolutePath(), e.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        try {
            monitor.register(dir);
        } catch (IOException e) {
            System.err.printf("Unable to register %s: %s%n", dir.toAbsolutePath(), e.getMessage());
        }
        return FileVisitResult.CONTINUE;
    }
}
