#!/usr/bin/env bash
# shell script to run strain-rso-annotation-pipeline
. /etc/profile

APPNAME=strain-rso-annotation-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST="mtutaj@mcw.edu sjwang@mcw.edu"
fi

cd $APPDIR
java -jar -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" > run.log 2>&1

mailx -s "[$SERVER] strain rso annotation pipeline OK" $EMAIL_LIST < $APPDIR/logs/summary.log

/home/rgddata/pipelines/OntologyLoad/run_single.sh RS -skip_download &

