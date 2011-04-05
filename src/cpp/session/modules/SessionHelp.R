#
# SessionHelp.R
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

# use html help 
options(help_type = "html")

# stop the help server if it was previously started e.g. by .Rprofile
if (tools:::httpdPort > 0L)
  suppressMessages(tools::startDynamicHelp(start=FALSE))

# now restart the help server (this ensures that it picks up the
# options("help.ports") value that we set in SessionHelp.cpp)
suppressMessages(tools::startDynamicHelp())

.rs.addFunction( "setHelprLoadHook", function()
{
   setHook(packageEvent("helpr", "onLoad"),
      function(...)
      {
         helpr:::deactivate_internetz()
         helpr:::set_router_custom_route(TRUE)
      })
})

.rs.addFunction( "handlerLookupError", function(path, query=NULL, ...)
{
   payload = paste(
         "<h3>R Custom HTTP Handler Not Found</h3>",
         "<p>Unable to locate custom HTTP handler for",
          "<i>", path, "</i>",
         "<p>Is the package which implements this HTTP handler loaded?</p>")

   list(payload, "text/html", character(), 404)
});

.rs.addJsonRpcHandler("suggest_topics", function(prefix)
{
   sort(utils:::matchAvailableTopics(prefix))
});

.rs.addJsonRpcHandler("get_help", function(topic, package, options)
{
   helpfiles = help(topic, help_type="html")
   if (length(helpfiles) <= 0)
      return ()

   file = helpfiles[[1]]
   path <- dirname(file)
   dirpath <- dirname(path)
   pkgname <- basename(dirpath)
   
   html = tools:::httpd(paste("/library/", 
                              pkgname, 
                              "/html/", 
                              basename(file),
                              ".html", sep=""),
                        NULL,
                        NULL)$payload

   match = suppressWarnings(regexpr('<body>.*</body>', html))
   if (match < 0)
   {
      html = NULL
   }
   else
   {
      html = substring(html, match + 6, match + attr(match, 'match.length') - 1 - 7)

      match = suppressWarnings(regexpr('<h3>Details</h3>', html))
      if (match >= 0)
         html = substring(html, 1, match - 1)
   }

   obj = tryCatch(get(topic, pos=globalenv()),
                  error = function(e) NULL)
   
   if (is.function(obj))
   {
      sig = .rs.getSignature(obj)
      sig = gsub('^function ', topic, sig)
   }
   else
   {
      sig = NULL
   }
   
   list('html' = html, 'signature' = sig, 'pkgname' = pkgname)
});

.rs.addFunction("helprIsActive", function()
{
   if ("helpr" %in% .packages())
   {
      return ( !identical(helpr:::router_url(), "") &&
               helpr:::router_custom_route())
   }
   else
   {
      return (FALSE)
   }
})

.rs.addJsonRpcHandler("show_help_topic", function(topic, package)
{
   print(help(topic, help_type="html"))
})

.rs.addJsonRpcHandler("search", function(query)
{
   exactMatch = help(query, help_type="html")
   if (length(exactMatch) == 1)
   {
      if (.rs.helprIsActive())
      {
         helpr::print.help_files_with_topic(exactMatch)
      }
      else
      {
         print(exactMatch)
      }
      return ()
   }
   else
   {
      paste("doc/html/Search?name=",
            utils::URLencode(query, reserved = TRUE),
            "&title=1&keyword=1&alias=1",
            sep = "")
   }
})

.rs.addJsonRpcHandler("get_help_links", function(name)
{
   if (name != "history" && name != "favorites")
      return ()
   
   historyFile = file.path(.rs.scratchPath(), paste(name, ".csv", sep=""))
   if (!file.exists(historyFile))
      return ()
   
   history = utils::read.csv(historyFile)
   list(url=as.character(history$url), title=as.character(history$title))
})

# NOTE: It is extremely wasteful for us to send the entire help history
# every time a navigate/clear occurs. Would be much better to send a 
# notification of the navigate only, but that requires doing deduplication
# on the server.

.rs.addJsonRpcHandler("set_help_links", function(name, history.urls, history.titles)
{
   if (name != "history" && name != "favorites")
      return ()
   
   historyFile = file.path(.rs.scratchPath(), paste(name, ".csv", sep=""))
   df = data.frame(url=as.character(history.urls), title=as.character(history.titles))
   utils::write.csv(df, file=historyFile)
})
