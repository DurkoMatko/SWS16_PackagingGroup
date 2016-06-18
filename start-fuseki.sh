#!/bin/bash

### Configure your environment here

#PROJECT_HOME=$PWD
#export FUSEKI_HOME=$HOME/opt/apache-jena-fuseki-2.3.1
source ~/.swsrc
### Other configuration

tmpdir=/tmp
port=3030
cfgfile=$PROJECT_HOME/etc/fuseki-config.ttl

### Do stuff

host=http://localhost:$port
ds=ds # This depends on the fuseki-config.ttl
wrkdir=$tmpdir/swsfuseki
srvc=$host/$ds
pingquery=sparql/ping.sparql

# Enforce clean start
rm -rf $wrkdir
mkdir $wrkdir

function sep {
  echo '################################################################################'
}

(

# Wait for fuseki

until ( s-query --service $srvc/query --file $pingquery > /dev/null 2>&1 )
do
  sleep 1
done

# Beautify data by iterating over corresponding folder

for sparql in $PROJECT_HOME/sparql/init/*.sparql
do
  echo
  echo "execute ${sparql##*/}"
  sep
  $FUSEKI_HOME/bin/s-update --service $srvc/update --file $sparql
done

echo
sep
echo Done.

) &

# Start fuseki in mem
(cd $wrkdir; $FUSEKI_HOME/fuseki-server --port $port --localhost --config $cfgfile)

