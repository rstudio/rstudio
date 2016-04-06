#
# SessionSource.R
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

.rs.addJsonRpcHandler("save_active_document", function(contents,
                                                       sweave,
                                                       rnwWeave)
{
   # manage working directory
   previousWd = getwd()
   setwd("~/")
   on.exit(setwd(previousWd))

   activeRStudioDoc <- "~/.active-rstudio-document"
   if (file.exists(activeRStudioDoc))
      file.remove(activeRStudioDoc)

   # The contents are always passed as UTF-8 from the client and
   # we want to make sure this is preserved on disk. Note that
   # when the source command is issued by the client for
   # "~/active-rstudio-document" UTF-8 is specified explicitly
   # (see TextEditingTarget.sourceActiveDocument)
   Encoding(contents) <- "UTF-8"
   writeLines(contents, activeRStudioDoc, useBytes = TRUE)
  
   if (sweave)
   {
      op <- function() {
         .Call("rs_rnwTangle", activeRStudioDoc, "UTF-8", rnwWeave)
         file.remove(activeRStudioDoc)
         file.rename(paste(activeRStudioDoc, ".R", sep=""), activeRStudioDoc)
      }
      capture.output(op())
   }

   .Call("rs_ensureFileHidden", activeRStudioDoc)

   return()
})

.rs.addFunction("iconvcommon", function()
{
   # NOTE: we originally included MacRoman and HZ-GB-2312 in our list of
   # common encodings however MacRoman isn't available on Windows or Linux
   # and HZ-GB-2312 isn't available on Linux so we removed them from the
   # common list (in the interest of providing a list of portable encodings
   # so that projects could reliably use common encodings and not run
   # into issues moving from system to system
   #

   common <- c(
      'ASCII', 'UTF-8',
      # Western
      'ISO-8859-1', 'Windows-1252', # 'MacRoman',
      # Japanese
      'Shift-JIS', 'ISO-2022-JP', #'EUC-JP', 'Shift-JISX0213',
      # Trad Chinese
      'Big5', #'Big5-HKSCS',
      # Korean
      'ISO-2022-KR',
      # Arabic
      #'ISO-8859-6', #'Windows-1256',
      # Hebrew
      #'ISO-8859-8', #'Windows-1255',
      # Greek
      'ISO-8859-7', #'Windows-1253',
      # Cyrillic
      #'ISO-8859-5', 'MacCyrillic', 'KOI8-R', 'Windows-1251',
      # Ukranian
      #'KOI8-U',
      # Simplified Chinese
      'GB2312', #'HZ-GB-2312',
      # Chinese
      'GB18030',
      # Central European
      'ISO-8859-2' #'ISO-8859-4', 'MacCentralEurope', 'Windows-1250'
      # Vietnamese
      #'Windows-1258',
      # Turkish
      #'ISO-8859-5', 'Windows-1254',
      # Baltic
      #'Windows-1257'
   )

   toupper(common)
})

.rs.addJsonRpcHandler("iconvlist", function()
{
   list(common=sort(intersect(.rs.iconvcommon(), toupper(iconvlist()))),
        all=sort(iconvlist()))
})

.rs.addGlobalFunction('source.with.encoding',
   function(path, encoding,
         echo=getOption('verbose'),
         print.eval=echo,
         max.deparse.length=150,
         chdir=FALSE)
{
   warning("source.with.encoding is deprecated and will be removed in a ",
           "future release of RStudio. Use source(..., encoding = '", encoding,
           "') instead.")
   conn = file(path, open='r', encoding=encoding)
   on.exit(close(conn))
   source(conn,
          echo=echo,
          print.eval=print.eval,
          max.deparse.length=max.deparse.length,
          chdir=chdir)
})

### Detect free variables ###

