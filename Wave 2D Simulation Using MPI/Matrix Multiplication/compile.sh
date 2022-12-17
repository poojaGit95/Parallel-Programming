#!/bin/sh
mpic++ greetings.cpp -o greetings
g++ matrix.cpp Timer.cpp -o matrix
mpic++ matrix_mpi_temp.cpp Timer.cpp -o matrix_mpi_new



