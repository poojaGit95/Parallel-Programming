import org.apache.spark.SparkConf;                 // Spark Configuration
import org.apache.spark.api.java.JavaSparkContext; // Spark Context created from SparkConf
import org.apache.spark.api.java.JavaRDD;          // JavaRDD(T) created from SparkContext
import java.util.Arrays;                           // Arrays, List, and Iterator returned from actions
import java.util.Comparator;
import java.io.*;


public class MyClass {
    public static void main( String[] args ) { // a driver program
        // initialize Spark Context
        SparkConf conf = new SparkConf( ).setAppName( "My Driver" );
        JavaSparkContext sc = new JavaSparkContext( conf );

        // read data from a file
        JavaRDD<String> document = sc.textFile( "sample.txt" );
        // read data from another data structure
        JavaRDD<Integer> numbers = sc.parallelize( Arrays.asList(0, 1, 2, 3, 4, 5) );

        // apply tranformations/actions to RDD
        System.out.println( "#words = " +
                document.flatMap( s -> Arrays.asList( s.split( " " ) ).iterator() ).count( ) );
        System.out.println( "max = " + numbers.max( new MyClassMax( ) ) );

        sc.stop( ); // stop Spark Context
    }
}

class MyClassMax implements Serializable, Comparator<Integer> {
    @Override
    public int compare(Integer o1, Integer o2 ) {
        return Integer.compare( o1, o2 );
    }
}


