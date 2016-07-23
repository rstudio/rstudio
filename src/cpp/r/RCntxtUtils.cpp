/*
 * RCntxtUtils.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/RCntxt.hpp>
#include <r/RCntxtUtils.hpp>
#include <r/RInterface.hpp>
#include <r/RExec.hpp>
#include <r/RUtil.hpp>

namespace rstudio {
namespace r {
namespace context {


RCntxtVersion contextVersion()
{
   // cache the context version (we look this up constantly to figure out the
   // appropriate offsets into the RCNXT struct)
   static RCntxtVersion s_rCntxtVersion = RVersionUnknown;

   if (s_rCntxtVersion == RVersionUnknown)
   {
      // currently there are only two known memory layouts for the R context
      // structure (see RCntxtVersion enum)
      s_rCntxtVersion = r::util::hasRequiredVersion("3.3") ? 
         RVersion33 : 
         RVersion32;
   }
   return s_rCntxtVersion;
}

RCntxt globalContext()
{
   return RCntxt(getGlobalContext());
}

RCntxt firstFunctionContext()
{
   RCntxt::iterator firstFunContext = RCntxt::begin();
   while ((firstFunContext->callfun() == NULL ||
           firstFunContext->callfun() == R_NilValue) &&
          firstFunContext->callflag())
      firstFunContext++;
   return *firstFunContext;
}

RCntxt getFunctionContext(const int depth,
                          int* pFoundDepth,
                          SEXP* pEnvironment)
{
   RCntxt foundContext;
   int currentDepth = 0;
   int foundDepth = 0;
   SEXP browseEnv = R_NilValue;
   for (RCntxt::iterator ctxt = RCntxt::begin(); ctxt != RCntxt::end(); ctxt++)
   {
      // if looking for the actively browsed function, pick the environment
      // evaluated by the browser on top of the stack
      if (ctxt->callflag() & CTXT_BROWSER && browseEnv == R_NilValue)
      {
         browseEnv = ctxt->cloenv();
      }
      if (ctxt->callflag() & CTXT_FUNCTION)
      {
         currentDepth++;
         if (depth == BROWSER_FUNCTION && ctxt->cloenv() == browseEnv)
         {
            foundDepth = currentDepth;
            foundContext = *ctxt;
            // continue traversing the callstack; there may be several 
            // functions eval'ing this environment and we want the "original"
            // (here meaning oldest on the callstack)
         }
         else if (depth > BROWSER_FUNCTION && currentDepth >= depth)
         {
            foundDepth = currentDepth;
            foundContext = *ctxt;
            break;
         }
      }
   }

   // indicate the depth at which we stopped and the environment we found at
   // that depth, if requested
   if (pFoundDepth)
   {
      *pFoundDepth = foundDepth;
   }
   if (pEnvironment)
   {
      *pEnvironment = (foundDepth == 0 || foundContext.isNull()) ?
         R_GlobalEnv : 
         foundContext.cloenv();
   }
   return foundContext;
}

// Return whether we're in browse context--meaning that there's a browser on
// the context stack and at least one function (i.e. we're not browsing at the
// top level).
bool inBrowseContext()
{
   bool foundBrowser = false;
   bool foundFunction = false;
   for (RCntxt::iterator ctxt = RCntxt::begin(); ctxt != RCntxt::end(); ctxt++)
   {
      if ((ctxt->callflag() & CTXT_BROWSER) &&
          !(ctxt->callflag() & CTXT_FUNCTION))
      {
         foundBrowser = true;
      }
      else if (ctxt->callflag() & CTXT_FUNCTION)
      {
         foundFunction = true;
      }
      if (foundBrowser && foundFunction)
      {
         return true;
      }
   }
   return false;
}

// Return whether the current context is being evaluated inside a hidden
// (debugger internal) function at the top level.
bool inDebugHiddenContext()
{
   for (RCntxt::iterator ctxt = RCntxt::begin(); ctxt != RCntxt::end(); ctxt++)
   {
      if (ctxt->callflag() & CTXT_FUNCTION)
      {
         // If we find a debugger internal function before any user function,
         // hide it from the user callstack.
         if (ctxt->isDebugHidden())
            return true;

         // If we find a user function before we encounter a debugger internal
         // function, don't hide the user code it invokes.
         if (ctxt->hasSourceRefs())
             return false;
      }
   }
   return false;
}

} // namespace context
} // namespace r
} // namespace rstudio

