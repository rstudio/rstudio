#
# SessionPresentation.R
#
# Copyright (C) 2020 by RStudio, PBC
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

.rs.addFunction( "showPresentation", function(file = ".") {

   if (!is.character(file))
      stop("file must be of type character")

   invisible(.Call(getNativeSymbolInfo("rs_showPresentation", PACKAGE=""),
                   .rs.normalizePath(path.expand(file))))
})

.rs.addFunction( "showPresentationHelpDoc", function(doc) {

  if (!is.character(doc))
    stop("doc must be of type character")

  invisible(.Call(getNativeSymbolInfo("rs_showPresentationHelpDoc", PACKAGE=""),
                  doc))
})



