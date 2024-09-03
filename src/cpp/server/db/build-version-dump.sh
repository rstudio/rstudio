#!/bin/bash

if [ ! -d test ] ; then
   echo "Must be run in the <rstudio[-pro]>/src/cpp/server/db directory"
   exit 1
fi

echo "This script is used to create database dumps from the previous release for testing schema upgrades."
echo "Run it the first time you make a schema change for any given release (i.e. when you run make-schema.sh)"
echo "Run it in the repo where the alter script is being committed - either rstudio or rstudio-pro"
echo "If running it in rstudio, run it again in rstudio-pro after merging"
echo "Normally, provide the name and version number of the last release. It uses git to get the"
echo "CreateTables files and populates temp databases with them that are then dumped into db/test to be added to your PR."

read -p "Enter the flower name of the *released* version e.g. Chocolate Cosmos: " flowerName
read -p "Enter the version string, e.g. 2024.04.2+764 for rstudio, otherwise 2024.04.2+764.pro1: " relVersion
read -p "Enter postgres database user: " psqluser
read -p "Enter postgres database password: " PGPASSWORD

encodedFlower=$(echo $flowerName | tr '[:upper:]' '[:lower:]' | tr -s ' ' '-')
versionSuffix=${encodedFlower}-$(echo $relVersion | tr '+' '-' | sed -e s/.pro.*//)

if [[ "$relVersion" == *"pro"* ]]; then
   versionSuffix="${versionSuffix}.workbench"
   echo "Generating schema files for workbench"
else
   echo "Generating schema files for open source"
fi

echo "version-suffix: $versionSuffix"

if git show v${relVersion}:./CreateTables.postgresql > /tmp/${versionSuffix}.psql ; then
   echo "Found postgres schema for ${relVersion}"
else
   echo "Failed to find CreateTables.postgresql with tag: ${relVersion}"
   exit 
fi

export PGPASSWORD

db_name=tmp_db_$$

# Create a new database
if createdb -U $psqluser $db_name ; then
   echo "Temp database created successfully"
else
   echo "Failed to create database with: $psqluser and $PGPASSWORD"
   exit 1
fi


# Import .sql file into the new database
psql -U $psqluser -d $db_name -f /tmp/${versionSuffix}.psql

# Create a dump file of the newly created database
pg_dump -U $psqluser $db_name > "./test/${versionSuffix}.postgresql"

if git show v${relVersion}:./CreateTables.sqlite > /tmp/${versionSuffix}.sqlite ; then
   echo "Found sqlite schema for ${relVersion}"
else
   echo "Failed to find CreateTables.sqlite with tag: ${relVersion}"
   exit 
fi

# Create the SQLite database and import the SQL file
sqlite3 "/tmp/${db_name}.db" < /tmp/${versionSuffix}.sqlite

# Dump the database contents to another file
sqlite3 "/tmp/${db_name}.db" ".output ./test/${versionSuffix}.sqlite.sql" ".dump"
