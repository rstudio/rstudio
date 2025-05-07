#
# SessionSource.R
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
      # Ukrainian
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

.rs.addFunction("detectFreeVarsExpr", function(node)
{
   .rs.detectFreeVars(substitute(node))
})

.rs.addFunction("detectFreeVars", function(node, globals = new.env(parent = baseenv()))
{
   vars <- .rs.stack(mode = "character")
   
   .rs.recursiveWalkIf(node, function(node)
   {
      # check and analyze call objects
      if (is.call(node))
      {
         # skip functions which are likely to perform non-standard evaluation
         if (is.symbol(node[[1L]]))
         {
            lhs <- as.character(node[[1L]])
            if (lhs %in% .rs.nse.primitives)
               return(FALSE)
         }
         
         # recurse into for loops with variable binding active
         isFor <-
            length(node) == 4L &&
            identical(node[[1L]], as.symbol("for"))
         
         if (isFor)
         {
            seqGlobals <- new.env(parent = globals)
            seqVars <- .rs.detectFreeVars(node[[3L]], seqGlobals)
            for (seqVar in seqVars)
               vars$push(seqVar)
            
            forGlobals <- new.env(parent = globals)
            forGlobals[[as.character(node[[2L]])]] <- TRUE
            
            forVars <- .rs.detectFreeVars(node[[4L]], forGlobals)
            for (forVar in forVars)
               vars$push(forVar)
            
            return(FALSE)
         }
      
         # recurse into function definitions; add bindings for
         # the function parameters
         isFunction <-
            identical(node[[1L]], as.symbol("function")) &&
            length(node) >= 3L
         
         if (isFunction)
         {
            functionGlobals <- new.env(parent = globals)
            for (functionParam in names(node[[2L]]))
               functionGlobals[[functionParam]] <- TRUE
            
            functionVars <- .rs.detectFreeVars(node[[3L]], functionGlobals)
            for (functionVar in functionVars)
               vars$push(functionVar)
            
            return(FALSE)
         }
      
         # don't descend into `::` or `:::` qualified lookups
         isNamespace <-
            is.symbol(node[[1L]]) &&
            as.character(node[[1L]]) %in% c("::", ":::")
         
         if (isNamespace)
         {
            return(FALSE)
         }
         
         # for `$` and `@`, only look at left hand side
         isMemberAccess <-
            is.symbol(node[[1L]]) &&
            as.character(node[[1L]]) %in% c("$", "@")
         
         if (isMemberAccess)
         {
            lhs <- node[[2L]]
            if (is.symbol(lhs))
               vars$push(as.character(lhs))
            
            return(FALSE)
         }
         
         # check for assignments
         isAssignment <-
            length(node) == 3L &&
            is.symbol(node[[1L]]) &&
            as.character(node[[1L]]) %in% c("=", "<-", "<<-")
         
         if (isAssignment)
         {
            assignee <- node[[2L]]
            if (is.symbol(assignee))
               assign(as.character(assignee), assignee, envir = globals)
         }
      }
      
      # if we encounter a symbol, it's a free var
      else if (is.symbol(node))
      {
         value <- as.character(node)
         if (nzchar(value) && !exists(value, envir = globals))
            vars$push(value)
         
         return(FALSE)
      }
      
      # try to recurse other objects by default
      TRUE
      
   })
   
   unique(vars$data())
   
})

.rs.addJsonRpcHandler("detect_free_vars", function(code)
{
   node <- parse(text = code, keep.source = FALSE)[[1L]]
   .rs.detectFreeVars(node)
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
      invisible(.Call("rs_fileEdit", args, PACKAGE = "(embedding)"))
   })
})

.rs.addFunction("getSourceDocument", function(id, includeContents = FALSE)
{
   .Call(
      "rs_getSourceDocument",
      as.character(id),
      as.logical(includeContents),
      PACKAGE = "(embedding)"
   )
})

.rs.addFunction("getSourceDocumentProperties", function(path, includeContents = FALSE)
{
   if (is.null(path) || !file.exists(path))
      return(NULL)

   path <- normalizePath(path, winslash = "/", mustWork = TRUE)
   .Call("rs_getDocumentProperties", path, includeContents)
})

.rs.addFunction("generateStylerFormatDocumentScript", function(documentPath,
                                                               scriptPath)
{
   # only invoke 'styler' on supported file types
   ext <- tools::file_ext(documentPath)
   if (!tolower(ext) %in% c("r", "rmd", "rmarkdown", "qmd", "rnw"))
      return()

   # figure out where 'styler' is installed
   stylerPath <- find.package("styler")
   libraryPaths <- .libPaths(c(dirname(stylerPath), .libPaths()))

   # create a tidyverse styler, using the current indentation settings
   indent <- .rs.readUserPref("num_spaces_for_tab", 2L)
   strict <- .rs.readUserPref("code_formatter_styler_strict", TRUE)
   
   # try to infer the base indentation
   contents <- readLines(documentPath, warn = FALSE)
   indents <- regexpr("\\S", contents, perl = TRUE)
   baseIndent <- min(indents[indents >= 0]) - 1L

   # generate and write code to file
   expr <- rlang::expr({
      .libPaths(!!libraryPaths)
      styler::style_file(
         path = !!documentPath,
         indent_by = !!indent,
         strict = !!strict,
         base_indention = !!baseIndent
      )
   })

   writeLines(deparse(expr), con = scriptPath)
})
