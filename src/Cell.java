import java.util.ArrayList;

/**
 * Created by jamescombs on 4/12/17.
 */
public abstract class Cell implements Comparable<Integer>{
    public abstract int getTotalCellSize();
    public abstract Integer getKey();
    public abstract byte[] getPayload();
    public abstract int compareTo(Integer key);
}
