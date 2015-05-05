#!/bin/bash

mkdir -p sebal-execution
cd sebal-execution

IMAGE_URL=$1

echo "Downloading SEBAL..."
wget -nc http://www2.lsd.ufcg.edu.br/~giovanni/SEBAL.tar.gz
echo "Downloading image "$IMAGE_URL
wget -nc $IMAGE_URL

tar -xvzf SEBAL.tar.gz
rm SEBAL.tar.gz

IMAGE_FILE=`ls *.gz`
IMAGE_NAME=`echo $IMAGE_FILE | cut -d . -f1`
echo "Image Name: "$IMAGE_NAME

mkdir SEBAL/$IMAGE_NAME
tar -xvzf $IMAGE_FILE -C SEBAL/$IMAGE_NAME

cd SEBAL

LEFT_X=$2
UPPER_Y=$3
RIGHT_X=$4
LOWER_Y=$5
NUMBER_OF_PARTITIONS=$6
PARTITION_INDEX=$7

ADDITIONAL_LIBRARY_PATH=$8
LIBRARY_PATH=/usr/local/lib/

MTL_FILE=`ls $IMAGE_NAME/*MTL*`

if [ -n "$ADDITIONAL_LIBRARY_PATH" ]
then
    LIBRARY_PATH=$LIBRARY_PATH:$ADDITIONAL_LIBRARY_PATH
fi

MTL_FILE=`ls $IMAGE_NAME/*MTL*`

OUTPUT_FILE="output_execution_partition_"$PARTITION_INDEX

java -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.BulkMain $MTL_FILE $LEFT_X $UPPER_Y $RIGHT_X $LOWER_Y $NUMBER_OF_PARTITIONS $PARTITION_INDEX > $OUTPUT_FILE
