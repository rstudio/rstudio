#
# SessionRAddins.R
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

assign(".rs.rAddinsEnv", new.env(parent = emptyenv()), envir = .rs.toolsEnv())

.rs.addJsonRpcHandler("prepare_for_addin", function()
{
   .rs.addins.addShinyResponseFilter()
})

.rs.addFunction("addins.addShinyResponseFilter", function()
{
   # save the old filter (if any)
   assign(
      ".rs.addins.savedShinyResponseFilter",
      getOption("shiny.http.response.filter"),
      envir = .rs.toolsEnv())
   
   # register console prompt handler (clean up the filter on next prompt event)
   .Call("rs_registerAddinConsolePromptHandler", PACKAGE = "(embedding)")
   
   # activate our filter
   options(shiny.http.response.filter = .rs.addins.shinyResponseFilter)
})

.rs.addFunction("addins.removeShinyResponseFilter", function()
{
   # restore the saved filter
   options(shiny.http.response.filter = .rs.addins.savedShinyResponseFilter)
   
   # remove the saved filter from tools env
   rm(".rs.addins.savedShinyResponseFilter", envir = .rs.toolsEnv())
})

.rs.addFunction("addins.shinyResponseFilter", function(request, response)
{
   # bail if this isn't the root response
   if (!identical(request$PATH_INFO, "/"))
      return(response)
   
   # ensure text is character rather than raw (not sure if this can happen
   # but other shiny.http.response.filters do this so best to be safe)
   if (is.raw(response$content))
      response$content <- rawToChar(response$content)
   
   # inject our CSS into the response
   inject <- '<head><style type="text/css">select, input { zoom: 1.000001; }</style>'
   response$content <- sub('<head>', inject, response$content, ignore.case = TRUE)
   
   # return the response
   response
})

