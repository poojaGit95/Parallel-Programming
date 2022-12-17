#include <iostream>
#include "Timer.h"
#include "mpi.h"
#include <stdlib.h>   // atoi
#include <stdio.h>
#include <math.h>
#include <omp.h>

int default_size = 100;  // the default system size
int defaultCellWidth = 8;
double c = 1.0;      // wave speed
double dt = 0.1;     // time quantum
double dd = 2.0;     // change in system

using namespace std;

int main( int argc, char *argv[] ) {
  int my_rank=0;            // used by MPI
  int mpi_size;           // number of processors

  // verify arguments
  if ( argc != 5 ) {
    cerr << "usage: Wave2D size max_time interval" << endl;
    return -1;
  }

  int size = atoi( argv[1] );
  int max_time = atoi( argv[2] );
  int interval  = atoi( argv[3] );
  int nThreads = atoi( argv[4] );


  if ( size < 100 || max_time < 3 || interval < 0 ) {
    cerr << "usage: Wave2D size max_time interval" << endl;
    cerr << "       where size >= 100 && time >= 3 && interval >= 0" << endl;
    return -1;
  }

  omp_set_num_threads(nThreads);
  MPI_Init(&argc, &argv); // start MPI
  MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
  MPI_Comm_size(MPI_COMM_WORLD, &mpi_size);
  MPI_Status status;

  // create a simulation space
  double z[3][size][size];
  for ( int p = 0; p < 3; p++ ) 
    for ( int i = 0; i < size; i++ )
      for ( int j = 0; j < size; j++ )
	      z[p][i][j] = 0.0; // no wave

  // start a timer
  Timer time;
  time.start( );

  // time = 0;
  // initialize the simulation space: calculate z[0][][]
  int weight = size / default_size;
  for( int i = 0; i < size; i++ ) {
    for( int j = 0; j < size; j++ ) {
      if( i > 40 * weight && i < 60 * weight  &&j > 40 * weight && j < 60 * weight ) {
	      z[0][i][j] = 20.0;
      } else {
	      z[0][i][j] = 0.0;
      }
    }
  }


  // time = 1
  // calculate z[1][][], cells not on edge
  // IMPLEMENT BY YOURSELF !!!
  #pragma omp parallel for
  for( int i = 1; i < size-1; i++){
    for( int j = 1; j < size-1; j++){
      z[1][i][j] = z[0][i][j] +  (c * c)/2 * ((dt * dt)/(dd * dd)) * (z[0][i+1][j] + z[0][i-1][j] + z[0][i][j+1] + z[0][i][j-1] - 4.0 * z[0][i][j]);
    }
  }

  int remainder = size%mpi_size;
  int stripes[mpi_size];

  for (int i=0; i<mpi_size; i++){
    if (i<remainder){
      stripes[i] = stripe+1;
    }else{
      stripes[i] = stripe;
    }
  }

  // simulate wave diffusion from time = 2
  // IMPLEMENT BY YOURSELF !!!
  for ( int t = 2; t < max_time; t++ ) {
    int p = t%3;
    int q = (t+2)%3;
    int r = (t+1)%3;

    //for remainder case:
    if (my_rank==0){    //master exchanges only right data to (rank+1)
        if (mpi_size>1){ //checking if more than 1 processors exist
          MPI_Send(*(*(z+q) + stripes[my_rank]-1) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD ); //sending data of its last lower boundry
          MPI_Recv(*(*(z+q) + stripes[my_rank]), size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD, &status ); //recieving data for its lower boundry+1
        }

    }else if (my_rank==mpi_size-1){ //last processor exchanges its left boundry data to (rank-1)
        if (mpi_size>1){ //checking if more than 1 processors exist
            MPI_Send(*(*(z+q) + (my_rank*stripes[my_rank])+remainder) , size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD ); //sending data of upper first boundry
            MPI_Recv(*(*(z+q) + (my_rank*stripes[my_rank])+remainder-1), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD, &status ); //recieving data for its upper boundry-1
        }
      
    }else{ //all other processors exchange their left and right boundry data to (rank-1) and (rank+1)
        if (mpi_size>1){ //checking if more than 1 processors exist
          if (my_rank<remainder){
            MPI_Send(*(*(z+q) + stripes[my_rank]*my_rank), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD ); //sending its first upper boundry data
            MPI_Recv(*(*(z+q) + (stripes[my_rank]*my_rank)-1), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD, &status); //Recieve data for its upper boundery-1
            MPI_Send(*(*(z+q) + (stripes[my_rank]*(my_rank+1))-1) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD ); //sending its last lower boundry data
            MPI_Recv(*(*(z+q) + stripes[my_rank]*(my_rank+1)) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD, &status); //Recieve data for its lower boundery+1
          }else{
            MPI_Send(*(*(z+q) + (stripes[my_rank]*my_rank)+remainder), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD ); //sending its first upper boundry data
            MPI_Recv(*(*(z+q) + (stripes[my_rank]*my_rank)+remainder-1), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD, &status); //Recieve data for its upper boundery-1
            MPI_Send(*(*(z+q) + (stripes[my_rank]*(my_rank+1))+remainder-1) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD ); //sending its last lower boundry data
            MPI_Recv(*(*(z+q) + (stripes[my_rank]*(my_rank+1))+remainder) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD, &status); //Recieve data for its lower boundery+1

          }
        }
    }


    //computing z data for current t
    
    if (my_rank<remainder){
       #pragma omp parallel for
       for (int i = (my_rank*stripes[my_rank]); i < (my_rank+1)*stripes[my_rank]; i++ ) {
       if (i==0 || i==size-1){
          continue;
        }
        for ( int j = 1; j < size - 1; j++ ) {
          z[p][i][j] = 2.0 * z[q][i][j] - z[r][i][j] + c * c * dt * dt / ( dd * dd ) * ( z[q][i + 1][j] + z[q][i - 1][j] + z[q][i][j + 1] + z[q][i][j - 1] - 4.0 * z[q][i][j] );
        }
      }
    }else{
      #pragma omp parallel for
      for (int i = ((my_rank*stripes[my_rank])+remainder); i < (((my_rank+1)*stripes[my_rank])+remainder); i++ ) {
       if (i==0 || i==size-1){
          continue;
        }
        for ( int j = 1; j < size - 1; j++ ) {
          z[p][i][j] = 2.0 * z[q][i][j] - z[r][i][j] + c * c * dt * dt / ( dd * dd ) * ( z[q][i + 1][j] + z[q][i - 1][j] + z[q][i][j + 1] + z[q][i][j - 1] - 4.0 * z[q][i][j] );
        }
      }

    }

    //printing the data for the interval
    if(interval != 0 && t%interval == 0){
      
      //all workers send their z's data to master.
      if (my_rank!=0){
        if (my_rank<remainder){
          MPI_Send(*(*(z+p)+(stripes[my_rank]*my_rank)), stripes[my_rank]*size, MPI_DOUBLE, 0, 0, MPI_COMM_WORLD );
        }else{
          MPI_Send(*(*(z+p)+(stripes[my_rank]*my_rank)+remainder), stripes[my_rank]*size, MPI_DOUBLE, 0, 0, MPI_COMM_WORLD );
        }
      
      }else{
        //master recieves data from workers
        for (int rank=1; rank<mpi_size; rank++){
          if (rank<remainder){
            MPI_Recv(*(*(z+p) + rank*stripes[rank]), stripes[rank]*size, MPI_DOUBLE, rank, 0, MPI_COMM_WORLD, &status );
          }else{
            MPI_Recv(*(*(z+p) + rank*stripes[rank]+remainder), stripes[rank]*size, MPI_DOUBLE, rank, 0, MPI_COMM_WORLD, &status );
          }
        }
        //printing z data for current time
        cout << t << endl;
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                cout << z[p][j][i] << " ";
            }
            cout << endl;
        }
        cout << endl;
      }
     
    }
  }
   // end of simulation

  MPI_Finalize( ); // shut down MPI

  // finish the timer
  if(my_rank == 0){
    for (int rank=0; rank<mpi_size; rank++){
      if (rank<remainder){
        cerr << "rank[" << rank << "]'s range " << rank*stripes[rank] << "~" << (rank+1)*stripes[rank]-1 << endl;
      }else{
        cerr << "rank[" << rank << "]'s range " << rank*stripes[rank]+remainder << "~" << (rank+1)*stripes[rank]+remainder-1 << endl;
      }
    }
    cerr << "Elapsed time = " << time.lap( ) << endl;
  }  
  return 0;
  
}
