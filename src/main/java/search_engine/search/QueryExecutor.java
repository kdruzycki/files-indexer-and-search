package search_engine.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.uhighlight.PassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;

import search_engine.search.SearchResults.SearchResult;

public class QueryExecutor implements AutoCloseable {

    public enum Mode {
        TERM,
        PHRASE,
        FUZZY
    }

    public enum Lang {
        PL,
        EN
    }

    private Mode mode = Mode.TERM;
    private Lang lang = Lang.EN;
    private IndexSearcher searcher;
    private IndexReader reader;
    private int limit = Integer.MAX_VALUE;
    private boolean details = false;
    private boolean color = true;
    private PassageFormatter blankFormatter = new DefaultPassageFormatter("","","...",false);
    private PassageFormatter colorFormatter = new DefaultPassageFormatter("\033[31m", "\033[0m", "...", false);
    private UnifiedHighlighter polishHighlighter;
    private UnifiedHighlighter englishHighlighter;
    private PolishAnalyzer polishAnalyzer;
    private EnglishAnalyzer englishAnalyzer;


    public QueryExecutor (Path indexDir) throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        searcher = new IndexSearcher(reader);
        polishAnalyzer = new PolishAnalyzer();
        englishAnalyzer = new EnglishAnalyzer();
        polishHighlighter = new UnifiedHighlighter(searcher, polishAnalyzer);
        englishHighlighter = new UnifiedHighlighter(searcher, englishAnalyzer);
        setColor(true);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public SearchResults search (String queryString) throws IOException {
        String field = languageAccurateField();
        Query query;
        switch (mode) {
            case PHRASE:
                query = phraseQuery(field, queryString);
            break;
            case FUZZY:
                query = fuzzyQuery(field, queryString);
            break;
            default:
                query = termQuery(field, queryString);
            break;
        }
        return search(query);
    }

    private SearchResults search (Query query) throws IOException {
        TopDocs docs = searcher.search(query, limit);
        SearchResult[] results = new SearchResult[(int) docs.totalHits.value];
        String[] contexts = null;

        if (details)
            contexts = languageAccurateHighlighter().highlight(languageAccurateField(), query, docs);

        int i = 0;
        for (ScoreDoc scoreDoc: docs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String path = doc.get("path"), context = null;
            if (contexts != null)
                context = contexts[i];
            results[i++] = new SearchResult(path, context);
        }
        return new SearchResults(results);
    }

    private String languageAccurateField() {
        if (lang == Lang.PL)
            return ("contentPolish");
        else
            return ("contentEnglish");
    }

    private Analyzer languageAccurateAnalyzer () {
        if (lang == Lang.PL)
            return polishAnalyzer;
        else
            return englishAnalyzer;
    }

    private UnifiedHighlighter languageAccurateHighlighter () {
        if (lang == Lang.PL)
            return polishHighlighter;
        else
            return englishHighlighter;
    }

    private Query phraseQuery (String field, String terms) {
        QueryBuilder builder = new QueryBuilder(languageAccurateAnalyzer());
        return builder.createPhraseQuery(field, terms);
    }

    private Query termQuery (String field, String term) {
        try (TokenStream tokenStream = languageAccurateAnalyzer().tokenStream(field, term)) {
            List<String> result = new ArrayList<String>();
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                result.add(attr.toString());
            }
            return new TermQuery(new Term(field, String.join(" ", result)));
        } catch (IOException e) {
            return new TermQuery(new Term(field, term));
        }
    }

    private Query fuzzyQuery (String field, String term) {
        return new FuzzyQuery(new Term(field, term));
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public boolean isColorOn() {
        return color;
    }

    public void setColor(boolean color) {
        this.color = color;
        if (color) {
            polishHighlighter.setFormatter(colorFormatter);
            englishHighlighter.setFormatter(colorFormatter);
        } else {
            polishHighlighter.setFormatter(blankFormatter);
            englishHighlighter.setFormatter(blankFormatter);
        }
    }

    public boolean areDetailsOn() {
        return details;
    }

    public void setDetails(boolean details) {
        this.details = details;
    }

    public void setMode (Mode mode) {
        this.mode = mode;
    }

    public Mode getMode () {
        return this.mode;
    }

    public void setLimit (int limit) {
        if (limit > 0)
            this.limit = limit;
        else if (limit == 0)
            this.limit = Integer.MAX_VALUE;
    }

    public int getLimit () {
        return this.limit;
    }
}