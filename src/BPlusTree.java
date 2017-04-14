import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Created by jamescombs on 4/11/17.
 */
public class BPlusTree {
    private HashMap<Integer, BTreePage> numToTreePageMap;   // Map from file page numbers to tree page
    private HashMap<BTreePage, Integer> treePageToNumMap;   // Map from tree page to file page numbers
    private BTreePage root; // Root page of the Tree
    private BTreePage curPage;
    private int numPages;   // number of pages in the B+Tree as well as represents each BTreePage number

    /**
     * Internal abstract B+Tree page
     */
    protected abstract class BTreePage {
        public static final int pageSize = 512;
        public static final int maxKeys = 10;
        public static final int maxChildren = maxKeys + 1;
        public Integer overallTotalSize;
        public Integer totalHeaderSize;
        public Byte pageType;
        public byte numCells;
        public Short startOfContent;
        public Integer rightPointer;
        public ArrayList<Short> offsetLocations;
        public TreeSet<Integer> cellKeys;
        public ArrayList<Cell> cells;
        public ArrayList<Short> offsets;
        public BTreePage parent;

        public BTreePage(Byte pageType) {
            this.pageType = pageType;
            numCells = 0;
            startOfContent = (short) (pageSize);
            rightPointer = -1;
            offsetLocations = new ArrayList<>();
            cellKeys = new TreeSet<>();
            cells = new ArrayList<>();
            offsets = new ArrayList<>();
            totalHeaderSize = 8;
            overallTotalSize = totalHeaderSize;
            treePageToNumMap.put(this, numPages);
            numToTreePageMap.put(numPages, this);
            numPages++;
            parent = null;
        }

        public void propogate(Cell mid, BTreePage newPage) {

            System.out.println("\nPropagating Data up the tree");

            IndexCell c = new IndexCell(mid.getKey());

            // If there was no parent.
            if (parent == null) {

                System.out.println("The page had no parent, so creating a new one");

                // Create the new parent
                BTreeIndexPage newParent = new BTreeIndexPage((byte) 0x02);
                System.out.println("New parent page number = " + retrievePageNumber(newParent));
                System.out.println("New parent right page pointer before update = " + newParent.rightPointer);
                newParent.rightPointer = retrievePageNumber(newPage);
                System.out.println("New parent right page pointer fter update = " + newParent.rightPointer);


                System.out.println("Middle key " + c.getKey() + " left pointer before updating = " + c.getLeftPointer());
                    c.setLeftPointer(retrievePageNumber(this));
                System.out.println("Middle key " + c.getKey() + " left pointer after updating  = " + c.getLeftPointer());

                newParent.cells.add(c);
                    newParent.numCells++;

                System.out.println("Current page parent before parent update = " + retrievePageNumber(parent));
                System.out.println("New Page parent before updating = " + retrievePageNumber(newPage.parent));
                parent = newParent;
                newPage.parent = newParent;
                System.out.println("Current page parent after parent update = " + retrievePageNumber(parent));
                System.out.println("New Page parent after updating = " + retrievePageNumber(newPage.parent));
                root = newParent;

            } else {

                // If the parent is not full
                if (parent.cells.size() < parent.maxKeys) {
                    System.out.println("Parent existed and is not full, inserting key into parent");

                    // Insert the new page smallest key and pointer into the parent.
                    boolean didInsert = false;
                    for (int i = 0; !didInsert && i < parent.cells.size(); i++) {
                            if (parent.cells.get(i).compareTo(c.getKey()) == 0) {
                                parent.cells.add(i, c);
                                parent.numCells++;
                                System.out.println("Middle cell left pointer before updating = " + ((IndexCell)parent.cells.get(i)).getLeftPointer());
                                ((IndexCell) parent.cells.get(i)).setLeftPointer(retrievePageNumber(newPage));
                                System.out.println("Middle cell left pointer after updating = " + ((IndexCell)parent.cells.get(i)).getLeftPointer());
                                didInsert = true;
                            }
                        }

                    if (!didInsert) {
                        // Must insert at the end since the key is greater than all keys in the page.
                        System.out.println("Adding key " + c.getKey() + " to end of parent page");
                        parent.cells.add(c);
                        parent.numCells++;
                        System.out.println("Parent page " + retrievePageNumber(parent) + " right pointer before update = " + parent.rightPointer);
                        parent.rightPointer = retrievePageNumber(newPage);
                        System.out.println("Parent page " + retrievePageNumber(parent) + " right pointer after update = " + parent.rightPointer);
                    }
                } else {

                    System.out.println("The parent was full so we must split the parent and continue propagating");
                    // The parent is full, so we must split the parent
                    ((BTreeIndexPage) this.parent).split(c, this, newPage);
                }

                // Update the new page's parent
                newPage.parent = this.parent;

            }
    }

