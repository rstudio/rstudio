#
# RStudioAPI.R
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

.rs.addFunction("rstudioapi.processRequest", function(requests,
                                                      response,
                                                      secret)
{
   result <- .rs.tryCatch(
      .rs.rstudioapi.processRequestImpl(requests, response, secret)
   )
   
   unlink(requests)
   if (inherits(result, "error")) {
      saveRDS(result, response)
      return(FALSE)
   }
   
   TRUE
})

.rs.addFunction("rstudioapi.processRequestImpl", function(requests,
                                                          response,
                                                          secret)
{
   data <- readRDS(requests)
   if (!identical(data$secret, secret))
      stop("invalid secret in rstudioapi IPC")
   
   call <- data$call
   output <- eval(call, envir = baseenv())
   saveRDS(output, file = response)
})
