#!/bin/bash

#date kb_read/s kb_write/s
export LC_NUMERIC="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, PID, COMMAND, KB_READ/S, KB_WRITE/S
while [ -e /proc/$1 ]; do
  pidstat -d -U $USER $TIME_BETWEEN_COMMANDS 1 | grep -e '[0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}[ \t]*'$USER | awk -v date="$( date +"%s" )" '{ if (NF) print date", "$3", "$8", "$4", "$5 ; }' 2> /dev/null
done