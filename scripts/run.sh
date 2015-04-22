#!/bin/bash
DIRNAME=`dirname $0`
LOG4J=log4j.properties
cd $DIRNAME/..
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi

TAR_FILE=$1
UPPER_X=$2
LEFT_Y=$3
LOWER_X=$4
RIGHT_Y=$5
NUMBER_OF_PARTITIONS=$6
PARTITION_INDEX=$7
FILE_NAME=`ls $TAR_FILE | cut -d . -f1`

mkdir $FILE_NAME
tar -xvzf $TAR_FILE -C $FILE_NAME

MTL_FILE=`ls $FILE_NAME/*MTL*`

echo $TAR_FILE
echo $MTL_FILE

java $CONF_LOG -cp target/SEBAL-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.fogbowcloud.sebal.BulkMain $MTL_FILE $UPPER_X $LEFT_Y $LOWER_X $RIGHT_Y $NUMBER_OF_PARTITIONS $PARTITION_INDEX > /dev/null &
#rm -r $FILE_NAME
