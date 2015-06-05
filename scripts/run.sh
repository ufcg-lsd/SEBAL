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

OUTPUT_DIR=$2

LEFT_X=$3
UPPER_Y=$4
RIGHT_X=$5
LOWER_Y=$6
NUMBER_OF_PARTITIONS=$7
PARTITION_INDEX=$8

BOUNDING_BOX_PATH=$9

ADDITIONAL_LIBRARY_PATH=$10
LIBRARY_PATH=/usr/local/lib/

MTL_FILE=`ls $IMAGE_NAME/*MTL*`

if [ -n "$ADDITIONAL_LIBRARY_PATH" ]
then
    LIBRARY_PATH=$LIBRARY_PATH:$ADDITIONAL_LIBRARY_PATH
fi

MTL_FILE=`ls $IMAGE_NAME/*MTL*`

OUTPUT_FILE="output_execution_partition_"$PARTITION_INDEX

java -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.BulkMain $MTL_FILE $OUTPUT_DIR $LEFT_X $UPPER_Y $RIGHT_X $LOWER_Y $NUMBER_OF_PARTITIONS $PARTITION_INDEX $BOUNDING_BOX_PATH > $OUTPUT_FILE

echo "Rendenring..."

java -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.render.RenderHelper $MTL_FILE $OUTPUT_DIR $LEFT_X $UPPER_Y $RIGHT_X $LOWER_Y $NUMBER_OF_PARTITIONS $PARTITION_INDEX  >> $OUTPUT_FILE