# Callback when code walker encounters function call.
# It's mostly looking for variable assignment--if it sees
# a symbol being assigned, it sets the symbol equal to true
# in w$assigned. Otherwise, it recurses.
#
# Functions are handled specially, they redefine w$assigned
# to be their sub-environment, with a parent reference to
# the containing environment.
.rs.addFunction("detectFreeVars_Call", function(e, w)
{
   freeVars <- character(0)

   func <- e[[1]]
   funcName <- as.character(func)
   args <- as.list(e[-1])
   
   if (typeof(func) == 'language')
   {
   	  freeVars <- c(freeVars, codetools:::walkCode(func, w))
   }
   else if (funcName %in% c('<-', '<<-', '=', 'for') 
            && length(args) > 1
            && typeof(args[[1]]) != 'language')
   {
      lvalue <- as.character(args[[1]])

      # Need to walk the right side of an assignment, before
      # considering the lvalue (e.g.: x <- x + 1)
      args <- args[-1]
      if (length(args) > 0)
      {
         for (ee in args)
            freeVars <- c(freeVars, codetools:::walkCode(ee, w))
      }
      args <- c()   # Clear out `args` so they aren't walked later
   
      if (funcName == '<<-')
         assign(lvalue, T, envir=w$assignedGlobals)
      else
	      assign(lvalue, T, envir=w$assigned)
   }
   else if (funcName == '$')
   {
      # In foo$bar, ignore bar
      args <- args[-2]
   }
   else if (funcName == 'function')
   {
      params <- args[[1]]
      w$assigned <- new.env(parent=w$assigned)
      
      for (param in names(params))
      {
         assign(param, T, envir=w$assigned)
         freeVars <- c(freeVars, codetools:::walkCode(params[[param]], w))
      }
      args <- args[-1]
   }
   
   if (length(args) > 0)
   {
      for (ee in args)
         freeVars <- c(freeVars, codetools:::walkCode(ee, w))
   }
   return(unique(freeVars))
})

# Lets us know when we've seen a symbol. If the symbol hasn't
# been assigned yet (i.e. it doesn't exist in w$assigned) then
# we can assume it's a free variable.
.rs.addFunction("detectFreeVars_Leaf", function(e, w)
{
   if (typeof(e) == 'symbol' && nchar(as.character(e)) > 0 && !exists(as.character(e), envir=w$assigned))
      return(as.character(e))
   else
      return(character(0))
})

.rs.addJsonRpcHandler("detect_free_vars", function(code)
{
   globals <- new.env(parent=emptyenv())

   # Ignore predefined symbols like T and F
   assign('T', T, envir=globals)
   assign('F', T, envir=globals)

   w <- codetools:::makeCodeWalker(assigned=globals,
                                   assignedGlobals=globals,
                                   call=.rs.detectFreeVars_Call,
                                   leaf=.rs.detectFreeVars_Leaf)
   freeVars <- character(0)
   for (e in parse(text=code))
     freeVars <- c(freeVars, codetools:::walkCode(e, w))
   return(unique(freeVars))
})


.rs.addFunction("createDefaultShellRd", function(`_name`, `_type`)
{  
  # create a tempdir and switch to it for the duration of the function
  dirName <- tempfile("RdShell")
  dir.create(dirName)
  previousWd <- getwd()
  on.exit(setwd(previousWd))
  setwd(dirName)
  
  if (identical(`_type`, "function"))
  {
    assign(`_name`, function(x) {})
    return (.rs.normalizePath(paste(getwd(),
                                utils::prompt(name = `_name`),
                                sep="/")))
  }
  else if (identical(`_type`, "data"))
  {
    assign(`_name`, data.frame(x=integer(), y=integer()))
    return (.rs.normalizePath(paste(getwd(),
                                utils::promptData(name = `_name`),
                                sep="/")))
  }
  else
  {
    return ("")
  }
})

.rs.addFunction("createShellRd", function(name, type, package) 
{  
  # create a tempdir and switch to it for the duration of the function
  dirName <- tempfile("RdShell")
  dir.create(dirName)
  previousWd <- getwd()
  on.exit(setwd(previousWd))
  setwd(dirName)
  
  if (identical(type, "function"))
  {
    func <- .rs.getPackageFunction(name, package)
    if (!is.null(func))
    {
      funcRd <- utils::prompt(func, name = name)
      return (.rs.normalizePath(paste(getwd(),funcRd,sep="/")))
    }
    else
    {
      return ("")
    }
  }
  else if (identical(type, "data"))
  {
    dataRd <- tryCatch(suppressWarnings({
      library(package,character.only=TRUE)
      eval(parse(text = paste("data(", name, "); ", 
                              "utils::promptData(", name, ");", sep="")),
           envir = globalenv())    
    }), error = function(e) {print(e); ""})
    
    if (nzchar(dataRd) == TRUE)
      return (.rs.normalizePath(paste(getwd(), dataRd, sep="/")))
    else
      return ("")
  }
  else
  {
    return ("")
  }
})

.rs.addFunction("initSource", function()
{
   .rs.registerReplaceHook("file.edit", "utils", function(original, ...)
   {
      # just take unnamed arguments (those are the files)
      args <- c(...)
      names <- names(args)
      if (!is.null(names))
        args <- args[names(args) == ""]

      # call rstudio fileEdit function
      files <- path.expand(args)
      invisible(.Call("rs_fileEdit", files))
   })
})

