#
# SessionInstallRTools.R
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

.rs.addFunction("findRtoolsInstaller", function(version, url)
{
   tryCatch(
      .rs.findRtoolsInstallerImpl(url),
      error = function(cnd) {
         .rs.findRtoolsInstallerFallback(version)
      }
   )
})

.rs.addFunction("findRtoolsInstallerImpl", function(url)
{
   # download contents of home page
   destfile <- tempfile("rtools-home-", fileext = ".html")
   download.file(url, destfile, mode = "wb", quiet = TRUE)
   contents <- paste(readLines(destfile), collapse = "\n")
   
   # search for links included in the page
   reLinkPattern <- 'href="([^"]+)"'
   matches <- .rs.regexMatches(reLinkPattern, contents)
   
   # keep those which appear to be rtools links
   links <- grep("[.]exe$", matches, value = TRUE, perl = TRUE)
   links <- grep("rtools", links, value = TRUE, perl = TRUE)
   
   # TODO: support aarch64 builds of R on Windows
   links <- grep("aarch64", links, perl = TRUE, value = TRUE, invert = TRUE)
   if (length(links) == 0L)
      stop("couldn't determine Rtools installer URL")
   
   rtoolsInstallerUrl <- links[[1]]
   file.path(dirname(url), rtoolsInstallerUrl)
})

.rs.addFunction("findRtoolsInstallerFallback", function(version)
{
   if (version == "4.4")
   {
      "https://rstudio.org/links/rtools44"
   }
   else if (version == "4.3")
   {
      "https://rstudio.org/links/rtools43"
   }
   else
   {
      stop("don't know how to download and install Rtools ", version)
   }
})
