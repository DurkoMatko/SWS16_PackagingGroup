#!/bin/bash

rm -f raw.ttl
counter=0
FILE=$1
while read city; 
do
     counter=$((counter+1))
     echo $city -$counter/457
     sed "s:<<city>>:$city:g" template > tmp.sparql
     rsparql --service http://dbpedia.org/sparql --query tmp.sparql --results turtle >> raw.ttl
done < $FILE