    public String toString() {
        System.out.println("Printing the tree");
        String s = "";
        for (int i = 0; i < cells.size(); i++) {
            s += (cells.get(i)).getKey() + " ";
        }
        return s + "#";
    }

    public abstract byte[] toBytes();
}

/**
 * Internal Leaf Page
 */
protected class BTreeLeafPage extends BTreePage {

    public BTreeLeafPage(Byte pageType) {
        super(pageType);
    }

    public void split(Cell cell) {

        System.out.println("\nNow splitting the leaf page " + retrievePageNumber(this));

        BTreeLeafPage right = new BTreeLeafPage((byte) 0x0d);
        System.out.println("New right page number = " + retrievePageNumber(right));

        // Insert and overpack the cells in the current page
        boolean dnodeinserted = false;
        for (int i = 0; !dnodeinserted && i < cells.size(); i++) {
            if (((LeafCell) cells.get(i)).compareTo(cell.getKey()) == 0) {
                System.out.println("Inserting key inside page to overpack the page");
                cells.add(i, cell);
                numCells++;
                dnodeinserted = true;
            }
        }
        if (!dnodeinserted) {
            System.out.println("Inserting cell to very right of page");
            cells.add(cells.size(), cell);
            numCells++;
        }


        // Calculate the split location in the current page we are splitting.
        int splitlocation;
        if (maxKeys % 2 == 0) {
            splitlocation = maxKeys / 2;
        } else {
            splitlocation = (maxKeys + 1) / 2;
        }

        // Move upper half of cells in current page to the new page.
        for (int i = cells.size() - splitlocation; i > 0; i--) {
            right.cells.add(cells.remove(splitlocation));
            right.numCells++;
            numCells--;
        }

        // If the page is a leaf page, then we must update the leaf pointers.
        // We do not need to update children pointers of index pages because
        //  removing the actual cells from the current page and adding them to the
        //  new page is going to bring along the left child pointers. But we do
        //  need to update the right child pointer since it is apart of the page header.

        System.out.println("Updating next leaf pointers");
        System.out.println("Current page right pointer before updating = " + rightPointer);
        System.out.println("New right page pointer before updating = " + right
                .rightPointer);

        right.rightPointer = rightPointer;
        rightPointer = retrievePageNumber(right);

        System.out.println("Current page right pointer after updating = " + rightPointer);
        System.out.println("New right page pointer after updating = " + right
                .rightPointer);

        // Propogate the data into the parents
        propogate(cells.get(cells.size() - 1), right);
    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }
}

/**
 * Internal Index Page
 */
protected class BTreeIndexPage extends BTreePage {

    public BTreeIndexPage(Byte pageType) {
        super(pageType);
    }

