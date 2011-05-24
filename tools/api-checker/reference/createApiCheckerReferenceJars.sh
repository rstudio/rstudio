#!/bin/bash

showhelp() {
  echo >&2 "usage: ./createModifiedJars.sh"
  echo >&2 "(1) checks for the existence of gwt-dev.jar and gwt-user.jar"
  echo >&2 "(2) creates gwt-dev-modified.jar and gwt-user-modified.jar (to be used by api-checker) by only including .java files in com/google/gwt"
  exit 1
}

for file in gwt-dev.jar gwt-user.jar
do 
  if [ ! -f $file ]
  then
    echo "[$file] not found - Aborting"
    showhelp
  fi
done

# unpack files in a temporary dir, create a new jar file with only .java files in com/google/gwt
for file in gwt-dev.jar gwt-user.jar
do
  TEMP_DIR=tmp 
  rm -rf ${TEMP_DIR}
  mkdir ${TEMP_DIR}
  cd ${TEMP_DIR} 
  jar -xf ../${file}
  MODIFIED_FILE=gwt-user-modified.jar
  if [ $file != gwt-user.jar ] 
  then 
    MODIFIED_FILE=gwt-dev-modified.jar
  fi 
  jar -cf ../${MODIFIED_FILE} `find com/google/gwt -name *.java` `find com/google/web -name *.java`
  cd ..
  rm -rf ${TEMP_DIR} 
done
