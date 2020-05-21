package search_engine.indexer;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;

import java.io.*;
import java.util.*;

class IndexedDirectoriesMonitor {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    private boolean notifyIndex = false;
    private IndexController indexController;

    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void registerAll(Path dir) throws IOException {
        Files.walkFileTree(dir, new ScanningFileVisitor(this, notifyIndex));
    }

    IndexedDirectoriesMonitor(File[] dirs) throws IOException {

        if (dirs.length == 0) {
            System.err.println("No indexed directories!");
            System.exit(1);
        }

        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();

        for (File dir: dirs) {
            System.out.printf("Scanning %s ...%n", dir);
            try {
                registerAll(dir.toPath());
            } catch (IOException e) {
                System.err.printf("Error: %s%n", e.getMessage());
            }
            System.out.println("Ready!");
        }
    }

    void notifyIndex(Path file) {
        if (notifyIndex)
            indexController.indexDocument(file);
    }

    void watch(IndexController indexController) {
        this.notifyIndex = true;
        this.indexController = indexController;

        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException e) {
                        System.out.printf("Error reading %s: %s%n", child.toAbsolutePath(), e.getMessage());
                    }
                } else if (kind == ENTRY_DELETE) {
                    indexController.deindex(child.toAbsolutePath().toFile());
                } else if (kind == ENTRY_MODIFY) {
                    if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                        indexController.indexDocument(child);
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
}
