#
# NotebookAlternateEngines.R
#
# Copyright (C) 2009-17 by RStudio, Inc.
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

.rs.addFunction("isSystemInterpreter", function(engine)
{
   # check to see if a knitr engine is defined
   knitr <- asNamespace("knitr")
   engines <- knitr$knit_engines$get()
   if (engine %in% names(engines))
   {
      # knitr engine defined; check to see if the engine uses 'eng_interpeted'
      isInterpreted <-
         is.function(knitr$eng_interpreted) &&
         is.function(engines[[engine]]) &&
         identical(engines[[engine]], knitr$eng_interpreted)
      isInterpreted
   }
   else
   {
      # no knitr engine defined; assume that the user intended to use a system
      # interpreter
      TRUE
   }
})

.rs.addFunction("runUserDefinedEngine", function(engine, options)
{
   # retrieve the engine
   knitrEngines <- knitr::knit_engines$get()
   if (!engine %in% names(knitrEngines))
   {
      fmt <- "engine '%s' has not yet been registered"
      stop(sprintf(fmt, options))
   }
   
   # double-check that this is indeed a function (shouldn't happen
   # but should guard against invalid user-defined engines)
   knitrEngine <- knitrEngines[[engine]]
   if (!is.function(knitrEngine))
   {
      fmt <- "engine '%s' is not a function"
      stop(sprintf(fmt, options))
   }
   
   # invoke engine
   mergedOptions <- knitr::opts_chunk$merge(options)
   knitrEngine(mergedOptions)
   
})
