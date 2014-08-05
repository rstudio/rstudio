#
# SessionClang.R
#
# Copyright (C) 2014 by RStudio, Inc.
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

.rs.addJsonRpcHandler("get_clang_completions", function(input, row, column) {
   .rs.getClangCompletions(input = input,
                           row = row,
                           column = column,
                           mode = "c++")
})

.rs.addFunction("getClangCompletions", function(input, row, column, mode) {

   ## TODO: 0-based indexing vs. 1-based indexing; handle earlier?

   cat("Hi from Clang auto completion!")
   input <- unlist(strsplit(input, "\n", fixed = TRUE))
   cat(input, sep = "\n")
   clang <- "/usr/local/bin/clang" ## for now

   ## Mode should be either 'c' or 'c++' -- we assume 'c++' if otherwise
   if (mode != "c") {
      mode <- "c++"
   }

   ## TODO: populate includes based on e.g. DESCRIPTION, // [[Rcpp::depends]], and so on

   fmt <- "-cc1 -x %s -I/usr/include -I/usr/local/include -fsyntax-only -code-completion-at -:%s:%s"
   cmd <- sprintf(fmt, mode, as.integer(row + 1), as.integer(column + 1))
   cat(cmd)
   output <- suppressWarnings(
      system2(clang, cmd, input = input, stdout = TRUE, stderr = FALSE)
   )

   ## Post-process auto-completion output
   output <- gsub("COMPLETION: ", "", output, fixed = TRUE)
   splat <- strsplit(output, " : ", fixed = TRUE)

   names <- sapply(splat, `[[`, 1)
   compl <- sapply(splat, `[[`, 2)

   ## Infer the class name we're completing on by the returned destructor
   class <- gsub("::", "", grep("::$", compl, value = TRUE, perl = TRUE), fixed = TRUE)

   ## Ignore constructors if we're completing on a '.'
   if (substring(input[row + 1], column, column) == ".") {
      remove <- Reduce(union, list(
         which(grepl("~", compl, fixed = TRUE)),
         which(grepl("::", compl, fixed = TRUE)),
         which(grepl("operator", compl, fixed = TRUE))
      ))
      names <- names[-c(remove)]
      compl <- compl[-c(remove)]
   }

   ## TODO: Parse returned signatures
   print(names)
   print(compl)
   if (length(names)) names <- sort(names)
   list(results = sort(names))

})
