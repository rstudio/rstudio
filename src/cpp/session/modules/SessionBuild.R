#
# SessionBuild.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

setHook("sourceCpp.onBuild", function(file, fromCode, showOutput) {
   .Call("rs_sourceCppOnBuild", file, fromCode, showOutput)
})

setHook("sourceCpp.onBuildComplete", function(succeeded, output) {
   .Call("rs_sourceCppOnBuildComplete", succeeded, output)
})

