
import java.io.*;
import java.util.HashMap;
import java.util.Map;


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
                HashMap<String, Integer> vertexNeighMap = new HashMap<>();
                for (String n: neighbours) {
                    String[] nAttributes = n.split(",");
                    vertexNeighMap.put(nAttributes[0], Integer.parseInt(nAttributes[1]));
                }
                vertexData.neighbors = vertexNeighMap;
                vertices.put(vertex, vertexData);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return vertices;
    }

    public int[][] createAdjacencyMatrix(HashMap<String, Data> vertices){
        int size = vertices.size();
        int[][] adjMatrix = new int[size][size];
        for (Map.Entry<String, Data> n : vertices.entrySet()) {
            int vertex = Integer.parseInt(n.getKey());
            HashMap<String, Integer> neighbors = n.getValue().neighbors;
            for (Map.Entry<String, Integer> ng: neighbors.entrySet()) {
                int ng_vertex = Integer.parseInt(ng.getKey());
                int weight = ng.getValue();
                adjMatrix[vertex][ng_vertex] = weight;
            }
        }
        return adjMatrix;
    }


}
