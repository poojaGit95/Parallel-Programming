//import mpi.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//
//public class PrimParallel {
//
//    public void runPrimAlgorithm(int[][] adjMatrix, int root, int matrixSize) throws MPIException{
//        int myrank = MPI.COMM_WORLD.Rank();
//        int nprocs = MPI.COMM_WORLD.Size();
//
//        List mst = new ArrayList<String>();
//        int[] d = new int[matrixSize];
//        int[] mstVertices = new int[matrixSize];
//        int mstCost = 0;
//
//        int[] mpiSize = new int[nprocs];
//        int mpiSizeStandard = matrixSize/nprocs;
//        int remainder = matrixSize%nprocs;
//        for (int i=0; i<nprocs; i++){
//            if (i<remainder){
//                mpiSize[i] = mpiSizeStandard+1;
//            }else{
//                mpiSize[i] = mpiSizeStandard;
//            }
//        }
//
//        int start = 0;
//        int end = 0;
//        if (myrank<remainder){
//            start = myrank*mpiSize[myrank];
//            end = start + mpiSize[myrank]-1;
//        }else{
//             start = myrank*mpiSize[myrank]+remainder;
//             end = start+mpiSize[myrank]-1;
//        }
//
//
//        for (int i=start; i<=end; i++){
//            if (adjMatrix[0][i]>0){
//                d[i] = adjMatrix[0][i];
//            }else{
//                d[i] = Integer.MAX_VALUE;
//            }
//        }
//
//        if (myrank==0) {
//            d[root] = 0;
//            mstVertices[root] = 1;
//        }
//
//        MPI.COMM_WORLD.Bcast(mstVertices, 0, mstVertices.length, MPI.INT, 0);
//
//        //vertices_count-1 iterations
//        for (int i=1; i<=matrixSize-1; i++){
//
//            //find min weight in d and its corresponding vertex.
//            int[] curSelectedVertex = new int[2];
//            curSelectedVertex[0] = -1;
//            curSelectedVertex[1] = Integer.MAX_VALUE;
//            int[] nextMinWtVertex = new int[2];
//            nextMinWtVertex[1] = Integer.MAX_VALUE;
//            int minWt = Integer.MAX_VALUE;
//            for (int j=start; j<=end; j++) {
//                if (mstVertices[j]==0 && d[j]!=Integer.MAX_VALUE && d[j]<nextMinWtVertex[1]){
//                    nextMinWtVertex[0] = j;
//                    nextMinWtVertex[1] = d[j];
//                }
//            }
//
//            MPI.COMM_WORLD.Allreduce(nextMinWtVertex, 1, curSelectedVertex, 1, 1, MPI.INT, MPI.MIN);
//
//            for (int j=start; j<=end; j++){
//                for (int k=0; k<matrixSize; k++){
//                    if (mstVertices[j]==1 && curSelectedVertex[1]==adjMatrix[k][j]){
//                        nextMinWtVertex[0] = k;
//                        d[k]=Integer.MAX_VALUE;
//                    }
//                }
//            }
//
//
//            if (myrank!=0){
//                MPI.COMM_WORLD.Send(nextMinWtVertex, 0, 1, MPI.INT, 0, 0);
//            }else{
//                System.out.println("selected vertex wt " + curSelectedVertex[1]);
//                int[] slaveNextMinWtVertex = new int[1];
//                for (int j=1; j<nprocs; j++){
//                    MPI.COMM_WORLD.Recv(slaveNextMinWtVertex, 0, 1, MPI.INT, j, 0);
//                    if (slaveNextMinWtVertex[0]!=-1){
//                        nextMinWtVertex[0] = slaveNextMinWtVertex[0];
//                    }
//                }
//                curSelectedVertex[0] = nextMinWtVertex[0];
//                mstVertices[curSelectedVertex[0]]=1;
//                mstCost = mstCost + curSelectedVertex[1];
//                System.out.println("selected vertex " + curSelectedVertex[0]);
//
//            }
//
//
//            //all worker nodes send their min d value and vertex to master node. Master selects the min d value
//            // sent from the worker nodes and broadcasts the corresponding min d vertex to all nodes.
////            if (myrank!=0){
////                MPI.COMM_WORLD.Send(nextMinWtVertex, 0, nextMinWtVertex.length, MPI.INT, 0, 0);
////            }else{
////                //finding vertex with min d
////                int[] slaveNextMinWtVertex = new int[2];
////                for (int j=1; j<nprocs; j++){
////                    MPI.COMM_WORLD.Recv(slaveNextMinWtVertex, 0, nextMinWtVertex.length, MPI.INT, j, 0);
////                    if (slaveNextMinWtVertex[1]<nextMinWtVertex[1]){
////                        nextMinWtVertex[1] = slaveNextMinWtVertex[1];
////                        nextMinWtVertex[0] = slaveNextMinWtVertex[0];
////                    }
////                }
////                mstVertices[nextMinWtVertex[0]]=1;
////                curSelectedVertex[0] = nextMinWtVertex[0];
////                curSelectedVertex[1] = nextMinWtVertex[1];
////            }
//
//            //the min d's vertex is broadcasted to all worker nodes.
//            MPI.COMM_WORLD.Bcast(mstVertices, 0, mstVertices.length, MPI.INT, 0);
//            MPI.COMM_WORLD.Bcast(curSelectedVertex, 0, curSelectedVertex.length, MPI.INT, 0);
//
////            if (curSelectedVertex[0]>=start && curSelectedVertex[0]<=end){
////                d[curSelectedVertex[0]]=Integer.MAX_VALUE;
////                System.out.println("*****" + myrank + " " + "making d inf");
////            }
//
//            MPI.COMM_WORLD.Barrier();
//
//            //update d values for all vertices having edges from min d's vertex.
//            for (int j=start; j<=end; j++){
//                if (mstVertices[j]==0 && adjMatrix[curSelectedVertex[0]][j]>0){
//                    d[j] = Math.min(d[j], adjMatrix[curSelectedVertex[0]][j]);
//                }
//            }
//            //MPI.COMM_WORLD.Barrier();
//
//            // To find parent of min d.
//            //In order to print the mst edges we need the parent vertex i.e., parent vertex and min d vertex form the
//            // selected MST edge.
////            int[] parent = new int[1];
////            parent[0]=-1;
////            for (int j=start; j<=end; j++){
////                if (mstVertices[j]==1 && adjMatrix[curSelectedVertex[0]][j]==curSelectedVertex[1]){
////                    parent[0] = j;
////                }
////            }
////
////            if (myrank!=0){
////                MPI.COMM_WORLD.Send(parent, 0, parent.length, MPI.INT, 0, 0);
////                //System.out.println("sent: " +  "parent: " + parent  + " " + MPI.COMM_WORLD.Rank());
////            }else{
////                int[] slaveParent = new int[1];
////                for (int j=1; j<nprocs; j++){
////                    MPI.COMM_WORLD.Recv(slaveParent, 0, parent.length, MPI.INT, j, 0);
////                    if (slaveParent[0]!=-1){
////                        parent[0] = slaveParent[0];
////                    }
////                }
////                mst.add(String.valueOf(parent[0])+"-"+String.valueOf(curSelectedVertex[0]));
////                //mstCost = mstCost + curSelectedVertex[1];
////            }
//
//        }
//
//        int[] mstLocalSum = new int[1];
//        int[] mstSum = new int[1];
//        for (int j=start; j<=end; j++){
//            mstLocalSum[0] = mstLocalSum[0] + d[j];
//        }
//
//        MPI.COMM_WORLD.Reduce(mstLocalSum, 0, mstSum, 0, 1, MPI.INT, MPI.SUM, 0);
//
//        if (myrank==0){
//            //System.out.println("mst: " + mst.toString());
//            //System.out.println("MST SUM: " + mstSum[0]);
//            System.out.println("mst--Cost: " + mstCost);
//        }
//    }
//
//
//
//    public static void main(String args[]) throws MPIException{
//        ReadGraphAndMap readGraphAndMap = new ReadGraphAndMap();
//        PrimParallel primParallel = new PrimParallel();
//        HashMap<String, Data> vertices = readGraphAndMap.readFile();
//        int[][] adjMatrix = readGraphAndMap.createAdjacencyMatrix(vertices);
//        MPI.Init(args);
//        int myrank = MPI.COMM_WORLD.Rank();
//
//        long startTime=0;
//        if (myrank == 0) {
//            startTime = System.currentTimeMillis();
//        }
//        primParallel.runPrimAlgorithm(adjMatrix, 0, adjMatrix.length);
//        if (myrank == 0) {
//            long endTime = System.currentTimeMillis();
//            long elapsedTime = endTime-startTime;
//            System.out.println("Elapsed time:" + elapsedTime);
//        }
//        MPI.Finalize();
//
//    }
//}
