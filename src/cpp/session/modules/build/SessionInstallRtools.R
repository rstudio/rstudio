#
# SessionInstallRTools.R
#
# Copyright (C) 2022 by Posit, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("findRtools42Installer", function(url, fallbackUrl)
{
   tryCatch({
       tmp <- tempfile()
       utils::download.file(url, tmp, mode = "w", quiet = TRUE)
       homePageHtml <- paste(readLines(tmp), collapse = " ")
       unlink(tmp)
     },
     error = function(e) {
       return(fallbackUrl)
     })
     reLinkPattern <- ".*<a\\shref=\"(.*rtools.*\\.exe)\">\\s*.+<\\/a>.*"
     if (grepl(reLinkPattern, homePageHtml)) {
       urlRoot <- dirname(url)
       # extract the relative url to the installer exe file
       installerUrl <- gsub(reLinkPattern, "\\1", homePageHtml)
       file.path(urlRoot, installerUrl)
     }
     else
       fallbackUrl
})
