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

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

namespace {

FilePath rsclangPath()
{
   FilePath path = options().rsclangPath().childPath("rsclang");
   core::system::fixupExecutablePath(&path);
   return path;
}

FilePath libclangPath()
{
   // get the path to libclang
#if defined(_WIN32)
   std::string ext = ".dll";
#elif defined(__APPLE__)
   std::string ext = ".dylib";
#else
   std::string ext = ".so";
#endif
   return options().libclangPath().childPath("libclang" + ext);
}


std::vector<std::string> rsclangArgs(std::vector<std::string> args)
{
   args.push_back("--libclang-path");
   args.push_back(libclangPath().absolutePath());
   return args;
}

bool isClangAvailable(std::string* pError)
{
   // call to check for availability
   std::vector<std::string> args;
   args.push_back("--check-available");
   core::system::ProcessResult result;
   Error error = core::system::runProgram(rsclangPath().absolutePath(),
                                          rsclangArgs(args),
                                          "",
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
   {
      *pError = error.summary();
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      *pError = result.stdErr;
      return false;
   }
   else
   {
      return true;
   }
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

