#!/bin/bash

export LC_NUMERIC="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, USED, PARAM1, PARAM2, PARAM4, PARAM5
while [ -e /proc/$1 ]; do
  ps -o pid,user,%mem,command ax | sort -b -k3 -r | grep /usr/lib/R/bin/exec/R | awk -v date="$( date +"%s" )" '{s+=$3} END { print date", "s", "$1", "$2", "$4", "$5}' 2> /dev/null
  sleep $TIME_BETWEEN_COMMANDS
done
