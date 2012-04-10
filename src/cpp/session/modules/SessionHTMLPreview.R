#
# SessionHTMLPreview.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction( "requiresKnit", function(fileContents, fileType)
{
   if (!.rs.isPackageInstalled("knitr"))
      return (FALSE)

   checkPattern <- function(lines, fileType, pattern)
   {
      pat <- knitr:::all_patterns[[fileType]][[pattern]]
      return (length(pat) && length(grep(pat,lines)))
   }

   lines <- strsplit(fileContents, split="\n")[[1]]

   return (checkPattern(lines, fileType, "chunk.begin") ||
           checkPattern(lines, fileType, "inline.code"))
})


