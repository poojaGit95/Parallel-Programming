#include "mpi.h"
#include <iostream>
#include "Timer.h"
#include <stdlib.h>   // atoi
#include <math.h>
#include <stdio.h>
#include <omp.h>

int default_size = 100;  // the default system size
int defaultCellWidth = 8;
double c = 1.0;      // wave speed
double dt = 0.1;     // time quantum
double dd = 2.0;     // change in system

using namespace std;

int main( int argc, char *argv[] ) {
  int mpi_rank = 0;            // used by MPI
  int mpi_size;                // Number of machines
  
  // verify arguments
  if ( argc != 5 ) {
    cerr << "usage: Wave2D size max_time interval" << endl;
    return -1;
  }
  int size = atoi( argv[1] );
  int max_time = atoi( argv[2] );
  int interval  = atoi( argv[3] );
  int nThreads = atoi( argv[4] );

  if ( size < 10 || max_time < 3 || interval < 0 || nThreads <= 0) {
    cerr << "usage: Wave2D size max_time interval" << endl;
    cerr << "       where size >= 100 && time >= 3 && interval >= 0 && mpi_size > 0" << endl;
    return -1;
  }

  MPI_Init( &argc, &argv ); // start MPI
  MPI_Comm_rank( MPI_COMM_WORLD, &mpi_rank );
  MPI_Comm_size( MPI_COMM_WORLD, &mpi_size );
  MPI_Status status;
  //Setting the number of threads
  omp_set_num_threads(nThreads);

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
      if( i > 40 * weight && i < 60 * weight  && j > 40 * weight && j < 60 * weight ) {
	      z[0][i][j] = 20.0;
      } 
      else {
        z[0][i][j] = 0.0;
      }
    }
  }

  // time = 1
  // calculate z[1][][] 
  // cells not on edge
  // IMPLEMENT BY YOURSELF !!!
  #pragma omp parallel for
    for( int i = 1; i < size-1; i++ ) {
      for( int j = 1; j < size-1; j++ ) {
        z[1][i][j] = z[0][i][j] +  (c * c)/2 * ((dt * dt)/(dd * dd)) * (z[0][i+1][j] + z[0][i-1][j] + z[0][i][j+1] + z[0][i][j-1] - 4.0 * z[0][i][j]);
    }
  }
  int stripe = size/mpi_size; // Partitioning Stripe

  // simulating wave diffusion from time = 2
  for ( int t = 2; t < max_time; t++ ) {
    int p = t%3;
    int q = (t+2)%3;
    int r = (t+1)%3;

    if (my_rank==0){    //master exchanges only right data to (rank+1)
        if (mpi_size>1){ //checking if more than 1 processors exist
          MPI_Send(*(*(z+q) + stripe-1) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD ); //sending data of its last lower boundry
          MPI_Recv(*(*(z+q) + stripe), size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD, &status ); //recieving data for its lower boundry+1
        }
        

    }else if (my_rank==mpi_size-1){ //last processor exchanges its left boundry data to (rank-1)
        if (mpi_size>1){ //checking if more than 1 processors exist
          MPI_Send(*(*(z+q) + my_rank*stripe) , size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD ); //sending data of upper first boundry
          MPI_Recv(*(*(z+q) + (my_rank*stripe)-1), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD, &status ); //recieving data for its upper boundry-1
        }
        

    }else{ //all other processors exchange their left and right boundry data to (rank-1) and (rank+1)
        if (mpi_size>1){ //checking if more than 1 processors exist
          MPI_Send(*(*(z+q) + stripe*my_rank ), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD ); //sending its first upper boundry data

          MPI_Recv(*(*(z+q) + (stripe*my_rank)-1), size, MPI_DOUBLE, my_rank-1, 0, MPI_COMM_WORLD, &status); //Recieve data for its upper boundery-1

          MPI_Send(*(*(z+q) + stripe*(my_rank+1)-1) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD ); //sending its last lower boundry data

          MPI_Recv(*(*(z+q) + stripe*(my_rank+1)) , size, MPI_DOUBLE, my_rank+1, 0, MPI_COMM_WORLD, &status); //Recieve data for its lower boundery+1
        }
    }

    // if(mpi_rank == 0){ // Master
    //   if(mpi_size>1){ // Checking if there are more than 1 node
    //   // Sending to rank + 1
    //     MPI_Send(*(*(z + q) + stripe-1), size, MPI_DOUBLE, mpi_rank+1, 0, MPI_COMM_WORLD);
        
    //     // Receiving from rank + 1
    //     MPI_Recv(*(*(z + q) + stripe), size, MPI_DOUBLE, mpi_rank+1, 0, MPI_COMM_WORLD, &status);
    //   }
    // }
    // else if(mpi_rank == mpi_size-1){ // Workers
    //   //Sending to rank - 1
    //   if(mpi_size>1){
    //     MPI_Send(*(*(z + q) + stripe * mpi_rank), size, MPI_DOUBLE, mpi_rank-1, 0, MPI_COMM_WORLD);

    //     //Receiving from rank - 1
    //     MPI_Recv(*(*(z + q) + (stripe * mpi_rank)-1), size, MPI_DOUBLE, mpi_rank-1, 0, MPI_COMM_WORLD, &status);
    //   }
    // }
    // else{
    //   if(mpi_size>1){
    //     //Sending to rank - 1
    //     MPI_Send(*(*(z + q) + stripe * mpi_rank), size, MPI_DOUBLE, mpi_rank-1, 0, MPI_COMM_WORLD);

    //     //Sending to rank + 1
    //     MPI_Send(*(*(z + q) + stripe * (mpi_rank+1) - 1), size, MPI_DOUBLE, mpi_rank+1, 0, MPI_COMM_WORLD);

    //     //Receiving from rank - 1
    //     MPI_Recv(*(*(z + q) + (stripe * mpi_rank)-1), size, MPI_DOUBLE, mpi_rank-1, 0, MPI_COMM_WORLD, &status);

    //     //Receiving from rank + 1
    //     MPI_Recv(*(*(z + q) + stripe * (mpi_rank+1)), size, MPI_DOUBLE, mpi_rank+1, 0, MPI_COMM_WORLD, &status);
    //   }
    // }
  
    //Parallelizing the Schrodinger's Formula
    #pragma omp parallel for
      for(int i = mpi_rank*stripe; i < (mpi_rank + 1)*stripe; i++){
        if(i == 0 || i == size-1){
          continue;
        }
        for(int j = 1; j < size-1; j++){
          //Schrodinger's Formula
          z[p][i][j] = 2.0 * z[q][i][j] - z[r][i][j] + c * c * dt * dt / ( dd * dd ) * ( z[q][i + 1][j] + z[q][i - 1][j] + z[q][i][j + 1] + z[q][i][j - 1] - 4.0 * z[q][i][j] );
        }
      }

      if(interval != 0 && t%interval == 0){
        if(mpi_rank == 0){ // Master
          // Master receiving from all the workers
          for(int rank = 1; rank < mpi_size; ++rank){
            MPI_Recv(*(*(z+p) + rank * stripe), stripe*size, MPI_DOUBLE, rank, 0, MPI_COMM_WORLD, &status);
          }
          
          cout << t << endl;
          for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
              cout << z[p][j][i] << " ";
            }
            cout << endl;
          }
          cout << endl;
        }
        else{
          // All workers sending to the master
          MPI_Send(*(*(z+p) + mpi_rank * stripe), stripe*size, MPI_DOUBLE, 0, 0, MPI_COMM_WORLD);
        }
      }
    } // end of simulation
  

  MPI_Finalize();
    
  // finish the timer
  if(mpi_rank == 0){
    cerr << "Elapsed time = " << time.lap( ) << endl;
  }  
return 0;
}