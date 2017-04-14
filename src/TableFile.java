/**
 * This class represents a table in the database as a table file. Each TableFile
 * has B+Tree pages that are 512 bytes in size.
 *
 * @author James Combs
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TableFile extends RandomAccessFile {
    public static int pageSize = 512;
    private String name;
    private String mode;
    private BPlusTree bTree;

    public TableFile(String name, String mode) throws FileNotFoundException {
        super(name, mode);
        try {
            this.setLength(pageSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.name = name;
        this.mode = mode;
        bTree = new BPlusTree();
    }

    public BPlusTree getTree() {
        return bTree;
    }

    public void writeCell(Cell cell) {
        if (bTree.getCurPage() instanceof BPlusTree.BTreeLeafPage) {
            writeLeafCell((LeafCell) cell);
        } else {
            writeIndexCell((IndexCell) cell);
        }
    }

    private void writeLeafCell(LeafCell cell) {

//        BPlusTree.BTreeLeafPage curPage = (BPlusTree.BTreeLeafPage) bTree.getCurPage();
        // Get the index of the position to write the cell.
        Integer key = cell.getKey();
        BPlusTree.BTreeLeafPage curPage = (BPlusTree.BTreeLeafPage)bTree.findCell(key);
        Integer pageNumber = bTree.retrievePageNumber(curPage);

        // If the idx is null, the cell doesn't exist in the tree, so we can insert it
        if (curPage != null) {
            System.out.println("Inserting cell");
            System.out.println("Total page size before insertion = " + curPage.overallTotalSize);

            // If the page is full, we need to split it.
            if (curPage.numCells == curPage.maxKeys) {

                System.out.println("The page is full so we must split the page");
                try {
                    this.setLength(2*pageSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                curPage.split(cell);
                bTree.incerementPages();
            } else {

                // Else we simply insert the cell where it belongs in the page and update the page header.
                try {
                    this.seek(curPage.startOfContent - cell.getTotalCellSize());
                    curPage.startOfContent = (short) this.getFilePointer();
                    curPage.offsets.add(curPage.startOfContent);
                    this.writeShort(cell.getTotalBytesOfPayload());
                    this.writeInt(cell.getKey());
                    this.writeByte(cell.getNumCols());
                    this.write(cell.getSerialCodes());
                    this.write(cell.getPayload());
                    curPage.numCells++;
                    curPage.cellKeys.add(cell.getKey());

                    boolean didInsert = false;
                    for(int i = 0; i < curPage.cells.size(); i++) {
                        if(curPage.cells.get(i).compareTo(cell.getKey()) == 0) {
                            curPage.cells.add(i, cell);
                            didInsert = true;
                        }
                    }
                    if(!didInsert) {
                        curPage.cells.add(cell);
                    }

                    curPage.overallTotalSize += cell.getTotalCellSize();
                    updateLeafHeader(curPage);
                    System.out.println("Total page size after insertion = " + curPage.overallTotalSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Cannot insert duplicate primary key into page " + bTree.retrievePageNumber(curPage));
        }
    }

    private void updateLeafHeader(BPlusTree.BTreeLeafPage page) {
        try {
            this.seek(0);
            this.writeByte(0x0d);
            this.writeByte(page.numCells);
            this.writeShort(page.startOfContent);
            this.writeInt(page.rightPointer);

            // Add offsets to offset location array but sorted according to key order.
            for(int i = 0; i < page.offsets.size(); i++) {
                    page.offsetLocations.add(page.offsets.get(i));
            }
            page.totalHeaderSize += 2;  // Update the total header size in the page.
            page.overallTotalSize += 2;

            for (short offset : page.offsetLocations) {
                this.writeShort(offset);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeIndexCell(IndexCell cell) {

    }

    public void printTree() {
        bTree.getCurPage().toString();
    }

    @Override
    public String toString() {
        return "Table - " + name + "\n\t Mode - " + mode;
    }
}
