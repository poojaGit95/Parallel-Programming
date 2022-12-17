#!/bin/sh
g++ -c pi_monte.cpp -fopenmp
g++ -c Timer.cpp 
g++ pi_monte.o Timer.o -fopenmp -o pi_monte_omp

g++ -c pi_integral.cpp -fopenmp
g++ -c Timer.cpp
g++ pi_integral.o Timer.o -fopenmp -o pi_integral_omp


