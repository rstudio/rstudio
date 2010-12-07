#
# SessionSource.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("save_active_document", function(contents, sweave)
{
   # manage working directory
   previousWd = getwd()
   setwd("~")
   on.exit(setwd(previousWd))

   writeChar(contents, "~/.active.document", eos=NULL)
   if (sweave)
   {
      op <- function() {
         utils::Stangle("~/.active.document")
         file.remove("~/.active.document")
         file.rename("~/.active.document.R", "~/.active.document")
      }
      capture.output(op())
   }
   return()
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
      if (funcName == '<<-')
	      assign(lvalue, T, envir=w$assignedGlobals)
      else
	      assign(lvalue, T, envir=w$assigned)
      args <- args[-1]
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
   if (typeof(e) == 'symbol' && nchar(e) > 0 && !exists(as.character(e), envir=w$assigned))
      return(as.character(e))
   else
      return(character(0))
})

.rs.addJsonRpcHandler("detect_free_vars", function(code)
{
   globals <- new.env(parent=emptyenv())
   w <- codetools:::makeCodeWalker(assigned=globals,
                                   assignedGlobals=globals,
                                   call=.rs.detectFreeVars_Call,
                                   leaf=.rs.detectFreeVars_Leaf)
   freeVars <- character(0)
   for (e in parse(text=code))
     freeVars <- c(freeVars, codetools:::walkCode(e, w))
   return(unique(freeVars))
})