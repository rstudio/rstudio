/*
 * SessionClang.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionClang.hpp"

#include <core/system/System.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "libclang/libclang.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

namespace {

FilePath libclangPath()
{
   // per-platform extension
#if defined(_WIN64)
   std::string libclang = "x86_64/libclang.dll";
#elif defined(_WIN32)
   std::string libclang = "x86/libclang.dll";
#elif defined(__APPLE__)
   std::string libclang = "libclang.dylib";
#else
   std::string libclang = "libclang.so";
#endif   
   return options().libclangPath().childPath(libclang);
}

bool isClangAvailable(std::string* pError)
{
   std::string path = libclangPath().absolutePath();
   module_context::consoleWriteOutput(path + "\n");
   rsclang::libclang lib(path);
   return lib.isLoaded(pError);
}

SEXP rs_isClangAvailable()
{   
   std::string error;
   bool isAvailable = isClangAvailable(&error);

   if (!isAvailable)
      module_context::consoleWriteError(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(isAvailable, &rProtect);
}

} // anonymous namespace
   
Error initialize()
{
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_isClangAvailable" ;
   methodDef.fun = (DL_FUNC)rs_isClangAvailable;
   methodDef.numArgs = 0;
   r::routines::addCallMethod(methodDef);

   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

