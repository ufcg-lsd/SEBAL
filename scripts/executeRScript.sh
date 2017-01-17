#!/bin/bash

R_ALGORITHM_PATH=$1
R_EXEC_DIR=$2
TMP_DIR_PATH=$3

TIMEOUT=14400

COMMAND="timeout $TIMEOUT Rscript $R_ALGORITHM_PATH $R_EXEC_DIR $TMP_DIR_PATH"

echo "Executing R script..."
if $COMMAND
then
  PROCESS_OUTPUT=$?
  if [ $PROCESS_OUTPUT -ne 0 ]
    exit 1
  fi
else
  exit 1
fi
