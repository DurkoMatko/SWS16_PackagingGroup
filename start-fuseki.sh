#!/bin/bash

# load environment

JENA_HOME=$HOME/opt/apache-jena-3.0.1
FUSEKI_HOME=$HOME/opt/apache-jena-fuseki-2.3.1
PROJECT_HOME=$HOME/uibk/sws/project/git

export PATH=$FUSEKI_HOME/bin:$JENA_HOME/bin:$PATH

### CONFIGURATION

tmpdir=/tmp
port=3030
ds=ds
cfgfile=$PROJECT_HOME/etc/fuseki-config.ttl
host=http://localhost:$port

### Script

wrkdir=$tmpdir/swsfuseki
# enforce clean start
rm -rf $wrkdir
mkdir $wrkdir

(

#wait for server
sleep 20;

# beautify data by iterating over corresponding folder
for sparql in $PROJECT_HOME/sparql/init/*.sparql
do
  echo "# run $sparql"
  s-update --service $host/$ds/update --file $sparql
done

) &

# start fuseki in mem
(cd $wrkdir; $FUSEKI_HOME/fuseki-server --localhost --config $cfgfile)
