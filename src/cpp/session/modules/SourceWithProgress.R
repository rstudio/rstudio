#
# SourceWithProgress.R
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

emitProgress <- function(kind, arg, con) {
   if (!is.null(con)) {
      cat(paste0("__rs_progress_0__ ", kind, " __ ", arg, 
                 " __rs_progress_1__\n"), file = con)
   }
}

sourceWithProgress <- function(script,               # path to R script
                               encoding = "unknown", # character encoding of script
                               con = NULL,           # connection to write progress
                               importRdata = NULL,   # RData file to import on start 
                               exportRdata = NULL    # RData file to export when done
                               ) {
   # create a new enviroment to host any values created; make its parent the global env so any
   # variables inside this function's environment aren't visible to the script
   sourceEnv <- new.env(parent = globalenv())

   # parse the script
   statements <- parse(file = script, keep.source = TRUE, encoding = encoding)
   units <- length(statements)
   unit <- 0

   # add extra steps for importing/exporting
   if (!is.null(importRdata))
      units <- units + 1
   if (!is.null(exportRdata))
      units <- units + 1

   # notify host process of 
   emitProgress("count", units, con)

   if (!is.null(importRdata)) {
      # indicate that we're importing the requested env
      emitProgress("section", "Importing environment", con)
      load(importRdata, envir = globalenv())

      # clear progress text and advance marker
      unit <- unit + 1
      emitProgress("section", "", con)
      emitProgress("statement", unit, con)
   }

   # find the sections
   lines <- readLines(con = script, encoding = encoding, warn = FALSE)
   sections <- regmatches(lines, regexec("^\\s*#+ (.+) -----*$", lines))
   sectLines <- which(sapply(sections, length) > 0)

   # evaluate each statement
   for (idx in seq_along(statements)) {
      # check to see if this is the first statement in a section
      ref <- attr(statements[idx], "srcref", exact = TRUE)
      if (!is.null(ref)) {
         line <- ref[[1]][[1]]

         # look for sections that begin above this line
         candidates <- which(sectLines <= line)

         if (length(candidates) > 0) {
            # find line on which section starts
            line <- sectLines[[length(candidates)]]

            # emit indicator that we've started this section
            emitProgress("section", sections[[line]][[2]], con)

            # remove this from the set of sections so we don't emit again
            sectLines <- sectLines[sectLines != line]
         }
      }
      
      # evaluate the statement
      eval(statements[[idx]], envir = sourceEnv)

      # update progress; statement is complete
      unit <- unit + 1
      emitProgress("statement", unit, con)
   }

   if (!is.null(exportRdata))
   {
      # export any created values
      emitProgress("section", "Exporting environment", con)
      save(list = ls(envir = sourceEnv, all.names = TRUE), 
           file = exportRdata, 
           envir = sourceEnv)

      # update progress
      unit <- unit + 1
      emitProgress("statement", unit, con)
   }

   emitProgress("completed", 1, con)
}
