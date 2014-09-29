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

#include <core/r_util/RPackageInfo.hpp>

#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "LibClang.hpp"
#include "SourceIndex.hpp"


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

std::vector<std::string> extractCompileArgs(const std::string& line)
{
   std::vector<std::string> compileArgs;

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

   return compileArgs;
}



std::string buildFileHash(const FilePath& filePath)
{
   if (filePath.exists())
   {
      std::ostringstream ostr;
      ostr << filePath.lastWriteTime();
      return ostr.str();
   }
   else
   {
      return std::string();
   }
}

std::string packageBuildFileHash()
{
   std::ostringstream ostr;
   FilePath buildPath = projects::projectContext().buildTargetPath();
   ostr << buildFileHash(buildPath.childPath("DESCRIPTION"));
   FilePath srcPath = buildPath.childPath("src");
   if (srcPath.exists())
   {
      ostr << buildFileHash(srcPath.childPath("Makevars"));
      ostr << buildFileHash(srcPath.childPath("Makevars.win"));
   }
   return ostr.str();
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

void CompilationDatabase::updateForCurrentPackage()
{
   // check hash to see if we can avoid this computation
   std::string buildFileHash = packageBuildFileHash();
   if (buildFileHash == packageBuildFileHash_)
      return;
   packageBuildFileHash_ = buildFileHash;

   TIME_FUNCTION

   // to approximate the compiler flags for files in the package src
   // directory we will build a temporary sourceCpp file
   std::ostringstream ostr;
   ostr << "#include <Rcpp.h>" << std::endl;
   ostr << "// [[Rcpp::export]]" << std::endl;
   ostr << "void foo() {}" << std::endl;

   // read the package description file
   using namespace projects;
   FilePath pkgPath = projectContext().buildTargetPath();
   core::r_util::RPackageInfo pkgInfo;
   Error error = pkgInfo.read(pkgPath);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // Discover all of the LinkingTo relationships
   if (!pkgInfo.linkingTo().empty())
   {
      std::vector<std::string> linkingTo;
      r::exec::RFunction parseLinkingTo(".rs.parseLinkingTo");
      parseLinkingTo.addParam(pkgInfo.linkingTo());
      Error error = parseLinkingTo.call(&linkingTo);
      if (error)
         LOG_ERROR(error);

      // emit Rcpp::depends for linking to
      BOOST_FOREACH(const std::string& pkg, linkingTo)
      {
         ostr << "// [[Rcpp::depends(" << pkg << ")]]" << std::endl;
      }
   }

   // write a temp file with all of the depends
   FilePath tempCpp = module_context::tempFile("clangdb", "cpp");
   error = core::writeStringToFile(tempCpp, ostr.str());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // Do a sourceCpp dryRun to capture the baseline config (bail on error)
   std::vector<std::string> args = argsForSourceCpp(tempCpp);
   if (args.empty())
      return;

   // Read Makevars to get PKG_CXXFLAGS and add that
   FilePath srcPath = pkgPath.childPath("src");
   if (srcPath.exists())
   {
      std::string makevars = "Makevars";
#ifdef _WIN32
      if (srcPath.childPath("Makevars.win").exists())
         makevars = "Makevars.win";
#endif
      FilePath makevarsPath = srcPath.childPath(makevars);
      if (makevarsPath.exists())
      {
         // read makevars into lines
         std::vector<std::string> lines;
         Error error = core::readStringVectorFromFile(makevarsPath, &lines);
         if (error)
            LOG_ERROR(error);

         bool continuationLine = false;
         BOOST_FOREACH(std::string line, lines)
         {
            boost::algorithm::trim_all(line);

            if (line.find("PKG_CFLAGS") != std::string::npos ||
                line.find("PKG_CXXFLAGS") != std::string::npos ||
                line.find("PKG_CPPFLAGS") != std::string::npos ||
                continuationLine)
            {
               std::vector<std::string> mArgs = extractCompileArgs(line);
               BOOST_FOREACH(std::string arg, mArgs)
               {
                  // do path substitutions
                  boost::algorithm::replace_first(
                           arg,
                           "-I..",
                           "-I" + srcPath.parent().absolutePath());
                  boost::algorithm::replace_first(
                           arg,
                           "-I.",
                           "-I" + srcPath.absolutePath());

                  args.push_back(arg);
               }

               continuationLine = boost::algorithm::ends_with(line, "\\");
            }
         }
      }
   }

   // set the args and re-build translation units if necessary
   if (args != packageSrcArgs_)
   {
      packageSrcArgs_ = args;

      // wipe out any exising translation units that map to this package
      FilePath pkgSrcDir = projectContext().buildTargetPath().childPath("src");
      std::vector<FilePath> pkgSrcFiles;
      error = pkgSrcDir.children(&pkgSrcFiles);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      BOOST_FOREACH(const FilePath& srcPath, pkgSrcFiles)
      {
         std::string filename = srcPath.absolutePath();
         if (sourceIndex().hasTranslationUnit(filename))
         {
            sourceIndex().removeTranslationUnit(filename);
            sourceIndex().updateTranslationUnit(filename);
         }
      }
   }
}

void CompilationDatabase::updateForStandaloneCpp(const core::FilePath& cppPath)
{
   TIME_FUNCTION

   // read the dependency attributes within the cpp file to compare to
   // previous sets of attributes we've used to generate compilation args.
   // bail if we've already generated based on these attributes
   std::string attributes = readDependencyAttributes(cppPath);
   AttribsMap::const_iterator it = attribsMap_.find(cppPath.absolutePath());
   if (it != attribsMap_.end() && it->second == attributes)
      return;

   // arguments for this translation unit
   std::vector<std::string> args = argsForSourceCpp(cppPath);

   // if we got args then update
   if (!args.empty())
   {
      // update if necessary
      updateIfNecessary(cppPath.absolutePath(), args);

      // save attributes to prevent recomputation
      attribsMap_[cppPath.absolutePath()] = attributes;
   }
}

std::vector<std::string> CompilationDatabase::argsForSourceCpp(
                                                     const FilePath& cppPath)
{
   // baseline args
   std::vector<std::string> compileArgs;
   std::string builtinHeaders = "-I" + clang().builtinHeaders();
   compileArgs.push_back(builtinHeaders);
#if defined(_WIN32)
   std::vector<std::string> rtoolsArgs = rToolsArgs();
   std::copy(rtoolsArgs.begin(),
             rtoolsArgs.end(),
             std::back_inserter(compileArgs));
#elif defined(__APPLE__)
   compileArgs.push_back("-stdlib=libstdc++");
#endif

   // get path to R script
   FilePath rScriptPath;
   Error error = module_context::rScriptPath(&rScriptPath);
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // setup env and options
   core::system::Options env;
   core::system::ProcessOptions options;

   // always run as a slave
   std::vector<std::string> args;
   args.push_back("--slave");

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

   // add rtools to path if we need to
   std::string warning;
   module_context::addRtoolsToPathIfNecessary(&env, &warning);

   // set environment into options
   options.environment = env;

   // add command to arguments
   args.push_back("-e");
   boost::format fmt("Rcpp::sourceCpp('%1%', showOutput = TRUE, dryRun = TRUE)");
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
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE("Error performing dry run: " + result.stdErr);
      return std::vector<std::string>();
   }

   // break into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, result.stdOut,
                           boost::algorithm::is_any_of("\r\n"));


   // find the line with the compilation and add it's args
   std::string compile = "-c " + cppPath.filename() + " -o " + cppPath.stem();
   BOOST_FOREACH(const std::string& line, lines)
   {
      if (line.find(compile) != std::string::npos)
      {
         std::vector<std::string> args = extractCompileArgs(line);
         std::copy(args.begin(), args.end(), std::back_inserter(compileArgs));
      }
   }

   // return the args
   return compileArgs;
}

std::vector<std::string> CompilationDatabase::argsForFile(
                                       const std::string& cppPath) const
{
   // if this is a package source file then return the package args
   using namespace projects;
   FilePath filePath(cppPath);
   if (projectContext().config().buildType == r_util::kBuildTypePackage &&
       filePath.parent() == projectContext().buildTargetPath().childPath("src"))
   {
      return packageSrcArgs_;
   }
   // otherwise lookup in the global dictionary
   else
   {
      ArgsMap::const_iterator it = argsMap_.find(cppPath);
      if (it != argsMap_.end())
         return it->second;
      else
         return std::vector<std::string>();
   }
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

