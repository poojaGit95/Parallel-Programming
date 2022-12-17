import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PrimSeq {

    public void runPrimsAlgorithm(int[][] adjMatrix, int root){
        List mst = new ArrayList<String>();
        List mstVertices = new ArrayList<Integer>();
        int size = adjMatrix.length;
        int[] d = new int[size];
        d[root]=0;
        mstVertices.add(root);
        int mstWt = 0;

        for (int i=0; i<size; i++){
            if (adjMatrix[0][i]>0){
                d[i] = adjMatrix[0][i];
            }else{
                d[i] = Integer.MAX_VALUE;
            }
        }


        for (int i=1; i<=size-1; i++){
            //find min among d s.t. v is not in mstVertices
            int nextMinWtVertex = Integer.MAX_VALUE;
            int minWt = Integer.MAX_VALUE;
            for (int j=0; j<d.length; j++) {
                if (!mstVertices.contains(j) && d[j]!=Integer.MAX_VALUE && d[j]<minWt){
                    nextMinWtVertex = j;
                    minWt = d[j];
                }
            }

            int parent;
            for (int j=0; j<size; j++){
                if (mstVertices.contains(j) && adjMatrix[j][nextMinWtVertex]==minWt){
                    parent = j;
                    mst.add(String.valueOf(parent)+"-"+String.valueOf(nextMinWtVertex));
                    mstWt = mstWt + minWt;
                    break;
                }
            }

            mstVertices.add(nextMinWtVertex);
            for (int k=0; k<size; k++){
                if (!mstVertices.contains(k) && adjMatrix[nextMinWtVertex][k]>0){
                    d[k] = Math.min(d[k], adjMatrix[nextMinWtVertex][k]);
                }
            }
        }
        System.out.println("mst= "+mst.toString());
        System.out.println("mst cost = "+mstWt);

    }

    public static void main(String args[]){
        ReadGraphAndMap readGraphAndMap = new ReadGraphAndMap();
        HashMap<String, Data> vertices = readGraphAndMap.readFile();
        int[][] adjMatrix = readGraphAndMap.createAdjacencyMatrix(vertices);
        PrimSeq primParallel = new PrimSeq();
        long start = System.currentTimeMillis();
        primParallel.runPrimsAlgorithm(adjMatrix, 0);
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println("elapsed time: " +  elapsed);
    }

}
