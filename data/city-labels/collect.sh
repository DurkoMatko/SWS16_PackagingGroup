#!/bin/bash

rm -f raw.ttl
for city in $(cat $1)
do
  echo $city
  sed "s:<<city>>:$city:g" template > tmp.sparql
  rsparql --service http://dbpedia.org/sparql --query tmp.sparql --results turtle >> raw.ttl
done
