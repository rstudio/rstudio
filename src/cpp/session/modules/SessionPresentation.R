#
# SessionPresentation.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction("hasKnitrVersion1", function() {
   .rs.isPackageInstalled("knitr") && .rs.getPackageVersion("knitr") >= "1.0"
})

.rs.addFunction( "showPresentation", function(directory = ".") {

   if (!is.character(directory))
      stop("directory must be of type character")

   invisible(.Call(getNativeSymbolInfo("rs_showPresentation", PACKAGE=""),
                   .rs.normalizePath(path.expand(directory))))
})

.rs.addFunction( "showPresentationHelpDoc", function(doc) {

  if (!is.character(doc))
    stop("doc must be of type character")

  invisible(.Call(getNativeSymbolInfo("rs_showPresentationHelpDoc", PACKAGE=""),
                  doc))
})

.rs.addFunction( "logPresentationEvent", function(file, 
                                                  type, 
                                                  time,
                                                  presentation,
                                                  slide,
                                                  input,
                                                  errors) {
  
  entry <- data.frame(type = type,
                      time = time,
                      presentation = presentation,
                      slide = slide,
                      input = input,
                      errors = errors)
  
  exists <- file.exists(file)
  write.table(entry, 
              file, 
              append=exists, 
              sep = "\n",
              row.names = FALSE,
              col.names = !exists,
              qmethod = "double")  
})

