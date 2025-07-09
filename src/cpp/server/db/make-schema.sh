#!/bin/bash

# Initialize variables
sql=""
type="internal"
directory=$(dirname "$(readlink -f "$0")")
flower_name=""
# Default flower_name to contents of version/RELEASE if the file exists
if [ -f "${directory}/../../../../version/RELEASE" ]; then
   flower_name=$(cat "${directory}/../../../../version/RELEASE")
   echo "Using flower name from version/RELEASE: ${flower_name}"
fi

# First, collect all arguments before any option flags into flower_name
while [[ $# -gt 0 && ! "$1" =~ ^- ]]; do
    if [ -z "$flower_name" ]; then
        flower_name="$1"
    else
        flower_name="$flower_name-$1"
    fi
    shift
done

if [ -z "$flower_name" ]; then
   echo "Helper script for creating schema update files."
   echo ""
   echo "Invocation: ./make-schema.sh [\"Release Flower\" [options]"
   echo "Ex: ./make-schema.sh \"Cucumberleaf Sunflower\" -t audit"
   echo ""
   echo "Options:"
   echo ""
   echo "Release Flower: The name of the release flower to be used in the filename."
   echo "                If not specified, the contents of version/RELEASE will be used."
   echo ""
   echo "-s | --sql      SQL provider. One of: sqlite, postgresql."
   echo "                If not specified, both .sqlite and .postgresql files will be created."
   echo ""
   echo "-t | --type     Which type of database the schema is for."
   echo "                One of: internal (default), audit"
   exit 1
fi

# Apply to variables
while [[ $# -gt 0 ]]; do
    case "$1" in
        --sql|-s)
            sql=$2
            shift 2
            ;;

        --type|-t)
            type=$2
            shift 2
            ;;

        *)
            break
            ;;
    esac
done

# Assemble the filename
filename=$(date -u '+%Y%m%d%H%M%S%N')
filename="${filename}_${flower_name}_AlterTables"
filename=$(echo "${filename}" | tr ' ' '-')

# Add audit and change directory if needed
if [ "$type" = "audit" ]; then
   directory="${directory}/audit"
   # if the directory does not exist, create it
   if [ ! -d "$directory" ]; then
      mkdir -p "$directory"
   fi
elif [ "$type" != "internal" ]; then
   echo "Unknown type: ${type}"
   exit 1
fi

echo "Flower name:  ${flower_name}"
echo "Type:         ${type}"
echo "Directory:    ${directory}"

# Create the migration file(s)
filename="${directory}/${filename}"
if [ -z "$sql" ]
then
   filename1="${filename}.sqlite"
   filename2="${filename}.postgresql"
   touch "${filename1}"
   touch "${filename2}"

   echo "SQL provider: [sqlite, postgresql]"
   echo "Filename:     ${filename1}"
   echo "              ${filename2}"
else
   if [ "$sql" = "sqlite" ]
   then
      filename="${filename}.sqlite"
   elif [ "$sql" = "postgresql" ]
   then
      filename="${filename}.postgresql"
   else
      echo ""
      echo "Error: Unknown SQL provider: ${sql}"
      exit 1
   fi

   echo "SQL provider: ${sql}"
   echo "Filename:     ${filename}"
fi


exit 0