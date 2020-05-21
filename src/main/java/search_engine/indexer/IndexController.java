package search_engine.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndexController {

    private Directory indexDir;
    private IndexWriter writerForNotifications = null;

    public IndexController (Path indexDirPath) {
        try {
            this.indexDir = FSDirectory.open(indexDirPath);

        } catch (IOException e) {
            System.err.printf("Error opening the index directory: %s%n", e.getMessage());
            System.exit(1);
        }
    }

    private boolean writerForNotificationsAvailable () {
        return writerForNotifications != null && writerForNotifications.isOpen();
    }

    private IndexWriter prepareWriter() {
        return prepareWriter(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    private IndexWriter prepareWriter(IndexWriterConfig.OpenMode mode) {

        Analyzer english = new EnglishAnalyzer(), polish = new PolishAnalyzer();
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("contentEnglish", english);
        analyzerMap.put("contentPolish", polish);
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(
                new KeywordAnalyzer(), analyzerMap);

        IndexWriterConfig iwc = new IndexWriterConfig(wrapper);
        iwc.setOpenMode(mode);

        try {
            return new IndexWriter(indexDir, iwc);

        } catch (IOException e) {
            System.err.printf("Error preparing the index writer: %s%n", e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public void index (File dir) {
            index(new File[] {dir}, false);
    }

    private void index (File[] dirs, boolean anew) {
        try (IndexWriter writer = prepareWriter(anew
                ?IndexWriterConfig.OpenMode.CREATE
                :IndexWriterConfig.OpenMode.CREATE_OR_APPEND)) {

            for (File dir: dirs)
                try {
                    indexDirectory(dir, writer);
                } catch (IOException e) {
                    System.err.printf("Error indexing %s: %s%n", dir.getAbsolutePath(), e.getMessage());
                }

        } catch (IOException e) {
            System.err.printf("Error closing index: %s%n", e.getMessage());
        }
    }

    private void indexDocument (File file, IndexWriter writer) {
        try {
            if (file.isFile()) {

                try {
                    String canonical = file.getCanonicalPath();
                    System.out.printf("Indexing %s... ", canonical);

                    Tika tika = new Tika();
                    Document doc = new Document();
                    doc.add(new StringField("path", canonical, Field.Store.YES));

                    String body;
                    if (file.length() > 0)
                        body = tika.parseToString(file);
                    else
                        body = "";

                    String content = file.getName().concat("\n").concat(body);

                    LanguageDetector detector = new OptimaizeLangDetector().loadModels();
                    LanguageResult result = detector.detect(content);

                    if (!result.isUnknown() && result.isLanguage("pl")) {
                        doc.add(new TextField("contentPolish", content, Field.Store.YES));
                    } else {
                        doc.add(new TextField("contentEnglish", content, Field.Store.YES));
                    }
                    writer.updateDocument(new Term("path", canonical), doc);
                    writer.commit();

                    System.out.println("Finished!");

                } catch (Exception e) {
                    System.err.printf("Error reading from file %s: %s%n", file.getAbsolutePath(), e.getMessage());
                }
            }
        } catch (SecurityException e) {
            System.err.printf("Unable to access path %s: %s%n", file.getAbsolutePath(), e.getMessage());
        }
    }

    void indexDocument (Path file) {
        if (writerForNotificationsAvailable())
            indexDocument(file.toFile(), writerForNotifications);
        else
            System.err.println("No writer available...");
    }

    public void clearIndex () {
        index(new File[] {}, false);
    }

    private void indexDirectory(File dir, IndexWriter writer) throws IOException {

        if (dir.isDirectory()) {
            String canonical = dir.getCanonicalPath();
            System.out.printf("Indexing %s... ", canonical);

            writerForNotifications = writer;
            Files.walkFileTree(dir.toPath(), new IndexingFileVisitor(this));

            Document indexedDir = new Document();
            indexedDir.add(new StringField("dir", canonical, Store.YES));
            indexedDir.add(new SortedDocValuesField("dir", new BytesRef(canonical)));
            writer.updateDocument(new Term("dir", new BytesRef(canonical)), indexedDir);

            System.out.println("Finished!");

        } else
            System.err.printf("Error: %s is not a directory", dir.getAbsolutePath());
    }

    private void deindex (File dir, IndexWriter writer) {

        String canonical;
        try {
            canonical = dir.getCanonicalPath();
        } catch (IOException e) {
            canonical = dir.getAbsolutePath();
            System.err.printf("Could not generate canonical path, using absolute path instead: %s%n", canonical);
        }

        try {
            System.out.printf("De-indexing %s... ", canonical);
            writer.deleteDocuments(new Term("dir", canonical));
            writer.deleteDocuments(new PrefixQuery(new Term("path", canonical)));
            System.out.println("Finished!");

        } catch (IOException e) {
            System.err.printf("Error de-indexing %s: %s%n", canonical, e.getMessage());
        }
    }

    public void deindex (File dir) {

        if (!writerForNotificationsAvailable()) {

            try (IndexWriter writer = prepareWriter()) {
                deindex(dir, writer);
            } catch (IOException e) {
                System.err.printf("Error closing writer: %s%n", e.getMessage());
            }
        } else
            deindex(dir, writerForNotifications);

    }

    public void listIndexedDirs () {
        for (File dir: getIndexedDirs())
            System.out.println(dir);
    }

    public File[] getIndexedDirs () {

        try (IndexReader reader = DirectoryReader.open(indexDir)) {
            try {
                IndexSearcher searcher = new IndexSearcher(reader);
                ScoreDoc[] docs = searcher.search(new DocValuesFieldExistsQuery("dir"), Integer.MAX_VALUE).scoreDocs;

                ArrayList<File> dirs = new ArrayList<>(docs.length);
                for (ScoreDoc doc: docs)
                    dirs.add(new File(searcher.doc(doc.doc).get("dir")));

                return dirs.toArray(new File[0]);

            } catch (IOException e) {
                System.err.printf("Error reading index: %s%n", e.getMessage());
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.printf("Error closing index: %s%n", e.getMessage());
        }
        return null;
    }

    public void rebuildIndex () {
            index(getIndexedDirs(), true);
    }

    public void watch () {
        try (IndexWriter writer = prepareWriter()) {
            writerForNotifications = writer;
            try {
                IndexedDirectoriesMonitor monitor = new IndexedDirectoriesMonitor(getIndexedDirs());
                monitor.watch(this);
            } catch (IOException e) {
                System.err.printf("Error creating watch service: %s%n", e.getMessage());
            }
        } catch (IOException e) {
            System.err.printf("Error closing writer: %s%n", e.getMessage());
        }
    }
}
