#
# SessionSpelling.R
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

.rs.addFunction("downloadAllDictionaries", function(targetDir, secure)
{
   # form url to dictionaries
   fmt <- "%s://s3.amazonaws.com/rstudio-buildtools/dictionaries/%s"
   protocol <- if (secure) "https" else "http"
   archive <- "all-dictionaries.zip"
   url <- sprintf(fmt, protocol, archive)
   
   # form path to downloaded dictionaries in tempdir
   archivePath <- file.path(tempdir(), archive)
   if (file.exists(archivePath))
      file.remove(archivePath)
   
   # download the dictionary
   download.file(
      url = url,
      destfile = archivePath,
      cacheOK = FALSE,
      quiet = TRUE
   )
   
   # remove existing dictionaries if they exist
   unlink(targetDir, recursive = TRUE)
   dir.create(targetDir, showWarnings = FALSE, recursive = TRUE)
   
   # unzip downloaded dictionaries into target -- if this fails for any
   # reason then remove any files that were unpacked (because we don't 
   # know if the partially unzipped archive is valid)
   tryCatch(
      unzip(archivePath, exdir = targetDir),
      error = function(e) {
         unlink(targetDir, recursive = TRUE)
         stop(e)
      }
   )
   
   # remove the downloaded archive
   unlink(archivePath)
   
   invisible(targetDir)
})
