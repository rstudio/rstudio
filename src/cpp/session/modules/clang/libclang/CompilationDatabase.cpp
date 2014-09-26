/*
 * CompilationDatabase.cpp
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

#include "CompilationDatabase.hpp"

#include <algorithm>

#include <core/Error.hpp>

#include "LibClang.hpp"
#include "SourceIndex.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

namespace {


} // anonymous namespace

CompilationDatabase::~CompilationDatabase()
{
   try
   {

   }
   catch(...)
   {
   }
}

void CompilationDatabase::updateForCurrentPackage()
{

}

void CompilationDatabase::updateForStandaloneCpp(const core::FilePath& cppPath)
{
   std::vector<std::string> args;
   std::string builtinHeaders = "-I" + clang().builtinHeaders();
   args.push_back(builtinHeaders.c_str());

#if defined(_WIN32)
   args.push_back("-IC:/RBuildTools/3.1/gcc-4.6.3/i686-w64-mingw32/include");
   args.push_back("-IC:/RBuildTools/3.1/gcc-4.6.3/include/c++/4.6.3");
   args.push_back("-IC:/RBuildTools/3.1/gcc-4.6.3/include/c++/4.6.3/i686-w64-mingw32");
   args.push_back("-IC:/Program Files/R/R-3.1.0/include");
   args.push_back("-DNDEBUG");
   args.push_back("-IC:/Users/jjallaire/Documents/R/win-library/3.1/Rcpp/include");
   args.push_back("-Id:/RCompile/CRANpkg/extralibs64/local/include");
#elif defined(__APPLE__)
   args.push_back("-stdlib=libstdc++");
   args.push_back("-I/Library/Frameworks/R.framework/Resources/include");
   args.push_back("-DNDEBUG");
   args.push_back("-I/usr/local/include");
   args.push_back("-I/usr/local/include/freetype2");
   args.push_back("-I/opt/X11/include");
   args.push_back("-I/Library/Frameworks/R.framework/Resources/library/Rcpp/include");
#else
   args.push_back("-I/usr/share/R/include");
   args.push_back("-DNDEBUG");
   args.push_back("-I/home/jjallaire/R/x86_64-pc-linux-gnu-library/3.1/Rcpp/include");
#endif

   updateIfNecessary(cppPath.absolutePath(), args);
}

std::vector<std::string> CompilationDatabase::argsForFile(
                                       const std::string& cppPath) const
{
   ArgsMap::const_iterator it = argsMap_.find(cppPath);
   if (it != argsMap_.end())
      return it->second;
   else
      return std::vector<std::string>();
}

void CompilationDatabase::updateIfNecessary(
                                    const std::string& cppPath,
                                    const std::vector<std::string>& args)
{
   // get existing args
   std::vector<std::string> existingArgs = argsForFile(cppPath);
   if (args != existingArgs)
   {
      // invalidate the source index
      sourceIndex().removeTranslationUnit(cppPath);

      // update
      argsMap_[cppPath] = args;
   }
}

CompilationDatabase& compilationDatabase()
{
   static CompilationDatabase instance;
   return instance;
}



} // namespace libclang
} // namespace clang
} // namespace modules
} // namesapce session

