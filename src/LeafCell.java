
/**
 * Created by jamescombs on 4/12/17.
 */
public class LeafCell extends Cell {
    private Short totalBytesOfPayload;
    private Integer rowid;
    private Byte numCols;
    private byte[] serialCodes;
    private byte[] payload;
    private int totalCellSize;
    private IndexCell parent;

    public LeafCell(Integer rowid, Byte numCols, byte[] serialCodes, byte[] payload) {
        this.rowid = rowid;
        totalBytesOfPayload = (short)(1 + serialCodes.length + payload.length);
        this.numCols = numCols;
        this.serialCodes = serialCodes;
        this.payload = payload;
        this.totalCellSize = totalBytesOfPayload + 6;
    }

    @Override
    public int getTotalCellSize() {
        return totalCellSize;
    }

    @Override
    public Integer getKey() {
        return rowid;
    }

    public Short getTotalBytesOfPayload() {
        return totalBytesOfPayload;
    }

    public Byte getNumCols() {
        return numCols;
    }

    public byte[] getSerialCodes() {
        return serialCodes;
    }

    public IndexCell getParent() {
        return parent;
    }

    public void setParent(IndexCell parent) {
        this.parent = parent;
    }

    @Override
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public int compareTo(Integer key) {
        return (key <= this.rowid) ? 0 : 1;
    }
}
