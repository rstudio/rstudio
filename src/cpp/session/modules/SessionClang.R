#
# SessionClang.R
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



.rs.addFunction("clangPCHPath", function(pkg, clangVersion)
{
   paste(
      packageVersion(pkg),
      R.version$platform,
      R.version$`svn rev`,
      clangVersion,
      sep = "-"
   )
})

.rs.addFunction("isClangAvailable", function() {
   cat("Attemping to load libclang for", R.version$platform, "\n")
   .Call("rs_isLibClangAvailable", PACKAGE = "(embedding)")
})

.rs.addFunction("setClangDiagnostics", function(level)
{
   if (!is.numeric(level) || (level < 0) || (level > 2))
      stop("level must be 0, 1, or 2")
   
   if (level > 0)
      .rs.isClangAvailable()
   
   .Call("rs_setClangDiagnostics", level, PACKAGE = "(embedding)")
   
   .rs.restartR()
   invisible(NULL)
})

.rs.addFunction("packagePCH", function(linkingTo)
{
   linkingTo <- .rs.parseLinkingTo(linkingTo)
   packages <- c("RcppArmadillo", "RcppEigen", "Rcpp11", "Rcpp")
   for (package in packages)
      if (package %in% linkingTo)
         return(package)
   ""
})

.rs.addFunction("includesForLinkingTo", function(linkingTo)
{
   includes <- character()
   
   linkingTo <- .rs.parseLinkingTo(linkingTo)
   for (pkg in linkingTo) {
      includeDir <- system.file("include", package = pkg)
      if (file.exists(includeDir)) {
         includes <- c(
            includes,
            paste("-I", .rs.asBuildPath(includeDir), sep = "")
         )
      }
   }
   
   includes
})

.rs.addFunction("asBuildPath", function(path)
{
   if (.Platform$OS.type == "windows") {
      path <- normalizePath(path)
      if (grepl(' ', path, fixed = TRUE))
         path <- utils::shortPathName(path)
      path <- gsub("\\\\", "/", path)
   }
   
   return(path)
})




