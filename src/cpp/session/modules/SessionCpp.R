#
# SessionCpp.R
#
# Copyright (C) 2022 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("cpp_is_cpp11_file", function(file)
{
    if (!file.exists(file))
        return(.rs.scalar(FALSE))

    lines <- readLines(file)
    if (any(grepl("cpp11::register", lines)))
        return(.rs.scalar(TRUE))

    return(.rs.scalar(FALSE))
})
