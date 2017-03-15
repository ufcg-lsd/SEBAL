#!/bin/bash

cmd=$(free -m | head -n 2 | tail -n 1) 2> /dev/null
while true; do
  echo "$(date +%s)#${cmd}"
  sleep 1
done
