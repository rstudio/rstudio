/*
 * RCntxt.cpp
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
#include <r/RExec.hpp>
#include <r/RNullCntxt.hpp>
#include <r/RCntxtUtils.hpp>

#include <core/Error.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace context {

Error RCntxt::callSummary(std::string* pCallSummary)
{
   return invokeFunctionOnCall(".rs.callSummary", pCallSummary);
}

Error RCntxt::functionName(std::string* pFunctionName)
{
   return invokeFunctionOnCall(".rs.functionNameFromCall", pFunctionName);
}

bool RCntxt::isDebugHidden()
{
   SEXP hideFlag = r::sexp::getAttrib(callfun(), "hideFromDebugger");
   return TYPEOF(hideFlag) != NILSXP && r::sexp::asLogical(hideFlag);
}

bool RCntxt::isErrorHandler()
{
   SEXP errFlag = r::sexp::getAttrib(callfun(), "errorHandlerType");
   return TYPEOF(errFlag) == INTSXP;
}

bool RCntxt::hasSourceRefs()
{
   SEXP refs = sourceRefs();
   return refs && TYPEOF(refs) != NILSXP;
}

SEXP RCntxt::sourceRefs()
{
   return r::sexp::getAttrib(originalFunctionCall(), "srcref");
}

std::string RCntxt::shinyFunctionLabel()
{
   std::string label;
   SEXP s = r::sexp::getAttrib(originalFunctionCall() , "_rs_shinyDebugLabel");
   if (s != NULL && TYPEOF(s) != NILSXP)
   {
      r::sexp::extract(s, &label);
   }
   return label;
}

// enabling tracing on a function turns it into an S4 object with an 'original'
// slot that includes the function's original contents. use this instead if
// it's set up. (consider: is it safe to assume that S4 objects here are always
// traced functions, or do we need to compare classes to be safe?)
SEXP RCntxt::originalFunctionCall()
{
   SEXP callObject = callfun();

   if (Rf_isS4(callObject))
   {
      callObject = r::sexp::getAttrib(callObject, "original");
   }
   return callObject;
}

Error RCntxt::fileName(std::string* pFileName)
{
   if (srcref() && TYPEOF(srcref()) != NILSXP)
   {
      r::sexp::Protect protect;
      SEXP fileName;
      Error error = r::exec::RFunction(".rs.sourceFileFromRef", srcref)
                    .call(&fileName, &protect);
      if (error)
          return error;

      return r::sexp::extract(fileName, pFileName, true);
   }
   else
   {
      // If no source references, that's OK--just set an empty filename.
      pFileName->clear();
      return Success();
   }
}

// call objects can't be passed as primary values through our R interface
// (early evaluation can be triggered) so we wrap them in an attribute attached
// to a dummy value when we need to pass them through
Error RCntxt::invokeFunctionOnCall(const char* rFunction, std::string* pResult)
{
   SEXP result;
   r::sexp::Protect protect;
   SEXP val = r::sexp::create("_rs_callval", &protect);
   r::sexp::setAttrib(val, "_rs_call", call());
   Error error = r::exec::RFunction(rFunction, val)
                            .call(&result, &protect);
   if (!error && r::sexp::length(result) > 0)
   {
      error = r::sexp::extract(result, pResult, true);
   }
   else
   {
      pResult->clear();
   }
   return error;
}

bool RCntxt::operator==(const RCntxt& other) const
{
   return other.rcntxt() == rcntxt();
}

RCntxt::iterator RCntxt::begin()
{
   return RCntxt::iterator(globalContext());
}

RCntxt::iterator RCntxt::end()
{
   return RCntxt::iterator(RNullCntxt());
}

} // namespace context
} // namespace r
} // namespace rstudio
