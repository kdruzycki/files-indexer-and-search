package search_engine.indexer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IndexerMain {
    private static Path indexPath = Paths.get(System.getProperty("user.home"),".index");

    public static void main(String[] args) {
        String usage = "java " + IndexerMain.class.getName()
                + " [--purge | --add <dir> | --rm <dir> | --reindex | --list]\n";

        IndexController indexController = new IndexController(indexPath);

        if (args.length == 0) indexController.watch();
        else if (args.length == 1 && args[0].equals("--reindex")) indexController.rebuildIndex();
        else if (args.length == 1 && args[0].equals("--purge")) indexController.clearIndex();
        else if (args.length == 1 && args[0].equals("--list")) indexController.listIndexedDirs();
        else if (args.length == 2 && args[0].equals("--add")) indexController.index(new File(args[1]));
        else if (args.length == 2 && args[0].equals("--rm")) indexController.deindex(new File(args[1]));
        else {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

    }
}
