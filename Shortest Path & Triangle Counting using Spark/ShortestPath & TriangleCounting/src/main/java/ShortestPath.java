import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.List;

public class ShortestPath {

    /*
     * This method returns the vertex un each line from graph.txt file.
     */
    public static String getVertex(String inputLine){
        String vertex = "";
        int i=0;
        while (inputLine.charAt(i)!='='){
            vertex = vertex + inputLine.charAt(i);
            i++;
        }
        return vertex;
    }

    /*
     * This method returns vertex data (neighbours, weight, prev, status)
     * attributes from each line of graph.txt file
     */
    public static Data getVertexData(String inputLine, String vertex){
        Data vertexData = new Data();
        String line = inputLine.substring(vertex.length()+1);
        String[] neighbours = line.split(";");
        for (String n: neighbours) {
            String[] nAttributes = n.split(",");
            vertexData.neighbors.add(new Tuple2<>(nAttributes[0],Integer.parseInt(nAttributes[1])));
        }
        vertexData.distance=0;
        vertexData.prev=Integer.MAX_VALUE;
        vertexData.status="INACTIVE";
        return vertexData;
    }


    public static void main(String[] args) {

        //To print only error logs on terminal
        Logger.getLogger("org").setLevel(Level.ERROR);

        String inputFile = args[0];     //input file containing graph
        String sourceVertex = args[1];  //source vertex
        String destVertex = args[2];    //destination vertex

        SparkConf conf = new SparkConf( ).setAppName( "BFS-based Shortest Path Search" );
        JavaSparkContext jsc = new JavaSparkContext( conf );
        JavaRDD<String> lines = jsc.textFile( inputFile );

        //starting the timer
        long startTime = System.currentTimeMillis();

        //creating mapPairs->network for JavaRDD->lines
        //reads the graph.txt and creates JAVAPairRDD with vertex->vertexData
        JavaPairRDD<String, Data> network = lines.mapToPair( line -> {
            String vertex = getVertex(line);
            Data vertexData = getVertexData(line, vertex);
            if (vertex.equals(sourceVertex)){
                vertexData.status="ACTIVE";
            }
            return new Tuple2<>(vertex, vertexData);
        });

        //checking number of active vertices
        long activeNodes = network.filter(vertex -> {
            return vertex._2().status.equals("ACTIVE");
        }).count();

        while (activeNodes>0) {

            // If a vertex is “ACTIVE”, create Tuple2( neighbor, new Data( ... ) ) for
            // each neighbor where Data should include a new distance to this neighbor.
            // Add each Tuple2 to a list. Don’t forget this vertex itself back to the
            // list. Return all the list items.
            JavaPairRDD<String, Data> propagatedNetwork = network.flatMapToPair(vertex-> {
                List<Tuple2<String, Data>> list = new ArrayList<>();
                String vstatus = vertex._2().status;
                int vdist = vertex._2().distance;

                if (vstatus.equals("ACTIVE")){
                    for (Tuple2<String, Integer> n: vertex._2().neighbors) {
                        String nvertex = n._1();
                        // Distance should not be calculated for source vertex
                        // as distance from itself is 0
                        if (nvertex.equals(sourceVertex)){
                            continue;
                        }
                        int nweight = n._2();
                        int newdistance = vdist + nweight;
                        Data nd = new Data();
                        nd.distance = newdistance;
                        list.add(new Tuple2<String, Data>(nvertex, nd));
                    }
                    vertex._2().status = "INACTIVE";
                }
                list.add(new Tuple2<>(vertex._1(), vertex._2()));
                return list.iterator();
            } );

            // For each key, (i.e., each vertex), find the shortest distance and
            // update this vertex’ Data attribute.
            network = propagatedNetwork.reduceByKey( ( k1, k2 ) ->{
                //if there are 2 vertex enteries one with distance 0 and other greater than 0
                // then the greater one is taken i.e. distance of vertex from source is assigned.
                if (k1.distance==0 && k2.distance>0 /* && k1.distance<k2.distance*/){
                    int dist = k2.distance;
                    k2 = k1;
                    k2.distance = dist;
                    return k2;
                } else if (k2.distance==0 && k1.distance>0){
                    int dist = k1.distance;
                    k1 = k2;
                    k1.distance = dist;
                    return k1;
                }
                //if there are 2 vertex enteries with both distnace > 0 then the smaller one is taken
                else{
                    if (k1.distance<k2.distance){
                        if (k1.neighbors==null || k1.neighbors.isEmpty()){
                            int dist = k1.distance;
                            k1 = k2;
                            k1.distance = dist;
                        }
                        return k1;
                    }else{
                        if (k2.neighbors==null || k2.neighbors.isEmpty()){
                            int dist = k2.distance;
                            k2 = k1;
                            k2.distance = dist;
                        }
                        return k2;
                    }
                }
            });

             // If a vertex’ new distance is shorter than prev, activate this vertex
            // status and replace prev with the new distance.
            network = network.mapValues( value -> {
                 // if the distance is not 0 and current distance computed is lesser
                // than previous distance then previous distance is updated and vertex status is activated
                if (value.distance!=0 && value.distance<value.prev){
                    value.prev = value.distance;
                    value.status = "ACTIVE";
                }
                // if distance is not 0 and current distance computed is greater than previously computed distance
                // then current distance is changed back to previous distance. Hre the vertex status is not activated
                // as distance is not changed.
                else if (value.distance!=0 && value.distance>value.prev){
                    value.distance = value.prev;
                }
                return  value;
            });

            //checking for active vertices
            activeNodes = network.filter(vertex -> {
                return vertex._2().status.equals("ACTIVE");
            }).count();
        }

        // printing the shortest distance of destination vertex from source vertex
        for (Tuple2<String, Data> n :network.collect()) {
            if (n._1().equals(destVertex)){
                System.out.println("shortest path: " + n._2().distance);
            }
        }

        //ending the timer
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time:"+ elapsedTime);

    }



}
