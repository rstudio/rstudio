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

#include <boost/foreach.hpp>

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

FilePath embeddedLibClangPath()
{
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


std::vector<FilePath> clangVersions()
{
   std::vector<FilePath> clangVersions;

   // embedded version
   clangVersions.push_back(embeddedLibClangPath());

   // platform-specific other versions
#ifndef _WIN32
#ifdef __APPLE__
   clangVersions.push_back(FilePath("/Applications/Xcode.app/Contents/"
                           "Developer/Toolchains/XcodeDefault.xctoolchain"
                           "/usr/lib/libclang.dylib"));
#else

#endif
#endif

   return clangVersions;
}

// load libclang (using embedded version if possible then search
// for other versions in well known locations)
bool loadLibclang(libclang* pLibClang, std::string* pDiagnostics)
{
   // get all possible clang versions
   std::ostringstream ostr;
   std::vector<FilePath> versions = clangVersions();
   BOOST_FOREACH(const FilePath& versionPath, versions)
   {
      ostr << versionPath << std::endl;
      if (versionPath.exists())
      {
         Error error = pLibClang->load(versionPath.absolutePath());
         if (!error)
         {
            ostr << "   (Successfully Loaded)" << std::endl;
            *pDiagnostics = ostr.str();
            return true;
         }
         else
         {
            ostr << "   (" << error.getProperty("dlerror") <<  ")" << std::endl;
         }
      }
      else
      {
         ostr << "   (Not Found)" << std::endl;
      }
   }

   // if we didn't find one by now then we failed
   *pDiagnostics = ostr.str();
   return false;
}


// diagnostic function to assist in determine whether/where
// libclang was loaded from (and any errors which occurred
// that prevented loading, e.g. inadequate version, missing
// symbols, etc.)
SEXP rs_isClangAvailable()
{
   // check availability
   libclang lib;
   std::string diagnostics;
   bool isAvailable = loadLibclang(&lib, &diagnostics);

   // print diagnostics
   module_context::consoleWriteOutput(diagnostics);

   // return status
   r::sexp::Protect rProtect;
   return r::sexp::create(isAvailable, &rProtect);
}

// shared instance of libclang
libclang& clang()
{
   static class libclang instance;
   return instance;
}


} // anonymous namespace
   
Error initialize()
{
   // attempt to load libclang
   std::string diagnostics;
   loadLibclang(&(clang()), &diagnostics);

   // register diagnostics function
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

