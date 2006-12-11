#!/bin/sh
# Copyright 2006 Google Inc.
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


if [ $# -ne 3 ]; then
	echo 1>&2 "usage: fix-install-path framework_name new_install_path jnilib"
	exit 1
fi

FRAMEWORK="./Frameworks/${1}.framework/Versions/A/${1}"
INSTALL_NAME=$2
JNILIB=$3

if [ ! -f ${JNILIB} ]; then
	echo 1>&2 "Unable to locate: ${JNILIB}"
	exit 1
fi

if [ ! -f ${FRAMEWORK} ]; then
	echo 1>&2 "Unable to locate: ${FRAMEWORK}"
	exit 1
fi

CURRENT_NAME=`otool -D ${FRAMEWORK} | tail -n 1`
install_name_tool -change "${CURRENT_NAME}" "${INSTALL_NAME}" ${JNILIB}
