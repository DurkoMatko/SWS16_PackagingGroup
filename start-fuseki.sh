#!/bin/bash

# load environment
source ~/.swsrc


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
sleep 40;

# beautify data by iterating over corresponding folder
for sparql in $PROJECT_HOME/sparql/init/*.sparql
do
  echo "# run $sparql"
  s-update --service $host/$ds/update --file $sparql
done

) &

# start fuseki in mem
(cd $wrkdir; $FUSEKI_HOME/fuseki-server --localhost --config $cfgfile)

