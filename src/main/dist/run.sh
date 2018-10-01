#!/usr/bin/env bash
# shell script to run StrainRsoAnnotation
. /etc/profile

APPNAME=StrainRsoAnnotation
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu
fi

cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export STRAIN_RSO_ANNOTATION_OPTS="$DB_OPTS $LOG4J_OPTS"

bin/$APPNAME "$@" 2>&1 | tee run.log

/home/rgddata/pipelines/OntologyLoad/run_single.sh RS -skip_download

mailx -s "[$SERVER] StrainRsoAnnotation pipeline OK" $EMAIL_LIST < $APPDIR/run.log
