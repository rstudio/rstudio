#
# SessionSpelling.R
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

.rs.addFunction("downloadAllDictionaries", function(targetDir, secure)
{
   # archive we are downloading
   allDicts <- "all-dictionaries.zip"
   
   # remove existing archive if necessary
   allDictsTemp <- paste(tempdir(), allDicts, sep="/")
   if (file.exists(allDictsTemp))
      file.remove(allDictsTemp)
   
   # download the dictionary
   download.file(paste(ifelse(secure, "https", "http"),
                       "://s3.amazonaws.com/rstudio-dictionaries/",
                       allDicts, sep=""),
                 destfile=allDictsTemp,
                 cacheOK = FALSE,
                 quiet = TRUE)
      
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
