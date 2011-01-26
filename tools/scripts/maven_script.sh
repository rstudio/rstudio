#!/bin/bash
# 
# Copyright 2010 Google Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

MAVEN_REPO=${MAVEN_REPO:-"~/.m2/repository"}
GWT_VERSION=${GWT_VERSION:-"2.2-rc1"}
GWT_DIR=${GWT_DIR:-"build/lib"}

echo "Pushing GWT jars from ${GWT_DIR} into local maven repo with version ${GWT_VERSION}."
echo "Customize by setting GWT_DIR and GWT_VERSION."

for i in dev user servlet
do
   mvn install:install-file -DgroupId=com.google.gwt -DartifactId=gwt-${i} -Dversion=${GWT_VERSION} -Dpackaging=jar -Dfile=${GWT_DIR}/gwt-${i}.jar -DgeneratePom=true
done 
touch /tmp/empty-fake-soyc-vis.jar
mvn install:install-file -DgroupId=com.google.gwt -DartifactId=gwt-soyc-vis -Dversion=${GWT_VERSION} -Dpackaging=jar -DgeneratePom=true -Dfile=/tmp/empty-fake-soyc-vis.jar
echo "installed the gwt libs in the maven repo"
