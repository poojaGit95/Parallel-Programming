#!/bin/sh

g++ Wave2D_template.cpp Timer.cpp -o Wave2D
javac Wout.java
#./Wave2D 50 1000 800 50

mpic++ Wave2D_template_mpi.cpp Timer.cpp -fopenmp -o Wave2D_mpi

