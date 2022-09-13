#!/usr/bin/env bash

#
# docker-run-unit-tests.sh
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

set -e
source "$(dirname "${BASH_SOURCE[0]}")/../../../../dependencies/tools/rstudio-tools.sh"

export PATH=/opt/rstudio-tools/dependencies/common/node/${RSTUDIO_NODE_VERSION}/bin/:$PATH
xvfb-run npm test
