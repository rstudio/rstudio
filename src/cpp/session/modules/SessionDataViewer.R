#
# SessionData.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

# host environment for data 
.rs.setVar("CachedDataEnv", new.env(parent = emptyenv()))

.rs.addFunction("formatDataColumn", function(x, start, len, ...)
{
   # extract the visible part of the column
   col <- x[start:min(length(x), start+len)]

   if (is.numeric(col)) {
     # show numbers as doubles
     storage.mode(col) <- "double"
   } else {
     # show everything else as characters
     col <- as.character(col)
   }
   format(col, trim = TRUE, justify = "none", ...)
})

.rs.addFunction("formatRowNames", function(x, start, len) 
{
  rownames <- row.names(x)
  rownames[start:min(length(rownames), start+len)]
})

.rs.addFunction("toDataFrame", function(x, name) {
  if (is.data.frame(x))
    return(x)
  frame <- as.data.frame(x)
  names(frame)[names(frame) == "x"] <- name
  frame
})

.rs.addFunction("applySort", function(x, col, dir) 
{
  if (identical(dir, "desc")) {
    as.data.frame(x[order(-x[,col]),])
  } else {
    as.data.frame(x[order(x[,col]),])
  }
})


.rs.addFunction("findDataFrame", function(envName, objName, cacheKey, cacheDir) 
{
  env <- NULL

  # do we have an object name? if so, check in a named environment
  if (!is.null(objName) && nchar(objName) > 0) 
  {
    if (is.null(envName) || identical(envName, "R_GlobalEnv") || 
        nchar(envName) == 0)
    {
      # global environment
      env <- globalenv()
    }
    else 
    {
      # some other environment
      tryCatch(
      {
        env <- as.environment(envName)
      }, 
      error = function(e)
      {
        # if we couldn't find the environment any more, use the empty one
        env <<- emptyenv()
      })
    }

    # if the object exists in this environment, return it (avoid creating a
    # temporary here)
    if (exists(objName, where = env, inherits = FALSE))
      return(.rs.toDataFrame(get(objName, envir = env, inherits = FALSE), objName))
  }

  # if the object exists in the cache environment, return it
  if (exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
    return(.rs.toDataFrame(get(cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE), objName))

  # perhaps the object has been saved? attempt to load it into the
  # cached environment
  cacheFile <- file.path(cacheDir, paste(cacheKey, "Rdata", sep = "."))
  if (file.exists(cacheFile))
  { 
    load(cacheFile, envir = .rs.CachedDataEnv)
    if (exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
      return(.rs.toDataFrame(get(cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE), objName))
  }
  
  # failure
  return(NULL)
})

.rs.addFunction("findOwningEnv", function(name, env = parent.frame()) 
{
   while (environmentName(env) != "R_EmptyEnv" && 
          !exists(name, where = env, inherits = FALSE)) 
   {
     env <- parent.env(env)
   }
   env
})


.rs.registerReplaceHook("View", "utils", function(original, x, title) 
{
   # generate title if necessary
   if (missing(title))
      title <- deparse(substitute(x))[1]

   name <- ""
   env <- emptyenv()

   # if the argument is the name of a variable, we can monitor it in its
   # environment, and don't need to make a copy for viewing
   if (is.name(substitute(x)))
   {
     name <- deparse(substitute(x))
     env <- .rs.findOwningEnv(name)
   }

   # save a copy into the cached environment
   cached = paste(sample(c(letters, 0:9), 10, replace = TRUE), collapse = "")
   assign(cached, force(x), .rs.CachedDataEnv)
   
   # call viewData 
   invisible(.Call("rs_viewData", x, title, name, env, cached))
})

.rs.addFunction("initializeDataViewer", function(server) {
    if (server) {
        .rs.registerReplaceHook("edit", "utils", function(original, name, ...) {
            if (is.data.frame(name) || is.matrix(name))
                stop("Editing of data frames and matrixes is not supported in RStudio.", call. = FALSE)
            else
                original(name, ...)
        })
    }
})


.rs.addFunction("removeCachedData", function(cacheKey, cacheDir)
{
  # remove data from the cache environment
  if (exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
    rm(list = cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE)

  # remove data from the cache directory
  cacheFile <- file.path(cacheDir, paste(cacheKey, "Rdata", sep = "."))
  if (file.exists(cacheFile))
    file.remove(cacheFile)
 
  invisible(NULL)
})

.rs.addFunction("saveCachedData", function(cacheDir)
{
  # create the cache directory if it doesn't already exist
  dir.create(cacheDir, recursive = TRUE, showWarnings = FALSE, mode = "0700")

  # save each active cache file from the cache environment
  lapply(ls(.rs.CachedDataEnv), function(cacheKey) {
    save(list = cacheKey, 
         file = file.path(cacheDir, paste(cacheKey, "Rdata", sep = ".")),
         envir = .rs.CachedDataEnv)
  })

  invisible(NULL)
})

