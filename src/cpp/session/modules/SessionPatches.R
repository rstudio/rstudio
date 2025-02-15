#
# SessionPatches.R
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

.rs.registerPackageLoadHook("rstudioapi", function(...)
{
   # bail if we're not version 0.7
   if (packageVersion("rstudioapi") != "0.7")
      return()
   
   # re-assign the 'selectDirectory()' function
   rstudioapi <- asNamespace("rstudioapi")
   override <- function(caption = "Select Directory",
                        label = "Select",
                        path = rstudioapi::getActiveProject())
   {
      callFun("selectDirectory", caption, label, path)
   }
   environment(override) <- rstudioapi
   
   .rs.replaceBinding("selectDirectory", "rstudioapi", override)
})

.rs.registerPackageLoadHook("parallel", function(...)
{
   # enforce sequential setup of cluster
   # https://github.com/rstudio/rstudio/issues/6692
   parallel:::setDefaultClusterOptions(setup_strategy = "sequential")
})

# On Windows, because we now set the active code page to UTF-8,
# we need to be careful to ensure the outputs from list.files(), list.dirs()
# and dir() have their encoding properly marked. We do this here.
if (.rs.platform.isWindows)
{
   setHook("rstudio.sessionInit", function(...)
   {
      enabled <- getOption("rstudio.enableFileHooks", default = getRversion() < "4.2.0")
      if (identical(enabled, TRUE))
         .rs.files.replaceBindings()
   })
}

.rs.registerReplaceHook("locator", "graphics", function(original, n = 512, type = "n", ...)
{
   if (length(extras <- list(...))) {
      opar <- par(extras)
      on.exit(par(opar))
   }
   
   n <- as.integer(n)
   if (n <= 0 || is.na(n))
      stop(gettextf("invalid number of points in %s", "locator()"))

   p1 <- graphics::locator(1L, type = type)
   if (type %in% c("p", "o"))
      points(p1[[1L]], p1[[2L]])
   
   if (n == 1L)
      return(p1)
   
   x <- numeric(n); y <- numeric(n)
   x[[1L]] <- p1[[1L]]; x[[2L]] <- p1[[2L]]
   
   range <- seq_len(n - 1L) + 1L
   for (i in range)
   {
      p2 <- graphics::locator(1L, type = type)
      x[[i]] <- p2[[1L]]; y[[i]] <- p2[[2L]]
      
      if (type %in% c("p", "o"))
         points(p2[[1L]], p2[[2L]])
      if (type %in% c("l", "o"))
         segments(p1[[1L]], p1[[2L]], p2[[1L]], p2[[2L]])
      
      p1 <- p2
   }
   
   list(x = x, y = y)
})
