#!/usr/bin/env bash
# shell script to run StrainRsoAnnotation
. /etc/profile

APPNAME=StrainRsoAnnotation
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu,sjwang@mcw.edu
fi

cd $APPDIR
pwd
java -jar -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@" 2>&1 | tee run.log

/home/rgddata/pipelines/OntologyLoad/run_single.sh RS -skip_download

mailx -s "[$SERVER] StrainRsoAnnotation pipeline OK" $EMAIL_LIST < $APPDIR/run.log
