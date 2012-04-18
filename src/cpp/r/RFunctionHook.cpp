/*
 * RFunctionHook.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#define R_INTERNAL_FUNCTIONS
#include <r/RFunctionHook.hpp>

#include <map>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <r/RInternal.hpp>
#include <r/RExec.hpp>
#include <r/RErrorCategory.hpp>

// R-INTERNAL-IMPORT: from memory.c
extern "C" CCODE (PRIMFUN)(SEXP x) ;
extern "C" void (SET_PRIMFUN)(SEXP x, CCODE f) ;

// R-INTERNAL-IMPORT: from util.c
extern "C" void Rf_checkArityCall(SEXP op, SEXP args, SEXP call);

using namespace core ;

namespace r {
namespace function_hook {
   
namespace {

SEXP rUnsupportedHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::exec::errorCall(call, "function not supported in RStudio");
   return R_NilValue;
}
   
} // anonymous namespace
   

Error registerReplaceHook(const std::string& name, 
                          CCODE hook,
                          CCODE* pOriginal)
{
   // ensure a name was passed
   if (name.empty())
      return Error(errc::SymbolNotFoundError, ERROR_LOCATION);
   
   // find SEXP for function
   SEXP symbolSEXP = Rf_install(name.c_str());
   SEXP functionSEXP = INTERNAL(symbolSEXP) ;
   if (functionSEXP == R_NilValue || functionSEXP == R_UnboundValue)
   {
      functionSEXP = SYMVALUE(symbolSEXP);
      if (functionSEXP == R_NilValue || functionSEXP == R_UnboundValue)
      {
         Error error = Error(errc::SymbolNotFoundError, ERROR_LOCATION);
         error.addProperty("symbol", name);
         return error;
      }
   }
   
   // provide the original to the caller if requested
   if (pOriginal != NULL)
      *pOriginal = PRIMFUN(functionSEXP);
   
   // add the hook
   SET_PRIMFUN(functionSEXP, hook);
   
   return Success();
}
   
void checkArity(SEXP op, SEXP args, SEXP call)
{
   Rf_checkArityCall(op, args, call);
}
   
Error registerUnsupported(const std::string& name, const std::string& package)
{
   return r::exec::RFunction(".rs.registerUnsupported", name, package).call();
}
   
Error registerUnsupportedInternal(const std::string& name)
{
   return r::function_hook::registerReplaceHook(name, rUnsupportedHook, NULL);
}
       
} // namespace function_hook   
} // namespace r



