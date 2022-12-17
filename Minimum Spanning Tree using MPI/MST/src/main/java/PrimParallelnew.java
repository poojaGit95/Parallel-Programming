import mpi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PrimParallel {

    public void runPrimAlgorithm(int[][] adjMatrix, int root, int matrixSize) throws MPIException{
        int myrank = MPI.COMM_WORLD.Rank();
        int nprocs = MPI.COMM_WORLD.Size();

        int[] d = new int[matrixSize];            //holds key-value of vertices
        int[] mstVertices = new int[matrixSize]; //holds 0 if vertex is not added in MT set else 1

        int[] mpiSize = new int[nprocs];
        int mpiSizeStandard = matrixSize/nprocs;
        int remainder = matrixSize%nprocs;

        //assigning number of vertices to processors
        for (int i=0; i<nprocs; i++){
            if (i<remainder){
                mpiSize[i] = mpiSizeStandard+1;
            }else{
                mpiSize[i] = mpiSizeStandard;
            }
        }

        int start = 0;
        int end = 0;

        if (myrank<remainder){
            start = myrank*mpiSize[myrank];
            end = start + mpiSize[myrank]-1;
        }else{
             start = myrank*mpiSize[myrank]+remainder;
             end = start+mpiSize[myrank]-1;
        }

        //assigning edge wt as key-value to vertices connected to root vertex else assigning infinity.
        for (int i=start; i<=end; i++){
            if (adjMatrix[0][i]>0){
                d[i] = adjMatrix[0][i];
            }else{
                d[i] = Integer.MAX_VALUE;
            }
        }

        //adding root vertex to MST set
        if (myrank==0) {
            d[root] = 0;
            mstVertices[root] = 1;
        }

        //broadcasting MST set to all processors
        MPI.COMM_WORLD.Bcast(mstVertices, 0, mstVertices.length, MPI.INT, 0);

        // (vertices - 1) iterations
        for (int i=1; i<=matrixSize-1; i++){

            //Note: In below code instead of MPIReduce following code is used to find min d
            // and  min d's vertex in single iteration. Using MPIReduce gives only min d value,
            // then vertex holding min d should be found using MPISend & MPIRecv

            //find min key-value in d and its corresponding vertex.
            int[] curSelectedVertex = new int[2];
            curSelectedVertex[0] = -1;                //final selected min d vertex
            curSelectedVertex[1] = Integer.MAX_VALUE; //final selected min d value
            int[] nextMinWtVertex = new int[2];       //min d vertex & value for each processor's vertices
            nextMinWtVertex[1] = Integer.MAX_VALUE;
            int minWt = Integer.MAX_VALUE;

            for (int j=start; j<=end; j++) {
                if (mstVertices[j]==0 && d[j]!=Integer.MAX_VALUE && d[j]<nextMinWtVertex[1]){
                    nextMinWtVertex[0] = j;      //hold's min d vertex
                    nextMinWtVertex[1] = d[j];  //holds min d's value
                }
            }

            //all worker nodes send their min d value and vertex to master node. Master selects the min d value
            // sent from the worker nodes and broadcasts the corresponding min d vertex to all nodes.
            if (myrank!=0){
                MPI.COMM_WORLD.Send(nextMinWtVertex, 0, nextMinWtVertex.length, MPI.INT, 0, 0);
            }else{
                //finding vertex with min d
                int[] slaveNextMinWtVertex = new int[2];
                for (int j=1; j<nprocs; j++){
                    MPI.COMM_WORLD.Recv(slaveNextMinWtVertex, 0, nextMinWtVertex.length, MPI.INT, j, 0);
                    if (slaveNextMinWtVertex[1]<nextMinWtVertex[1]){
                        nextMinWtVertex[1] = slaveNextMinWtVertex[1];
                        nextMinWtVertex[0] = slaveNextMinWtVertex[0];
                    }
                }
                mstVertices[nextMinWtVertex[0]]=1;
                curSelectedVertex[0] = nextMinWtVertex[0];
                curSelectedVertex[1] = nextMinWtVertex[1];
            }

            //the min d's vertex is broadcasted to all worker nodes.
            MPI.COMM_WORLD.Bcast(mstVertices, 0, mstVertices.length, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(curSelectedVertex, 0, curSelectedVertex.length, MPI.INT, 0);

            //update d values for all vertices having edges from min d's vertex.
            for (int j=start; j<=end; j++){
                if (mstVertices[j]==0 && adjMatrix[curSelectedVertex[0]][j]>0){
                    d[j] = Math.min(d[j], adjMatrix[curSelectedVertex[0]][j]);
                }
            }
        }

        int[] mstLocalSum = new int[1];
        int[] mstSum = new int[1];
        for (int j=start; j<=end; j++){
            mstLocalSum[0] = mstLocalSum[0] + d[j];
        }

        //mst sum can be obtained from sum of all values of d array
        MPI.COMM_WORLD.Reduce(mstLocalSum, 0, mstSum, 0, 1, MPI.INT, MPI.SUM, 0);

        if (myrank==0){
            System.out.println("MST SUM: " + mstSum[0]);
        }
    }


    public static void main(String args[]) throws MPIException{

        ReadGraphAndMap readGraphAndMap = new ReadGraphAndMap();
        PrimParallel primParallel = new PrimParallel();

        //reading the graph.txt file
        HashMap<String, Data> vertices = readGraphAndMap.readFile();

        // creating adjacency matrix from graph.txt file
        int[][] adjMatrix = readGraphAndMap.createAdjacencyMatrix(vertices);

        MPI.Init(args);
        int myrank = MPI.COMM_WORLD.Rank();

        long startTime=0;
        if (myrank == 0) {
            startTime = System.currentTimeMillis();
        }

        //executing Prim's algorithm
        primParallel.runPrimAlgorithm(adjMatrix, 0, adjMatrix.length);

        if (myrank == 0) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime-startTime;
            System.out.println("Elapsed time:" + elapsedTime);
        }
        MPI.Finalize();

    }
}
