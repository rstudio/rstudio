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

.rs.addJsonRpcHandler("get_shinyapps_account_list", function() {
   shinyapps::accounts()
})

.rs.addJsonRpcHandler("remove_shinyapps_account", function(account) {
   shinyapps::removeAccount(account)
})

.rs.addJsonRpcHandler("get_shinyapps_app_list", function(account) {
   apps <- shinyapps::applications(account)
   appList <- list()
   # Extract the properties we're interested in and hint to the R-JSON
   # converter that these are scalar values
   for (i in seq_len(nrow(apps)))
   {
      appList[[i]] <- list(
         name = .rs.scalar(unlist(apps[i,'name'])),
         url = .rs.scalar(unlist(apps[i,'url'])),
         status = .rs.scalar(unlist(apps[i,'status'])),
         created_time = .rs.scalar(unlist(apps[i,'created_time'])),
         updated_time = .rs.scalar(unlist(apps[i,'updated_time'])))
   }
   return(appList)
})

# The parameter to this function is a string containing the R command from
# the ShinyApps service; we just need to parse and execute it directly.
# The client is responsible for verifying that the statement corresponds to
# a valid ::setAccountInfo command.
.rs.addJsonRpcHandler("connect_shinyapps_account", function(accountCmd) {
   cmd <- parse(text=accountCmd)
   eval(cmd, envir = globalenv())
})
