#
# Hooks.R
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

assign(".rs.downloadFile", utils::download.file, envir = .rs.toolsEnv())

.rs.defineGlobalHook(
   package = "utils",
   binding = "download.file",
   when    = getRversion() < "4.6.0" && "headers" %in% names(formals(utils::download.file)),
   function(url, destfile, method)
   {
      ""
      "This is an RStudio hook."
      "Use `.rs.downloadFile` to bypass this hook if necessary."
      ""

      # Note that R also supports downloading multiple files in parallel,
      # so the 'url' parameter may be a vector of URLs.
      #
      # Unfortunately, it doesn't support the use of URL-specific headers,
      # so we try to handle this appropriately here.
      if (missing(method))
         method <- getOption("download.file.method", default = "auto")

      # Silence diagnostic warnings.
      headers <- get("headers", envir = environment(), inherits = FALSE)

      # Handle the simpler length-one URL case up front.
      if (length(url) == 1L)
      {
         # Build relevant headers for the call.
         callHeaders <- headers
         authHeader <- .rs.computeAuthorizationHeader(url)
         if (length(authHeader) && nzchar(authHeader))
            callHeaders <- c(callHeaders, Authorization = authHeader)

         # Build a call to invoke the base R downloader.
         call <- match.call(expand.dots = TRUE)
         call[[1L]] <- quote(.rs.downloadFile)
         if (length(callHeaders))
            call["headers"] <- list(callHeaders)
         status <- eval(call, envir = parent.frame())
         return(invisible(status))
      }

      # Otherwise, do some more work to map headers to URLs as appropriate.
      retvals <- vector("integer", length = length(url))
      authHeaders <- .rs.mapChr(url, .rs.computeAuthorizationHeader)
      for (authHeader in unique(authHeaders))
      {
         # Figure out which URLs are associated with the current header.
         idx <- which(authHeaders == authHeader)

         # Build relevant headers for the call.
         callHeaders <- headers
         if (length(authHeader) && nzchar(authHeader))
            callHeaders <- c(callHeaders, Authorization = authHeader)

         # Build a call to download these files all in one go.
         call <- match.call(expand.dots = TRUE)
         call[[1L]] <- quote(.rs.downloadFile)
         call["url"] <- list(url[idx])
         call["destfile"] <- list(destfile[idx])
         if (length(callHeaders))
            call["headers"] <- list(callHeaders)
         retvals[idx] <- eval(call, envir = parent.frame())
      }

      # Note that even if multiple files are downloaded, R only reports
      # a single status code, with 0 implying that all downloads succeeded.
      status <- if (all(retvals == 0L)) 0L else 1L
      if (getRversion() >= "4.5.0")
         attr(status, "retvals") <- retvals
      invisible(status)

   })
