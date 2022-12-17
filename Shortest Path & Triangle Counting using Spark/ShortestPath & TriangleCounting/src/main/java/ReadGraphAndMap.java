import scala.Tuple2;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadGraphAndMap {

    public HashMap<String, Data> readFile(){
        File file=new File("graph.txt");    //creates a new file instance
        FileReader fr= null;   //reads the file
        HashMap<String, Data> vertices = new HashMap<String, Data>();
        try {
            fr = new FileReader(file);
            BufferedReader br=new BufferedReader(fr);
            String line;
            while((line = br.readLine())!=null){
                Data vertexData = new Data();
                String vertex = "";
                int i=0;
                while (line.charAt(i)!='='){
                    vertex = vertex+line.charAt(i);
                    i++;
                }
                String remLine = line.substring(i+1);
                String[] neighbours = remLine.split(";");
                for (String n: neighbours) {
                    String[] nAttributes = n.split(",");
                    vertexData.neighbors.add(new Tuple2<>(nAttributes[0],Integer.parseInt(nAttributes[1])));
                }
                vertexData.distance=0;
                vertexData.prev=Integer.MAX_VALUE;
                if (vertex.equals("0")){
                    vertexData.status="ACTIVE";
                }else{
                    vertexData.status="INACTIVE";
                }
                vertices.put(vertex, vertexData);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (HashMap.Entry<String, Data> v : vertices.entrySet()) {
            System.out.println("vertex:"+ v.getKey());
            System.out.println(v.getValue().neighbors);
        }

        return vertices;

    }

    public int findshortestPath(HashMap<String, Data> vertices){
        String source = "0";
        String dest = "1500";//String.valueOf(vertices.size()-1);

        List activeNodes = new ArrayList<String>();
        activeNodes.add(source);

        while (!activeNodes.isEmpty()){
            Object vertex = activeNodes.remove(0);
            Data data = vertices.get(vertex);
            List<Tuple2<String, Data>> neighbors = new ArrayList<>();
            for (Tuple2<String, Integer> n : data.neighbors) {
                String v = n._1();
                if (v.equals("0")){
                    continue;
                }
                int weight = n._2();
                Data d = vertices.get(v);
                int newd = data.distance + weight;
                if (d.prev>newd){
                    d.distance = newd;
                    d.status = "ACTIVE";
                    d.prev = newd;
                    vertices.put(v, d);
                    activeNodes.add(v);
                }
            }
        }
        return vertices.get(dest).distance;
    }

    public static void main(String[] args){
        ReadGraphAndMap m = new ReadGraphAndMap();
        HashMap<String, Data> vertices = m.readFile();
        int mindist = m.findshortestPath(vertices);
        System.out.println("shortest path: " + mindist);
    }

}
