#!/bin/bash

# Global variables
LOG4J_FILE_PATH=/var/log/sebal/sebal.log
SEBAL_SNAPSHOT_M2_PATH=/home/fogbow/.m2/repository/org/fogbowcloud/SEBAL/0.0.1-SNAPSHOT/

function gettingSebalSnapshot {
  # getting sebal snapshot from public_html
  sudo tar -xvzf target.tar.gz
  rm target.tar.gz

  # putting snapshot into .m2
  SEBAL_DIR_PATH=$(pwd)
  sudo mkdir -p $SEBAL_SNAPSHOT_M2_PATH
  sudo cp $SEBAL_DIR_PATH/target/SEBAL-0.0.1-SNAPSHOT.jar $SEBAL_SNAPSHOT_M2_PATH

  sudo mkdir -p $LOG4J_FILE_PATH
}

function verifyRScript {
  echo "Verifying dependencies for R script"
  
  # TODO: put this in our SEBAL repository
  bash -x scripts/verify-dependencies.sh
}

gettingSebalSnapshot
verifyRScript
