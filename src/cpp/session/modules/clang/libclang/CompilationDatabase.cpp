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

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim_all.hpp>

#include <core/Error.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/FileSerializer.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include "LibClang.hpp"
#include "SourceIndex.hpp"

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

namespace {

std::string readDependencyAttributes(const core::FilePath& cppPath)
{
   // read file
   std::string contents;
   Error error = core::readStringFromFile(cppPath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   // find dependency attributes
   std::string attributes;
   boost::regex re(
     "^\\s*//\\s*\\[\\[Rcpp::(\\w+)(\\(.*?\\))?\\]\\]\\s*$");
   boost::sregex_token_iterator it(contents.begin(), contents.end(), re, 0);
   boost::sregex_token_iterator end;
   for ( ; it != end; ++it)
   {
      std::string attrib = *it;
      boost::algorithm::trim_all(attrib);
      attributes.append(attrib);
   }

   // return them
   return attributes;
}

std::vector<std::string> argsForSourceCpp(const core::FilePath& cppPath)
{
   // get path to R script
   FilePath rScriptPath;
   Error error = module_context::rScriptPath(&rScriptPath);
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // setup args and options
   std::vector<std::string> args;
   core::system::ProcessOptions options;

   // always run as a slave
   args.push_back("--slave");

   // setup environment to force make into --dry-run mode
   core::system::Options env;
   core::system::environment(&env);
   core::system::setenv(&env, "MAKE", "make --dry-run");
   core::system::setenv(&env, "R_MAKEVARS_USER", "");
   core::system::setenv(&env, "R_MAKEVARS_SITE", "");

   // for packrat projects we execute the profile and set the working
   // directory to the project directory; for other contexts we just
   // propagate the R_LIBS
   if (module_context::packratContext().modeOn)
   {
      options.workingDir = projects::projectContext().directory();
      args.push_back("--no-save");
      args.push_back("--no-restore");
   }
   else
   {
      args.push_back("--vanilla");
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&env, "R_LIBS", libPaths);
   }

   // set environment into options
   options.environment = env;

   // add command to arguments
   args.push_back("-e");
   boost::format fmt("Rcpp::sourceCpp('%1%', showOutput = TRUE)");
   args.push_back(boost::str(fmt % cppPath.absolutePath()));

   // execute and capture output
   core::system::ProcessResult result;
   error = core::system::runProgram(
            core::string_utils::utf8ToSystem(rScriptPath.absolutePath()),
            args,
            "",
            options,
            &result);
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // break into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, result.stdOut,
                           boost::algorithm::is_any_of("\r\n"));


   // find the line with the compilation and add it's args
   std::vector<std::string> compileArgs;
   std::string compile = "-c " + cppPath.filename() + " -o " + cppPath.stem();
   BOOST_FOREACH(const std::string& line, lines)
   {
      if (line.find(compile) != std::string::npos)
      {
         // find arguments libclang might care about
         boost::regex re("-[I|D|i|f|s](?:\\\"[^\\\"]+\\\"|[^ ]+)");
         boost::sregex_token_iterator it(line.begin(), line.end(), re, 0);
         boost::sregex_token_iterator end;
         for ( ; it != end; ++it)
         {
            // remove quotes and add it to the compile args
            std::string arg = *it;
            boost::algorithm::replace_all(arg, "\"", "");
            compileArgs.push_back(arg);
         }
         break;
      }
   }

   // return the args
   return compileArgs;
}

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

void CompilationDatabase::updateForPackageCppAddition(
                                          const core::FilePath& cppPath)
{
   // if we don't have this source file already then fully update
   if (argsMap_.find(cppPath.absolutePath()) == argsMap_.end())
      updateForCurrentPackage();
}

void CompilationDatabase::updateForCurrentPackage()
{

}

void CompilationDatabase::updateForStandaloneCpp(const core::FilePath& cppPath)
{
   TIME_FUNCTION

   // if we don't have Rcpp attributes then forget it
   if (!module_context::haveRcppAttributes())
      return;

   // read the dependency attributes within the cpp file to compare to
   // previous sets of attributes we've used to generate compilation args.
   // bail if we've already generated based on these attributes
   std::string attributes = readDependencyAttributes(cppPath);
   AttribsMap::const_iterator it = attribsMap_.find(cppPath.absolutePath());
   if (it != attribsMap_.end() && it->second == attributes)
      return;

   // baseline arguments
   std::vector<std::string> args;
   std::string builtinHeaders = "-I" + clang().builtinHeaders();
   args.push_back(builtinHeaders);
#if defined(_WIN32)
   std::vector<std::string> rtoolsArgs = rToolsArgs();
   std::copy(rtoolsArgs.begin(), rtoolsArgs.end(), std::back_inserter(args));
#elif defined(__APPLE__)
   args.push_back("-stdlib=libstdc++");
#endif

   // arguments for this translation unit
   std::vector<std::string> fileArgs = argsForSourceCpp(cppPath);

   // if we got args then update
   if (!fileArgs.empty())
   {
      // combine them
      std::copy(fileArgs.begin(), fileArgs.end(), std::back_inserter(args));

      // update if necessary
      updateIfNecessary(cppPath.absolutePath(), args);

      // save attributes to prevent recomputation
      attribsMap_[cppPath.absolutePath()] = attributes;
   }
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

std::vector<std::string> CompilationDatabase::rToolsArgs() const
{

#ifdef _WIN32
   if (rToolsArgs_.empty())
   {
      // scan for Rtools
      std::vector<core::r_util::RToolsInfo> rTools;
      Error error = core::r_util::scanRegistryForRTools(&rTools);
      if (error)
         LOG_ERROR(error);

      // enumerate them to see if we have a compatible version
      // (go in reverse order for most recent first)
      std::vector<r_util::RToolsInfo>::const_reverse_iterator it = rTools.rbegin();
      for ( ; it != rTools.rend(); ++it)
      {
         if (module_context::isRtoolsCompatible(*it))
         {
            FilePath rtoolsPath = it->installPath();

            rToolsArgs_.push_back("-I" + rtoolsPath.childPath(
               "gcc-4.6.3/i686-w64-mingw32/include").absolutePath());

            rToolsArgs_.push_back("-I" + rtoolsPath.childPath(
               "gcc-4.6.3/include/c++/4.6.3").absolutePath());

            std::string bits = "-I" + rtoolsPath.childPath(
               "gcc-4.6.3/include/c++/4.6.3/i686-w64-mingw32").absolutePath();
#ifdef _WIN64
            bits += "/64";
#endif
            rToolsArgs_.push_back(bits);

            break;
         }
      }
   }
#endif

   return rToolsArgs_;
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

