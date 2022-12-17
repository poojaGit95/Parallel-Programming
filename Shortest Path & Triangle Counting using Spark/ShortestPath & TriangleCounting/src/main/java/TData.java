import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import scala.Tuple2;

/**
 * Vertex Attributes
 */
public class TData implements Serializable {
    List<Integer> neighbors;
    String status;
    List<List<Integer>> messageSource;
    int vertex;
    int count;

    public TData(List<List<Integer>> messageSource){
        this.messageSource = messageSource;
    }

    public TData( int vertex, String status, List<Integer> neighbors){
        if ( neighbors != null ) {
            this.neighbors = new ArrayList<>( neighbors );
        } else {
            this.neighbors = new ArrayList<>( );
        }
        this.status = status;
        this.vertex = vertex;
        this.messageSource = new ArrayList<>();
    }
}
