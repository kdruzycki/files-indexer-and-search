package search_engine.search;

import java.util.Arrays;
import java.util.Iterator;

public class SearchResults implements Iterable<SearchResults.SearchResult> {

    public static class SearchResult {

        private String context;
        private String path;

        public String getPath() {
            return path;
        }

        public String getContext() {
            return context;
        }

        public boolean hasContext () {
            return context != null;
        }

        SearchResult (String path, String context) {
            this.path = path;
            this.context = context;
        }
    }

    private int number;
    private SearchResult[] results;
    private int curr = 0;

    SearchResults(SearchResult[] results) {
        this.number = results.length;
        this.results = results;
    }

    public int size() {
        return number;
    }

    @Override
    public Iterator<SearchResult> iterator() {
        return Arrays.asList(results).iterator();
    }
}
