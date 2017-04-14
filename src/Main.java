import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by jamescombs on 4/11/17.
 */
public class Main {

    static String OS = System.getProperty("os.name");   // Global OS variable
    static HashMap<String, File> directories = new HashMap<>(); // HashMap to store System data directories
    static HashMap<String, TableFile> tableFiles = new HashMap<>();  // HashMap to store all tables
    static String dataDir = "data"; // Data directory name
    static String userDataDir = "user_data";    // user_data directory name
    static String catalogDir = "catalog";   // catalog directory name
    static String tablesCatalogName = "davisbase_tables.tbl";
    static String colsCatalogName = "davisbase_columns.tbl";

    public static void main(String[] args) {
            boolean dataDirExists = false;
            boolean userDataDirExists = false;
            boolean catalogDirExists = false;

            /*
            Create data directory if they does not exist, then add them to the directories HashMap.
             */

        if (OS.startsWith("Windows")) {
            dataDirExists = createDir(new File(".\\data"));
            userDataDirExists = createDir(new File(".\\data\\user_data"));
            catalogDirExists = createDir(new File(".\\data\\catalog"));
        } else {
            dataDirExists = createDir(new File("./data"));
            userDataDirExists = createDir(new File("./data/user_data"));
            catalogDirExists = createDir(new File("./data/catalog"));
        }

            /*
            Retrieve catalog tables if they exist and add them to the tableFiles HashMap
             */

        retrieveTables(directories.get(catalogDir).getAbsolutePath());

            /*
            Create catalog files if they do not exist.
            First instantiation of DB.
             */

        if (!tableFiles.containsKey("davisbase_tables.tbl")) {
            File tablesCatalog = new File(directories.get(catalogDir).getAbsolutePath() + "/davisbase_tables.tbl");
            try {
                tableFiles.put(tablesCatalog.getName(), new TableFile(tablesCatalog.getAbsolutePath(), "rw"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("Created " + tablesCatalog.getName());

                /*
                Setup B+Tree for tables catalog
                 */

            TableFile catalog = tableFiles.get(tablesCatalogName);
            BPlusTree tree = catalog.getTree();

            if (tree.getNumPageCells() == 0) {

                //Write the catalog since it doesn't have any cells
                Integer rowId = 1;
                Byte numCols = 2;
                byte[] serialCodes = {0x06, (byte) (0x0c + tablesCatalogName.length())};

                for(int i = 0; i < 120; i++) {
                    Cell c1 = new LeafCell(rowId++, numCols, serialCodes, tablesCatalogName.getBytes());
                    catalog.writeCell(c1);
                }
            }
        } else {

            // The tables catalog already exists, so instantiate its B+Tree
            System.out.println("Tables catalog exists. Instantiating its B+Tree");
            TableFile tablesCatalog = tableFiles.get(tablesCatalogName);

        }

        if (!tableFiles.containsKey("davisbase_columns.tbl")) {
            File colsCatalog = new File(directories.get(catalogDir).getAbsolutePath() + "/davisbase_columns.tbl");
            try {
                tableFiles.put(colsCatalog.getName(), new TableFile(colsCatalog.getAbsolutePath(), "rw"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("Created " + colsCatalog.getName());

            // Setup B+Tree for columns catalog

        }

            /*
            Retrieve all tables that may exist and add them to the tableFiles HashMap.
            */

        //boolean hasTables = retrieveTables(directories.get(dataDir).getAbsolutePath());


            /*
            If there were any tables
             */
//        if (hasTables) {
//            printTables();
//        }


    }

    /**
     * Creates system data directories if they do not exist. This is for database initialization.
     *
     * @param dir Directory to create if it doesn't exist.
     */
    static boolean createDir(File dir) {
        boolean alreadyExists = true;
        if (!dir.exists()) {
            System.out.println("Creating system directory " + dir.getName());
            dir.mkdir();
            alreadyExists = false;
        }
        directories.put(dir.getName(), dir);
        return alreadyExists;
    }

    /**
     * Recursively retrieves all tables stored in the database and adds them to the tablesFiles HashMap.
     * @param dir The directory that tables reside in
     * @return True if there were 1 or more tables, false otherwise.
     */
    static boolean retrieveTables(String dir) {
        int i = 0;
        System.out.println("Retrieving files tables from " + dir);
        File directory = new File(dir);

        // Get all the files from a directory
        File[] fList = directory.listFiles();

        for (File file : fList) {
            if (file.isFile()) {
                try {
                    tableFiles.put(file.getName(), new TableFile(file.getAbsolutePath(), "rw"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                i++;
            } else if (file.isDirectory()) {
                retrieveTables(file.getAbsolutePath());
            }
        }

        return (i > 0) ? true : false;
    }

    /**
     * Prints the tables in the database to the user.
     */
    static void printTables() {
        System.out.println("Printing Tables in the database\n---------------------------------------------------------");
        Collection<TableFile> tables = tableFiles.values();
        Iterator<TableFile> tableIt = tables.iterator();
        for (Iterator<TableFile> it = tableIt; it.hasNext(); ) {
            TableFile file = it.next();
            System.out.println(file.toString());
        }
        System.out.println("---------------------------------------------------------");
    }
}

