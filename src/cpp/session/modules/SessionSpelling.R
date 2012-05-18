#
# SessionSpelling.R
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

.rs.addFunction("downloadAllDictionaries", function(targetDir)
{
   # archive we are downloading
   allDicts <- "all-dictionaries.zip"
   
   # remove existing archive if necessary
   allDictsTemp <- paste(tempdir(), allDicts, sep="/")
   if (file.exists(allDictsTemp))
      file.remove(allDictsTemp)
   
   # download function
   download <- function(protocol, method) {
      download.file(paste(protocol,
                          "://s3.amazonaws.com/rstudio-dictionaries/",
                          allDicts, sep=""),
                    destfile=allDictsTemp,
                    method=method,
                    cacheOK = FALSE,
                    quiet = TRUE)
   }
   
   # system-specific download methods (to try to get https)
   switch(Sys.info()[['sysname']],
          Windows = { method <- "internal"},
          Linux   = { method <- "wget"},
          Darwin  = { method <- "curl"})
   
   # try downloading using https, fallback to http if it fails for any reason
   tryCatch(download("https", method), 
            error = function(e) download("http", "internal"))
   
   # define function to remove the existing dictionaries then call it
   removeExisting <- function() {
      suppressWarnings({
         file.remove(paste(targetDir, list.files(targetDir), sep="/"))
         unlink(targetDir)
      })
   }
   removeExisting()
   
   # unzip downloaded dictionaires into target -- if this fails for any
   # reason then remove any files that were unpacked (because we don't 
   # know if the partially unzipped archive is valid)
   tryCatch(unzip(allDictsTemp, exdir=targetDir),
            error = function(e) { removeExisting(); stop(e); })
   
   # remove the archive
   file.remove(allDictsTemp)   
})
