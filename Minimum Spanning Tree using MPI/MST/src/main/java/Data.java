import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**                                                                                                                        
 * Vertex Attributes
 */
public class Data implements Serializable {
    HashMap<String,Integer> neighbors; // <neighbor0, weight0>, <neighbor1, weight1>, ...
    String status;                          // "INACTIVE" or "ACTIVE"
    Integer distance;                       // the distance so far from source to this vertex
    Integer prev;                           // the distance calculated in the previous iteration

    public Data(){
        neighbors = new HashMap<>();
        status = "INACTIVE";
        distance = 0;
    }

    public Data( List<HashMap<String,Integer>> neighbors, Integer dist, Integer prev, String status ){
        if ( neighbors != null ) {
            this.neighbors = new HashMap<>();
        } else {
            this.neighbors = new HashMap<>();
        }
        this.distance = dist;
	    this.prev = prev;
        this.status = status;
    }
}
