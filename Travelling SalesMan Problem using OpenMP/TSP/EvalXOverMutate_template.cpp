#include <iostream>  // cout
#include <stdlib.h>  // rand
#include <math.h>    // sqrt, pow
#include <omp.h>     // OpenMP
#include <string.h>  // memset
#include "Timer.h"
#include "Trip.h"
#include <algorithm>


#define CHROMOSOMES    50000 // 50000 different trips
#define CITIES         36    // 36 cities = ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
#define TOP_X          25000 // top optimal 25%
#define MUTATE_RATE    10   // optimal 50%

using namespace std;

// newly implemented functions
double calculateCoordinateDistance(double x1, double y1, double x2, double y2);
int getCoordinateIndexOfGene(char city);
bool distanceComparator( Trip trip1, Trip trip2);


//array holding complement value for each city/gene.
const char complementCityArray[36] = {'9', '8', '7', '6', '5', '4', '3', '2', '1', '0', 'Z', 'Y', 'X', 'W', 'V', 'U', 'T', 'S',
    'R', 'Q', 'P', 'O', 'N', 'M', 'L', 'K', 'J', 'I', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A'};

/*
 * Evaluates the distance/fitness of each trip/chromosome and sorts them out in ascending order.
 */
void evaluate( Trip trip[CHROMOSOMES], int coordinates[CITIES][2] ) {

    //parallelizing 
    #pragma omp parallel for
    for(int chromosomes=0; chromosomes<CHROMOSOMES; chromosomes++){
        string chromosome = trip[chromosomes].itinerary;
        double x1 = 0;
        double y1 = 0;
        double x2;
        double y2;
        double distance = 0;

        //calculates total distance/fitness for one entire trip/chromosome.
        for(int gene=0; gene<chromosome.length(); gene++){
            int index = getCoordinateIndexOfGene(chromosome[gene]);
            x2 = coordinates[index][0];
            y2 = coordinates[index][1];
            distance = distance + calculateCoordinateDistance(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        trip[chromosomes].fitness = distance;
        
    } 

    //sorting based on fittness value using in built sort function
    std::sort(trip, trip + CHROMOSOMES, distanceComparator);
}

/*
 * Generates new TOP_X offsprings from TOP_X parents.
 * The i and i+1 offsprings are created from the i and i+1  parents
 */
void crossover( Trip parents[TOP_X], Trip offsprings[TOP_X], int coordinates[CITIES][2] ) {

    #pragma omp parallel for
    for (int offspring=0; offspring<TOP_X; offspring+=2){
        
        string parent1 = parents[offspring].itinerary;
        string parent2 = parents[offspring+1].itinerary;
        string child1;
        string child2;

        char firstParent1Char = parent1.at(0);
        child1.push_back(firstParent1Char);
        char prevCityIdx = getCoordinateIndexOfGene(child1.at(0));

        //array holding visited cities
        int visited[36] = {0};
        visited[prevCityIdx] = 1;

        //child2 has complement cities of child1
        child2.push_back(complementCityArray[prevCityIdx]);

        int p1 = 1;
        int p2 = 0;
        int curIdx = 1;

        //First city is taken from parent1 and next cities are appended from parent1 or parent2 based 
        // shortest on distance among them or based on city not visited.
        while (curIdx<36){
            char curCity1 = parent1.at(p1);
            char curCity2 = parent2.at(p2);

            int idxCurCity1 = getCoordinateIndexOfGene(curCity1);
            int idxCurCity2 = getCoordinateIndexOfGene(curCity2);
            
            if(visited[idxCurCity1]==0 && visited[idxCurCity2]==0){
                double distCurCity1 = calculateCoordinateDistance(coordinates[idxCurCity1][0], coordinates[idxCurCity1][1], coordinates[prevCityIdx][0], coordinates[prevCityIdx][1]);
                double distCurCity2 = calculateCoordinateDistance(coordinates[idxCurCity2][0], coordinates[idxCurCity2][1], coordinates[prevCityIdx][0], coordinates[prevCityIdx][1]);
                //choosing city with smallest distance from last cityin child
                if (distCurCity1 < distCurCity2){
                    child1 = child1 + curCity1;
                    child2.push_back(complementCityArray[idxCurCity1]);
                    prevCityIdx = idxCurCity1;
                    visited[idxCurCity1] = 1;
                    p1++;
                }else{
                    child1 = child1 + curCity2;
                    child2.push_back(complementCityArray[idxCurCity2]);
                    prevCityIdx = idxCurCity2;
                    visited[idxCurCity2] = 1;
                    p2++;
                }
                
            }else if (visited[idxCurCity1]==1 && visited[idxCurCity2]==0){
                //choosing city of parent2 as it is not visited
                child1 = child1 + curCity2;
                child2.push_back(complementCityArray[idxCurCity2]);
                prevCityIdx = idxCurCity2;
                visited[idxCurCity2] = 1;
                p2++;
                p1++;
            
            }else if (visited[idxCurCity1]==0 && visited[idxCurCity2]==1){
                //choosing city of parent1 as it is not visited
                child1 = child1 + curCity1;
                child2.push_back(complementCityArray[idxCurCity1]);
                prevCityIdx = idxCurCity1;
                visited[idxCurCity1] = 1;
                p1++;
                p2++;
                
            }else{
                p1++;
                p2++;
                curIdx--;
            }
            curIdx++;
            
        }

        //adding value of child1 in (i)th offspring
        char charArrayChild1[36];
        strcpy(charArrayChild1, child1.c_str());
        strcpy(offsprings[offspring].itinerary, charArrayChild1);

        //adding complement of child1 in (i+1)th offspring
        char charArrayChild2[36];
        strcpy(charArrayChild2, child2.c_str());
        strcpy(offsprings[offspring+1].itinerary, charArrayChild2);
        

    }

}

/*
 * Mutate - swap a pair of genes in each offspring.
 */
void mutate( Trip offsprings[TOP_X] ){

  for(int offSpring = 0; offSpring < TOP_X; offSpring++){
    char tempOffSpring[CITIES];
    strcpy(tempOffSpring, offsprings[offSpring].itinerary);
    string tempOffSpring_String = tempOffSpring;
    int randomNum = rand() % 100;
    // Checking if the random number generated has value less than the defined mutation rate
    if(randomNum <= MUTATE_RATE){ 
      int value1 = rand()%CITIES;
      int value2 = rand()%CITIES;
      swap(tempOffSpring_String[value1], tempOffSpring_String[value2]);
      strcpy(offsprings[offSpring].itinerary, tempOffSpring_String.c_str());
    }
  }
}

/*
 * Calculates distance between 2 the coordinate points.
 */
double calculateCoordinateDistance(double x1, double y1, double x2, double y2){
    double distance = (sqrt(pow((x1-x2), 2) + pow((y1-y2), 2)));
    return distance;
}

/*
 * Calculates the index of the city as per the cities.txt file 
 * which is required to fetch coordinates of the city.
 */
int getCoordinateIndexOfGene(char city){
    int index = -1;
    if(city>='A'){
        index = city - 'A';
    }else{
        index = city - '0' + 26;
    }
    return index;
}

/*
* Comparator to compares fittness/distance values of 2 genes/trips.
*/
bool distanceComparator( Trip trip1, Trip trip2){	
	return trip1.fitness<trip2.fitness;
}

