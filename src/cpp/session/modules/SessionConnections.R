#
# SessionConnections.R
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

.rs.addFunction("validateCharacterParams", function(params) {
   paramNames <- names(params)
   for (param in paramNames) {
      value <- params[[param]]
      if (!is.character(value) || length(value) != 1)
         stop(param, " must be a single element character vector", call. = FALSE)
   }
})

options(connectionViewer = list(
   connectionOpened = function(type, host, finder, connectCode, disconnectCode, ...) {
      .rs.validateCharacterParams(list(
         type = type, host = host, connectCode = connectCode, 
         disconnectCode = disconnectCode
      ))
      if (!is.function(finder))
         stop("finder must be a function", call. = FALSE)
      finder <- paste(deparse(finder), collapse = "\n")
      invisible(.Call("rs_connectionOpened", type, host, finder, 
                      connectCode, disconnectCode))
   },
   connectionClosed = function(type, host, ...) {
      .rs.validateCharacterParams(list(type = type, host = host))
      invisible(.Call("rs_connectionClosed", type, host))
   },
   connectionUpdated = function(type, host, ...) {
      .rs.validateCharacterParams(list(type = type, host = host))
      invisible(.Call("rs_connectionUpdated", type, host))
   }
))


.rs.addFunction("getDisconnectCode", function(finder, host, template) {
   finderFunc <- eval(parse(text = finder))
   name <- finderFunc(globalenv(), host)
   if (!is.null(name))
      sprintf(template, name)
   else
      ""
})
