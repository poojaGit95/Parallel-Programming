import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.List;

public class TriangleCount {


    public static void main(String[] args){
        //To print only error logs on terminal
        Logger.getLogger("org").setLevel(Level.ERROR);

        String inputFile = args[0];     //input file containing graph

        SparkConf conf = new SparkConf( ).setAppName( "BFS-based Shortest Path Search" );
        JavaSparkContext jsc = new JavaSparkContext( conf );
        JavaRDD<String> lines = jsc.textFile( inputFile );

        //starting the timer
        long startTime = System.currentTimeMillis();

        //creating mapPairs->network for JavaRDD->lines
        //reads the graph.txt and creates JAVAPairRDD with vertex->vertexData
        JavaPairRDD<Integer, TData> network = lines.mapToPair( line -> {
            int vertex = Integer.parseInt(line.split("=")[0]);
            List<Integer> neighbors = new ArrayList<>();
            for(String n: line.split("=")[1].split(",")){
                neighbors.add(Integer.parseInt(n));
            }
            String status = "ACTIVE";
            return new Tuple2<>(vertex, new TData(vertex, status, neighbors));
        });

        for (int i=0; i<3; i++){
            if (i<=1){
                //flatmap
                JavaPairRDD<Integer, TData> propagatedNetwork = network.flatMapToPair(vertex-> {
                    List<Tuple2<Integer, TData>> list = new ArrayList<>();
                    String vstatus = vertex._2().status;
                    if (vstatus.equals("ACTIVE")){
                        for (Integer v: vertex._2().neighbors) {
                            if (v < vertex._1()){
                                List<List<Integer>> vMessageSource = new ArrayList<>();
                                if (vertex._2().messageSource.size()!=0){
                                    for (List<Integer> m: vertex._2().messageSource) {
                                        if (!m.contains(vertex._1())){
                                            m.add(vertex._1);
                                        }
                                        vMessageSource.add(m);
                                    }
                                }else{
                                    vMessageSource.add(new ArrayList<>(vertex._1));
                                }
                                list.add(new Tuple2<>(v, new TData(vMessageSource)));
                            }
                        }
                    }
                    return list.iterator();
                } );

                //reducebykey
                network = propagatedNetwork.reduceByKey( ( k1, k2 ) ->{

                    if (k1.neighbors==null && k2.neighbors==null){
                        for(List<Integer> ms : k2.messageSource){
                            k1.messageSource.add(ms);
                        }
                        return k1;
                    } else if (k2.neighbors==null && k1.neighbors!=null){
                        for(List<Integer> ms : k2.messageSource){
                            k1.messageSource.add(ms);
                        }
                        return k1;
                    }else{
                        for(List<Integer> ms : k1.messageSource){
                            k2.messageSource.add(ms);
                        }
                        return k2;
                    }
                });

                //mapvalues
                network = network.mapValues( value -> {
                    if (value.status=="ACTIVE" && value.messageSource.size()==0){
                        value.status = "INACTIVE";
                    }
                    return value;
                });

            }else{

                network = network.mapValues( vertex -> {
                    if (vertex.status=="ACTIVE"){
                        for (List<Integer>m:vertex.messageSource){
                            if (vertex.neighbors.contains(m.get(0)) && vertex.neighbors.contains(m.get(1))){
                                vertex.count+=1;
                            }
                        }
                    }
                    return vertex;
                });

            }
            int triangleCount = 0;
            for (Tuple2<Integer, TData> n :network.collect()) {
                triangleCount += n._2().count;
            }
            System.out.println(triangleCount);
        }

















    }


}
