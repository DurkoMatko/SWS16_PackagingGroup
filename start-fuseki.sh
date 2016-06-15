#!/bin/bash

### Configure your environment here

PROJECT_HOME=$PWD
FUSEKI_HOME=$HOME/opt/apache-jena-fuseki-2.3.1

### Other configuration

tmpdir=/tmp
port=3030
cfgfile=$PROJECT_HOME/etc/fuseki-config.ttl

### Do stuff

host=http://localhost:$port
ds=ds # This depends on the fuseki-config.ttl
wrkdir=$tmpdir/swsfuseki

# Enforce clean start
rm -rf $wrkdir
mkdir $wrkdir

(

# Wait for fuseki

sleep 20;

# Beautify data by iterating over corresponding folder

for sparql in $PROJECT_HOME/sparql/init/*.sparql
do
  echo "# run $sparql"
  $FUSEKI_HOME/bin/s-update --service $host/$ds/update --file $sparql
done

) &

# Start fuseki in mem
(cd $wrkdir; $FUSEKI_HOME/fuseki-server --port $port --localhost --config $cfgfile)

