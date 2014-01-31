#
# SessionShinyApps
#
# Copyright (C) 2009-14 by RStudio, Inc.
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

.rs.addFunction("scalarListFromFrame", function(frame)
{
   ret <- list()
   cols <- names(frame)

   # take apart the frame and compose a list of scalars from each row
   for (i in seq_len(nrow(frame))) {
      row <- lapply(cols, function(col) { .rs.scalar(unlist(frame[i,col])) })
      names(row) <- cols
      ret[[i]] <- row
   }
   return(ret)
})

.rs.addJsonRpcHandler("get_shinyapps_account_list", function() {
   shinyapps::accounts()
})

.rs.addJsonRpcHandler("remove_shinyapps_account", function(account) {
   shinyapps::removeAccount(account)
})

.rs.addJsonRpcHandler("get_shinyapps_app_list", function(account) {
   .rs.scalarListFromFrame(shinyapps::applications(account))
})

.rs.addJsonRpcHandler("get_shinyapps_deployments", function(dir) {
   .rs.scalarListFromFrame(shinyapps::readDeployments(dir))
})

# The parameter to this function is a string containing the R command from
# the ShinyApps service; we just need to parse and execute it directly.
# The client is responsible for verifying that the statement corresponds to
# a valid ::setAccountInfo command.
.rs.addJsonRpcHandler("connect_shinyapps_account", function(accountCmd) {
   cmd <- parse(text=accountCmd)
   eval(cmd, envir = globalenv())
})
