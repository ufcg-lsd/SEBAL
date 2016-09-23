#!/bin/bash

# ${IMAGE_NAME}
IMAGE_NAME=$1
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/
IMAGES_DIR_PATH=$2
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/
RESULTS_DIR_PATH=$3
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
OUTPUT_IMAGE_DIR=$4
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt"
IMAGE_MTL_PATH=$5
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"
IMAGE_MTL_FMASK_PATH=$6
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
IMAGE_STATION_FILE_PATH=$7


# Global variables
CONF_FILE=sebal.conf
LIBRARY_PATH=/usr/local/lib
BOUNDING_BOX_PATH=example/boundingbox_vertices

R_EXEC_DIR=SEBAL/workspace/R
R_ALGORITHM_VERSION=AlgoritmoFinal-v2_01072016.R

OUTPUT_IMAGE_DIR=$RESULTS_DIR_PATH/$IMAGE_NAME

SEBAL_DIR_PATH=
LOG4J_PATH=

# This function calls a pre process java code to prepare a station file of a given image
function preProcessImage {
  cd SEBAL
  SEBAL_DIR_PATH=$(pwd)
  LOG4J_PATH=$SEBAL_DIR_PATH/log4j.properties

  #echo "Generating app snapshot"
  #mvn -e install -Dmaven.test.skip=true

  sudo java -Dlog4j.configuration=file:$LOG4J_PATH -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.PreProcessMain $IMAGES_DIR_PATH/ $IMAGE_MTL_PATH $RESULTS_DIR_PATH/ 0 0 9000 9000 1 1 $SEBAL_DIR_PATH/$BOUNDING_BOX_PATH $SEBAL_DIR_PATH/$CONF_FILE $IMAGE_MTL_FMASK_PATH
  sudo chmod 777 $IMAGE_STATION_FILE_PATH
  echo -e "\n" >> $IMAGE_STATION_FILE_PATH
}

# This function prepare a dados.csv file and calls R script to begin image execution
function executeRScript {
  echo "Creating dados.csv for image $IMAGE_NAME"

  cd $R_EXEC_DIR

  echo "File images;MTL;File Station Weather;File Fmask;Path Output" > dados.csv
  echo "$IMAGES_DIR_PATH/$IMAGE_NAME;$IMAGE_MTL_PATH;$IMAGE_STATION_FILE_PATH;$IMAGE_MTL_FMASK_PATH;$OUTPUT_IMAGE_DIR" >> dados.csv
  echo "Executing R script..."
  sudo Rscript $R_EXEC_DIR/$R_ALGORITHM_VERSION $R_EXEC_DIR
  echo "Process finished!"

  echo "Renaming dados file"
  mv dados.csv dados"-${IMAGE_NAME}".csv
  sudo mv dados"-${IMAGE_NAME}".csv $OUTPUT_IMAGE_DIR
}

preProcessImage
executeRScript
