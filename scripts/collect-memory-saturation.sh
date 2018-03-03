#!/bin/bash

export LC_ALL="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, SI, SO
while [ -e /proc/$1 ]; do
        /usr/bin/vmstat $TIME_BETWEEN_COMMANDS 1 | awk -v date="$( date +"%s" )" 'NR==3 { print date", "$7", "$8}'
        sleep $TIME_BETWEEN_COMMANDS
done

