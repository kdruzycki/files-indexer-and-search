package search_engine.search;

import org.jline.terminal.Terminal;

import java.io.IOException;
import java.nio.file.Path;

public class LineParser implements AutoCloseable {

    private QueryExecutor queryExecutor;

    public LineParser (Path indexPath) throws IOException {
        this.queryExecutor = new QueryExecutor(indexPath);
    }

    @Override
    public void close() throws IOException {
        queryExecutor.close();
    }

    public void parseCommand (String line) throws IllegalArgumentException {
        String[] cmd = line.split("\\s+");
        switch (cmd[0]) {

            case "%lang":
                switch (cmd[1]) {
                    case "pl":
                        queryExecutor.setLang(QueryExecutor.Lang.PL);
                        break;
                    case "en":
                        queryExecutor.setLang(QueryExecutor.Lang.EN);
                        break;
                    default:
                        throw new IllegalArgumentException("Language not found");
                }
                break;

            case "%details":
                switch (cmd[1]) {
                    case "on":
                        queryExecutor.setDetails(true);
                        break;
                    case "off":
                        queryExecutor.setDetails(false);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option");
                }
                break;

            case "%color":
                switch (cmd[1]) {
                    case "on":
                        queryExecutor.setColor(true);
                        break;
                    case "off":
                        queryExecutor.setColor(false);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option");
                }
                break;

            case "%limit":
                queryExecutor.setLimit(Integer.parseInt(cmd[1]));
                break;


            case "%term":
                queryExecutor.setMode(QueryExecutor.Mode.TERM);
                break;


            case "%phrase":
                queryExecutor.setMode(QueryExecutor.Mode.PHRASE);
                break;


            case "%fuzzy":
                queryExecutor.setMode(QueryExecutor.Mode.FUZZY);
                break;

            default:
                throw new IllegalArgumentException("No idea what you mean, mate");
        }
    }

    public SearchResults runQuery (String line) throws IOException {
        return queryExecutor.search(line);
    }
}
