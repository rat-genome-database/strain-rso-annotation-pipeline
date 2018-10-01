#!/bin/sh

source /home/rgddata/pipelines/pipeUtils/bin/set_env.sh
cd $(dirname $0)

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

$UTILS_HOME/bin/run_sql.sh all.sql Console >> $(dirname $0)/../logs/StrainRsoAnnotation_$($UTILS_HOME/bin/get_log_date.sh).log
/home/rgddata/pipelines/OntologyLoad/run_single.sh RS -skip_download
mailx -s "[$SERVER] Strain-RSO annotation pipeline result" mtutaj@mcw.edu < $(dirname $0)/../logs/StrainRsoAnnotation_$($UTILS_HOME/bin/get_log_date.sh).log
