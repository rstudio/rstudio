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

# host environment for cached data; this allows us to continue to view data 
# even if the original object is deleted
.rs.setVar("CachedDataEnv", new.env(parent = emptyenv()))

# host environment for working data; this allows us to sort/filter/page the
# data without recomputing on the original object every time
.rs.setVar("WorkingDataEnv", new.env(parent = emptyenv()))

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

.rs.addFunction("describeCols", function(x, maxCols, maxFactors) 
{
  colNames <- names(x)

  # truncate to maximum displayed number of columns
  colNames <- colNames[1:min(length(colNames), maxCols)]

  # get the attributes for each column
  colAttrs <- lapply(seq_along(colNames), function(idx) {
    col_name <- if (idx <= length(colNames)) 
                  colNames[idx] 
                else 
                  as.character(idx)
    col_type <- "unknown"
    col_min <- 0
    col_max <- 0
    col_vals <- ""
    col_search_type <- ""
    if (length(x[,idx]) > 0)
    {
      val <- x[,idx][1]
      if (is.factor(val))
      {
        col_type <- "factor"
        if (length(levels(val)) > maxFactors)
        {
          # if the number of factors exceeds the max, search the column as 
          # though it were a character column
          col_search_type <- "character"
        }
        else 
        {
          col_search_type <- "factor"
          col_vals <- levels(val)
        }
      }
      else if (is.numeric(val))
      {
        # ignore missing values when computing min/max
        col_type <- "numeric"
        col_search_type <- "numeric"
        col_min <- min(x[,idx], na.rm = TRUE)
        col_max <- max(x[,idx], na.rm = TRUE)
      }
      else if (is.character(val))
      {
        col_type <- "character"
        col_search_type <- "character"
      }
    }
    list(
      col_name        = .rs.scalar(col_name),
      col_type        = .rs.scalar(col_type),
      col_min         = .rs.scalar(col_min),
      col_max         = .rs.scalar(col_max),
      col_search_type = .rs.scalar(col_search_type),
      col_vals        = col_vals
    )
  })
  c(list(list(
      col_name        = .rs.scalar(""),
      col_type        = .rs.scalar("rownames"),
      col_min         = .rs.scalar(0),
      col_max         = .rs.scalar(0),
      col_search_type = .rs.scalar("none"),
      col_vals        = ""
    )), colAttrs)
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

.rs.addFunction("applyTransform", function(x, filtered, search, col, dir) 
{
  # apply columnwise filters
  for (i in seq_along(filtered)) {
    if (nchar(filtered[i]) > 0 && length(x[,i]) > 0) {
      # split filter--string format is "type|value" (e.g. "numeric|12-25") 
      filter <- strsplit(filtered[i], split="|", fixed = TRUE)[[1]]
      if (length(filter) < 2) 
      {
        # no filter type information
        next
      }
      filtertype <- filter[1]
      filterval <- filter[2]

      # apply filter appropriate to type
      if (identical(filtertype, "factor")) 
      {
        # apply factor filter: convert to numeric values 
        filterval <- as.numeric(filterval)
        x <- x[as.numeric(x[,i]) == filterval,]
      }
      else if (identical(filtertype, "character"))
      {
        # apply character filter: non-case-sensitive prefix
        x <- x[grepl(filterval, x[,i], ignore.case = TRUE),]
      } 
      else if (identical(filtertype, "numeric"))
      {
        # apply numeric filter, range ("2-32") or equality ("15")
        filterval <- as.numeric(strsplit(filterval, "-")[[1]])
        if (length(filterval) > 1)
          # range filter
          x <- x[x[,i] >= filterval[1] & x[,i] <= filterval[2],]
        else
          # equality filter
          x <- x[x[,i] == filterval,]
      }
    }
  }

  # apply global search
  if (!is.null(search) && nchar(search) > 0)
  {
    x <- x[Reduce("|", lapply(x, function(column) { 
             grepl(search, column, ignore.case = TRUE)
           })),]
  }

  # apply sort
  if (col > 0 && length(x[,col]) > 0)
  {
    x <- as.data.frame(x[order(x[,col], decreasing = identical(dir, "desc")),])
  }

  return(x)
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

# given a name, return the first environment on the search list that contains
# an object bearing that name
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
   cacheKey <- .rs.addCachedData(force(x))
   
   # call viewData 
   invisible(.Call("rs_viewData", x, title, name, env, cacheKey))
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

.rs.addFunction("addCachedData", function(obj) 
{
   cacheKey <- paste(sample(c(letters, 0:9), 10, replace = TRUE), collapse = "")
   .rs.assignCachedData(cacheKey, obj)
   cacheKey
})

.rs.addFunction("assignCachedData", function(cacheKey, obj) 
{
   assign(cacheKey, obj, .rs.CachedDataEnv)
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

  # remove any working data
  .rs.removeWorkingData(cacheKey)
 
  invisible(NULL)
})

.rs.addFunction("saveCachedData", function(cacheDir)
{
  # no work to do if we have no cache
  if (!exists(".rs.CachedDataEnv")) 
    return(invisible(NULL))

  # create the cache directory if it doesn't already exist
  dir.create(cacheDir, recursive = TRUE, showWarnings = FALSE, mode = "0700")

  # save each active cache file from the cache environment
  lapply(ls(.rs.CachedDataEnv), function(cacheKey) {
    save(list = cacheKey, 
         file = file.path(cacheDir, paste(cacheKey, "Rdata", sep = ".")),
         envir = .rs.CachedDataEnv)
  })

  # clean the cache environment
  rm(list = ls(.rs.CachedDataEnv), where = .rs.CachedDataEnv)

  invisible(NULL)
})

.rs.addFunction("findWorkingData", function(cacheKey)
{
  if (exists(cacheKey, where = .rs.WorkingDataEnv, inherits = FALSE))
    get(cacheKey, envir = .rs.WorkingDataEnv, inherits = FALSE)
  else
    NULL
})

.rs.addFunction("removeWorkingData", function(cacheKey)
{
  if (exists(cacheKey, where = .rs.WorkingDataEnv, inherits = FALSE))
    rm(list = cacheKey, envir = .rs.WorkingDataEnv, inherits = FALSE)
  invisible(NULL)
})

.rs.addFunction("assignWorkingData", function(cacheKey, obj)
{
  assign(cacheKey, obj, .rs.WorkingDataEnv)
})

