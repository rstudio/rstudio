#
# SessionHelp.R
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

# use html help 
options(help_type = "html")

.rs.addFunction( "httpdPort", function()
{
   as.character(tools:::httpdPort)
})

.rs.addFunction("initHelp", function(port)
{
   # stop the help server if it was previously started e.g. by .Rprofile
   if (tools:::httpdPort > 0)
     suppressMessages(tools::startDynamicHelp(start=FALSE))

   # set the help port directly
   env <- environment(tools::startDynamicHelp)
   unlockBinding("httpdPort", env)
   assign("httpdPort", port, envir = env)
   lockBinding("httpdPort", env)
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

.rs.addJsonRpcHandler("show_help_topic", function(topic, package)
{
   if (!is.null(package))
      require(package, character.only = TRUE)
   print(help(topic, help_type="html"))
})

.rs.addJsonRpcHandler("search", function(query)
{
   exactMatch = help(query, help_type="html")
   if (length(exactMatch) == 1)
   {
      print(exactMatch)
      return ()
   }
   else
   {
      paste("help/doc/html/Search?pattern=",
            utils::URLencode(query, reserved = TRUE),
            "&title=1&keyword=1&alias=1",
            sep = "")
   }
})
