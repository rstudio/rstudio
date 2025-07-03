#
# SessionPPM.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.setVar("ppm.vulns", new.env(parent = emptyenv()))

.rs.addFunction("ppm.getVulnerabilityInformation", function(repos = NULL)
{
   # For each available repository, ask it for vulnerability information,
   # then merge that all together.
   repos <- .rs.nullCoalesce(repos, getOption("repos"))
   lapply(repos, function(repoUrl)
   {
      tryCatch(
         .rs.ppm.getVulnerabilityInformationImpl(repoUrl),
         error = function(cnd) list()
      )
   })
})

.rs.addFunction("ppm.getVulnerabilityInformationImpl", function(repoUrl)
{
   # If we have cached information available, use it.
   if (exists(repoUrl, envir = .rs.ppm.vulns))
      return(get(repoUrl, envir = .rs.ppm.vulns))
   
   # Request vulnerabilities
   ppmUrl <- .rs.ppm.parseRepositoryUrl(repoUrl)
   if (length(ppmUrl) == 0L)
      return(list())
   
   fmt <- "%s/__api__/repos/%s/vulns"
   endpoint <- sprintf(fmt, ppmUrl$root, ppmUrl$repos)
   destfile <- tempfile("ppm-vuln-")
   download.file(endpoint, destfile = destfile, quiet = TRUE)
   
   contents <- readLines(destfile, warn = FALSE)
   json <- .rs.fromJSON(contents)
   vulns <- .rs.markScalars(json)
   
   # Cache the result and return.
   assign(repoUrl, vulns, envir = .rs.ppm.vulns)
   vulns
})

.rs.addFunction("ppm.parseRepositoryUrl", function(url)
{
   pattern <- paste0(
      "^",                                  # start of url
      "(?<root>.*?)/",                      # leading URL components
      "(?<repos>[^/]+)/",                   # repository name
      "(?:__linux__/(?<binary>[^/]+)/)?",   # binary url (optional)
      "(?<snapshot>[^/]+)",                 # snapshot
      "$"
   )
   
   m <- regexec(pattern, url, perl = TRUE)
   matches <- regmatches(url, m)[[1L]]
   
   as.list(matches)
})

# Get the active PPM repository, if any.
.rs.addFunction("ppm.getActiveRepository", function()
{
   repos <- getOption("repos")[[1L]]
   parts <- .rs.ppm.parseRepositoryUrl(repos)
   if (length(parts) == 0L)
      return(NULL)
   
   .rs.scalar(parts[[1L]])
})

.rs.addFunction("markScalars", function(object)
{
   if (is.recursive(object))
      for (i in seq_along(object))
         object[[i]] <- .rs.markScalars(object[[i]])
   
   if (is.atomic(object) && length(object) == 1L)
      .rs.scalar(object)
   else
      object
})
