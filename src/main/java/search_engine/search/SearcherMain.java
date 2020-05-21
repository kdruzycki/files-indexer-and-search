package search_engine.search;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.terminal.Attributes;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import search_engine.search.SearchResults.SearchResult;

public class SearcherMain {

    private static Path indexPath = Paths.get(System.getProperty("user.home"),".index");

	public static void main(String[] args) {
		try (Terminal terminal = TerminalBuilder.builder()
			.jna(false)
			.jansi(true)
			.build()) {

		    try (LineParser parser = new LineParser(indexPath)){

                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();

                while (true) {
                    try {
                        String line = lineReader.readLine("> ");
                        if (line.startsWith("%"))
                            try {
                                parser.parseCommand(line);
                            } catch (IllegalArgumentException e) {
                                terminal.writer().printf("Wrong command: %s%n", e.getMessage());
                            }
                        else {
                            try {
                                SearchResults results = parser.runQuery(line);
                                terminal.writer().println(new AttributedStringBuilder()
                                        .append("File count: ")
                                        .style(AttributedStyle.DEFAULT.bold())
                                        .append(String.valueOf(results.size()))
                                        .toAnsi());

                                for (SearchResult result: results) {

                                    AttributedStringBuilder builder = new AttributedStringBuilder();
                                    builder.style(AttributedStyle.DEFAULT.bold()).append(result.getPath());

                                    if (result.hasContext()) {
                                        builder.append(":\n").style(AttributedStyle.DEFAULT.boldOff());
                                        builder.append(result.getContext());
                                    }

                                    terminal.writer().println(builder.toAnsi());
                                }
                            } catch (IOException e) {
                                System.err.printf("Error: %s%n", e.getMessage());
                            }

                        }
                    } catch (UserInterruptException | EndOfFileException e) {
                        break;
                    }
                }
            }
		} catch (IOException e) {
			System.err.printf("An error has occurred: %s%n", e);
		}
	}
}
