#
# SessionGraphics.R
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

.rs.addFunction("graphics.supportedBackends", function()
{
   c(
      if (Sys.info()[["sysname"]] == "Windows")
         "windows",
      
      if (.rs.hasCapability("aqua"))
         "quartz",
      
      if (.rs.hasCapability("cairo"))
         c("cairo", "cairo-png"),
      
      "ragg"
   )

})

#' TIFF compression types
#'
#' The compressions offered when a plot is saved as a TIFF, in the order they
#' are listed: the lossless ones named by the 'compression' argument of
#' grDevices::tiff(), which ragg's agg_tiff() shares, as far as the running
#' R offers them.
.rs.addFunction("graphics.tiffCompressionTypes", function()
{
   # 'rle' (CCITT Group 3) fails for colour images, 'jpeg' is lossy at a fixed
   # quality, and the recent additions (lerc, lzma, zstd, webp) are liable to
   # be unsupported by libtiff and by viewers, and are unknown to ragg
   types <- c("none", "lzw", "zip", "lzw+p", "zip+p")
   offered <- tryCatch(
      eval(formals(grDevices::tiff)[["compression"]]),
      error = function(e) "none"
   )
   intersect(types, offered)
})

.rs.addFunction("graphics.locator", function(n = 512, type = "n", ...)
{
   if (length(extras <- list(...))) {
      opar <- par(extras)
      on.exit(par(opar))
   }
   
   n <- as.integer(n)
   if (is.na(n) || n <= 0)
      stop(gettextf("invalid number of points in %s", "locator()"))
   
   p1 <- .External2(C_locator, 1L, type = type)
   if (p1[[3L]] == 0L)
      return(invisible(NULL))
   
   if (type %in% c("p", "o"))
      points(p1[[1L]], p1[[2L]])
   
   x <- numeric(n); y <- numeric(n)
   x[[1L]] <- p1[[1L]]; y[[1L]] <- p1[[2L]]
   
   range <- seq_len(n - 1L) + 1L
   for (i in range)
   {
      p2 <- .External2(C_locator, 1L, type = type)
      if (p2[[3L]] == 0L) {
         length(x) <- length(y) <- i - 1L
         break
      }
      
      x[[i]] <- p2[[1L]]; y[[i]] <- p2[[2L]]
      
      if (type %in% c("p", "o"))
         points(p2[[1L]], p2[[2L]])
      if (type %in% c("l", "o"))
         segments(p1[[1L]], p1[[2L]], p2[[1L]], p2[[2L]])
      
      p1 <- p2
   }
   
   list(x = x, y = y)
})

.rs.replaceBinding(
   binding  = "locator",
   package  = "graphics",
   override = .rs.graphics.locator
)
