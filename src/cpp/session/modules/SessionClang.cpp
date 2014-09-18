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

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptions.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

namespace {

SEXP rs_clangPaths()
{
   std::vector<std::string> paths;

   paths.push_back(session::options().rsclangPath().absolutePath());
   paths.push_back(session::options().libclangPath().absolutePath());

   r::sexp::Protect rProtect;
   return r::sexp::create(paths, &rProtect);
}

} // anonymous namespace
   
Error initialize()
{
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_clangPaths" ;
   methodDef.fun = (DL_FUNC)rs_clangPaths ;
   methodDef.numArgs = 0;
   r::routines::addCallMethod(methodDef);

   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

