#
# SessionCopilot.R
#
# Copyright (C) 2023 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("copilot.setLogLevel", function(level = 0L)
{
   .Call("rs_copilotSetLogLevel", as.integer(level), PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.sendRequest", function(method, params = list())
{
   .Call("rs_copilotSendRequest", as.character(method), as.list(params), PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.networkProxy", function()
{
   # check for proxy from option
   proxy <- getOption("rstudio.copilot.networkProxy")
   if (is.null(proxy))
      return(NULL)
 
   # return as scalar list
   .rs.scalarListFromList(proxy)
})

.rs.addFunction("copilot.authProvider", function()
{
   # check for auth provider from option
   authProvider <- getOption("rstudio.copilot.authProvider")
   if (is.null(authProvider))
      return(NULL)
 
   # return as scalar list
   .rs.scalarListFromList(authProvider)
})


.rs.addFunction("copilot.proxyKerberosServicePrincipal", function()
{
   # check for principal from option
   principal <- getOption("rstudio.copilot.proxyKerberosServicePrincipal")
   if (is.null(principal))
      return(NULL)
 
   # return as scalar list
   .rs.scalarListFromList(principal)
})

.rs.addFunction("copilot.parseNetworkProxyUrl", function(url)
{
   # build regex
   reProxyUrl <- paste0(
      "^",
      "(?:(\\w+)://)?",         # protocol (optional)
      "(?:([^:]+):([^@]+)@)?",  # username + password (optional)
      "([^:]+):(\\d+)",         # host + port (required)
      "/?",                     # optional trailing slash, just in case
      "$"
   )
   
   # attempt to match
   networkProxy <- as.list(regmatches(url, regexec(reProxyUrl, url))[[1L]])
   if (length(networkProxy) != 6L)
      warning("couldn't parse network proxy url '", url, "'", call. = FALSE)
   
   # set names of matched values
   names(networkProxy) <- c("url", "protocol", "user", "pass", "host", "port")
   
   # drop empty strings
   networkProxy[!nzchar(networkProxy)] <- NULL
   
   # validate the protocol, if it was set
   protocol <- .rs.nullCoalesce(networkProxy$protocol, "http")
   if (protocol != "http")
      warning("only 'http' network proxies are supported by the GitHub Copilot agent", call. = FALSE)
   
   # drop the 'url' and 'protocol' fields as they are not used by copilot
   networkProxy[c("url", "protocol")] <- NULL
   
   # port needs to be a number
   networkProxy[["port"]] <- as.integer(networkProxy[["port"]])

   # return rest of the data   
   .rs.scalarListFromList(networkProxy)
})

