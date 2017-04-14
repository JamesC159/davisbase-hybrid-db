/**
 * Created by jamescombs on 4/12/17.
 */
public class IndexCell extends Cell {
    private Integer leftPointer;
    private Integer keyDelim;
    private int totalCellSize;

    public IndexCell(Integer key) {
        totalCellSize = 8;
        keyDelim = key;
    }

    public Integer getLeftPointer() {
        return leftPointer;
    }

    public void setLeftPointer(Integer leftPointer) {
        this.leftPointer = leftPointer;
    }

    @Override
    public int getTotalCellSize() {
        return totalCellSize;
    }

    @Override
    public Integer getKey() {
        return keyDelim;
    }

    @Override
    public byte[] getPayload() {
        return null;
    }

    @Override
    public int compareTo(Integer key) {
        return (key <= this.keyDelim) ? 0 : 1;
    }
}
