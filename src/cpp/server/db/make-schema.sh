#!/bin/bash

if [ -z "$1" ]
then
   echo "Must specify at least one argument."
   echo "Invocation: ./make-schema.sh [upgradeVersion] [sql]"
   echo "Ex: ./make-schema.sh \"Ghost Orchid\" sql"
   exit 1
fi

filename=$(date -u '+%Y%m%d%H%M%S%N')
filename="${filename}_${1}_AlterTables"

filename=$(echo "${filename}" | tr ' ' '-')

if [ -z "$2" ]
then
   filename1="${filename}.sqlite"
   filename2="${filename}.postgresql"
   touch "${filename1}"
   touch "${filename2}"
else
   filename="${filename}.sql"
   touch "${filename}"
fi