    public void split(Cell cell, BTreePage left, BTreePage right) {
        System.out.println("\nNow splitting the index page " + retrievePageNumber(this));

        int splitLocation, insertLocation = 0;

        // Floor split
        if (maxKeys % 2 == 0) {
            splitLocation = maxKeys / 2;
        } else {
            splitLocation = (maxKeys + 1) / 2 - 1;
        }

        // insert dnode into the vector (it will now be overpacked)
        boolean dnodeinserted = false;
        for (int i = 0; !dnodeinserted && i < cells.size(); i++) {
            if (cells.get(i).compareTo(cell.getKey()) == 0) {
                System.out.println("Inserting key + " + cell.getKey() + " inside of the index page " + retrievePageNumber(this));
                cells.add(i, cell);
                numCells++;
                System.out.println("Current page " + retrievePageNumber(this) + " left pointer before update = " + ((IndexCell) cells.get(i)).getLeftPointer());
                ((IndexCell) cells.get(i)).setLeftPointer(retrievePageNumber(left));
                System.out.println("Current page " + retrievePageNumber(this) + " left pointer before update = " + ((IndexCell) cells.get(i)).getLeftPointer());
                System.out.println("Current page " + retrievePageNumber(this) + " right pointer before update = " + rightPointer);
                rightPointer = retrievePageNumber(right);
                System.out.println("Current page " + retrievePageNumber(this) + " right pointer after update = " + rightPointer);
                dnodeinserted = true;

                // set the location of the insert this will be used to set the parent
                insertLocation = i;
            }
        }

        if (!dnodeinserted) {
            System.out.println("Inserting key " + cell.getKey() + " at end of the index page " + retrievePageNumber(this));
            insertLocation = cells.size();
            cells.add(cell);
            numCells++;
            ((IndexCell) cells.get(cells.size() - 1)).setLeftPointer(retrievePageNumber(left));
            System.out.println("Current page " + retrievePageNumber(this) + " right pointer before update = " + rightPointer);
            rightPointer = retrievePageNumber(right);
            System.out.println("Current page " + retrievePageNumber(this) + " right pointer after update = " + rightPointer);
        }

        // Get middle data
        IndexCell mid = (IndexCell) cells.remove(splitLocation);
        numCells--;

        // Create a new page for the split
        BTreeIndexPage newRight = new BTreeIndexPage((byte) 0x02);
        System.out.println("Created new right page " + retrievePageNumber(newRight) + " for the split");

        // populate the data and pointers of the new right node
        for (int i = cells.size() - splitLocation; i > 0; i--) {
            newRight.cells.add(cells.remove(splitLocation));
            newRight.numCells++;
            numCells--;
        }

        System.out.println("New right page " + retrievePageNumber(newRight) + " right pointer before update = " + newRight.rightPointer);
        newRight.rightPointer = rightPointer;
        System.out.println("New right page " + retrievePageNumber(newRight) + " right pointer after update = " + newRight.rightPointer);

        if (insertLocation < splitLocation) {
            left.parent = parent;
            right.parent = parent;
        } else if (insertLocation == splitLocation) {
            left.parent = parent;
            right.parent = newRight;
        } else {
            left.parent = newRight;
            right.parent = newRight;
        }

        propogate(mid, newRight);

    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

}

        /*
        Start of BPlusTree methods
         */

    public BPlusTree() {
        numPages = 0;
        numToTreePageMap = new HashMap<>();
        treePageToNumMap = new HashMap<>();
        root = new BTreeLeafPage((byte) 0x0d);
        curPage = root;
    }

        /*
        Getters
         */

    public BTreePage getCurPage() {
        return curPage;
    }

    public int getNumPageCells() {
        return curPage.numCells;
    }

    public BTreePage getRoot() {
        return root;
    }

    public BTreeLeafPage getNewLeafPage() {
        return new BTreeLeafPage((byte) 0x0d);
    }

    public BTreeIndexPage getNewIndexPage() {
        return new BTreeIndexPage((byte) 0x02);
    }

    public BTreePage retrieveTreePage(Integer pageNumber) {
        return numToTreePageMap.get(pageNumber);
    }

    public Integer retrievePageNumber(BTreePage page) {
        return treePageToNumMap.get(page);
    }

        /*
        Setters
         */

    public void incerementPages() {
        numPages++;
    }

        /*
        Tree Insertion methods
         */

    public void InsertPage(BTreePage page, Integer key) {

    }

        /*
        Tree Tranversal Methods
         */

    public BTreePage findCell(Integer key) {
        System.out.println("Searching for key = " + key);
        return findCell(root, key);
    }

    public BTreePage findCell(BTreePage page, Integer key) {
        // Base case = leaf page.
        if (page.getClass().getName().trim().equals("BPlusTree$BTreeLeafPage")) {
            System.out.println("Searching leaf page " + retrievePageNumber(page));
            for (Cell cell : page.cells) {
                LeafCell c = (LeafCell) cell;
                System.out.println("\tLeaf page key = " + c.getKey());
                if (c.getKey() == key) {
                    System.out.println("\tFound key " + key);
                    return null;
                }
            }
            System.out.println("\tKey " + key + " did not exist");
            return page;
        } else {
            // Otherwise search through all cells, and take the pointer to the next lower level.
            System.out.println("Searching index page " + retrievePageNumber(page));
            BTreeIndexPage idxPage = (BTreeIndexPage) page;
            for (Cell cell : page.cells) {
                IndexCell c = (IndexCell) cell;
                System.out.println("\tIndex page key = " + c.getKey());
                if (key <= c.getKey()) {
                    System.out.println("\tTaking left page pointer " + c.getLeftPointer());
                    return findCell(numToTreePageMap.get(c.getLeftPointer()), key);
                }
            }
            System.out.println("\tTaking right page pointer " + page.rightPointer);
            return findCell(numToTreePageMap.get(page.rightPointer), key);
        }
    }
}
