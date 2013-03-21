#
# SessionData.R
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

.rs.addFunction( "formatDataColumn", function(x, len, ...)
{
   # first truncate if necessary
   if ( length(x) > len )
      length(x) <- len

   # now format
   format(x, trim = TRUE, justify = "none", ...)
})

.rs.registerReplaceHook("View", "utils", function(original, x, title) {
   
   # generate title if necessary
   if (missing(title))
      title <- deparse(substitute(x))[1]
   
   # make sure we are dealing with a data frame
   if (!is.data.frame(x))
      x <- as.data.frame(x)
     
   # call viewData (prepare columns so they are either double or character)   
   invisible(.Call("rs_viewData", 
                   lapply(x, function(col) {
                      if (is.numeric(col)) {
                         storage.mode(col) <- "double"
                         col  
                      }
                      else 
                         as.character(col)
                   }), 
                   title))
})
