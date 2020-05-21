package search_engine.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class IndexingFileVisitor implements FileVisitor<Path> {
    private IndexController indexController;

    IndexingFileVisitor(IndexController indexController) {
        super();
        this.indexController = indexController;
    }

    @Override
    public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
        indexController.indexDocument(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        System.err.printf("Error visiting file %s: %s%n", file.toAbsolutePath(), e.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        if (e != null)
            System.err.printf("Error visiting directory %s: %s%n", dir.toAbsolutePath(), e.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }
}
