#
# SessionEnvironment.R
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
.rs.addFunction("valueAsStr", function(val)
{
   tryCatch(
   {
      is.scalarOrVector <- function (x) {
         if (is.null(attributes(x)))
         {
            !is.na(c(NULL=TRUE,
                     logical=TRUE,
                     double=TRUE,
                     integer=TRUE,
                     complex=TRUE,
                     character=TRUE)[typeof(x)])
         }
         else
         {
            FALSE
         }
      }

      if (is.scalarOrVector(val))
      {
         if (length(val) == 1)
         {
            if (nchar(val) < 1024)
                return (deparse(val))
            else
                return (paste(substr(val, 1, 1024), " ..."))
         }
         else if (length(val) > 1)
            return (capture.output(str(val)))
         else
            return ("NO_VALUE")
      }
      else if (.rs.isFunction(val))
         return (.rs.getSignature(val))
      else
         return ("NO_VALUE")
   },
   error = function(e) print(e))

   return ("NO_VALUE")
})

.rs.addFunction("valueContents", function(val)
{
   tryCatch(
   {
      return (capture.output(str(val)))
   },
   error = function(e) print(e))

   return ("NO_VALUE")
})
