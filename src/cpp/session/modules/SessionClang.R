#
# SessionClang.R
#
# Copyright (C) 2022 by RStudio, PBC
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
   cat("Attempting to load libclang for", R.version$platform, "\n")
   .Call("rs_isLibClangAvailable", PACKAGE = "(embedding)")
})

.rs.addFunction("setClangDiagnostics", function(level)
{
   if (!is.numeric(level) || (level < 0) || (level > 3))
      stop("level must be within the range [0, 3]")
   
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

# this function can be useful when updating an Rtools definition,
# when you need to determine the default compiler include paths
.rs.addFunction("libclang.defaultCompilerIncludeDirectories", function(compiler = NULL,
                                                                       isCpp = TRUE)
{
   # put rtools on PATH for windows
   if (.rs.platform.isWindows)
   {
      path <- Sys.getenv("PATH")
      on.exit(Sys.setenv(PATH = path), add = TRUE)
      .rs.addRToolsToPath()
   }
   
   if (is.null(compiler))
   {
      # if compiler is not set, then use the default C++ compiler
      exe <- if (.rs.platform.isWindows) "R.exe" else "R"
      R <- file.path(R.home("bin"), exe)
      compiler <- if (isCpp) "CXX" else "CC"
      cxx <- system2(R, c("CMD", "config", compiler), stdout = TRUE, stderr = TRUE)
      
      # take only last line, in case R or the compiler spat out other output
      compiler <- tail(cxx, n = 1L)
   }
   
   # create a dummy c++ file
   file <- tempfile(fileext = if (isCpp) ".cpp" else ".c")
   writeLines("void test() {}", con = file)
   
   # build a command for printing compiler include paths
   command <- paste(compiler, "-E -v", basename(file))
   
   # run it
   output <- local({
      owd <- setwd(tempdir())
      on.exit(setwd(owd), add = TRUE)
      suppressWarnings(system(command, intern = TRUE))
   })
   
   # find the lines of interest
   start <- grep("#include <...> search starts here:", output)
   end <- grep("End of search list.", output)
   if (length(start) == 0L || length(end) == 0L)
   {
      .rs.logWarningMessage("couldn't determine compiler search list")
      return(character())
   }
   
   lines <- output[(start + 1L):(end - 1L)]
   
   # trim and normalize paths
   paths <- .rs.trimWhitespace(lines)
   normalizePath(paths, winslash = "/", mustWork = FALSE)
})

.rs.addFunction("libclang.generateCompilerDefinitions", function(path, isCpp = TRUE)
{
   # put rtools on PATH for windows
   if (.rs.platform.isWindows)
   {
      envpath <- Sys.getenv("PATH")
      on.exit(Sys.setenv(PATH = envpath), add = TRUE)
      .rs.addRToolsToPath()
   }

   # use the default compiler configured by R   
   exe <- if (.rs.platform.isWindows) "R.exe" else "R"
   R <- file.path(R.home("bin"), exe)
   compiler <- if (isCpp) "CXX" else "CC"
   cxx <- system2(R, c("CMD", "config", compiler), stdout = TRUE, stderr = TRUE)
   
   # take only last line, in case R or the compiler spat out other output
   compiler <- tail(cxx, n = 1L)
   
   # create a dummy c++ file
   file <- tempfile(fileext = if (isCpp) ".cpp" else ".c")
   writeLines("void test() {}", con = file)
   
   # build a command for printing compiler definitions
   command <- paste(compiler, "-dM -E", basename(file))
   
   # run it
   output <- local({
      owd <- setwd(tempdir())
      on.exit(setwd(owd), add = TRUE)
      suppressWarnings(system(command, intern = TRUE))
   })
   
   # for each line, only define if it's not already defined
   formatted <- unlist(lapply(output, function(line) {
      parts <- strsplit(line, "\\s+", perl = TRUE)[[1L]]
      fmt <- "#ifndef %s\n%s\n#endif\n"
      msg <- sprintf(fmt, gsub("\\(.*", "", parts[[2L]]), line)
      gsub("#define", "# define", msg)
   }))
   
   # libclang doesn't seem to support __float128 with a Windows target,
   # even though gcc does -- either way, remove this define so that we
   # don't get (hopefully spurious) libclang warnings
   if (.rs.platform.isWindows && isCpp)
   {
      formatted <- c(
         "#include <bits/c++config.h>",
         "",
         formatted,
         "",
         "#undef _GLIBCXX_USE_FLOAT128"
      )
   }
   
   # dump it to file
   dir.create(dirname(path), showWarnings = FALSE, recursive = TRUE)
   writeLines(formatted, con = path)
   
   # return path
   normalizePath(path)
   
})
