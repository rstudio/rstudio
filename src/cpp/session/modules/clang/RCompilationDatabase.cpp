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

#include "RCompilationDatabase.hpp"

#include <algorithm>

#include <boost/format.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim_all.hpp>

#include <core/Error.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/FileSerializer.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ProcessArgs.hpp>

#include <core/r_util/RPackageInfo.hpp>

#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "libclang/Clang.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

using namespace libclang;

namespace {

std::string readDependencyAttributes(const core::FilePath& srcPath)
{
   // read file
   std::string contents;
   Error error = core::readStringFromFile(srcPath, &contents);
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
   boost::regex re("[ \\t]-(?:[IDif]|std)(?:\\\"[^\\\"]+\\\"|[^ ]+)");
   boost::sregex_token_iterator it(line.begin(), line.end(), re, 0);
   boost::sregex_token_iterator end;
   for ( ; it != end; ++it)
   {
      // remove quotes and add it to the compile args
      std::string arg = *it;
      boost::algorithm::trim_all(arg);
      boost::algorithm::replace_all(arg, "\"", "");
      compileArgs.push_back(arg);
   }

   return compileArgs;
}

std::string extractStdArg(const std::vector<std::string>& args)
{
   BOOST_FOREACH(const std::string& arg, args)
   {
      if (boost::algorithm::starts_with(arg, "-std="))
         return arg;
   }

   return std::string();
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

RCompilationDatabase::~RCompilationDatabase()
{
   try
   {

   }
   catch(...)
   {
   }
}

void RCompilationDatabase::updateForCurrentPackage()
{
   // check hash to see if we can avoid this computation
   std::string buildFileHash = packageBuildFileHash();
   if (buildFileHash == packageBuildFileHash_)
      return;
   packageBuildFileHash_ = buildFileHash;

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
   std::vector<std::string> args = computeArgsForSourceFile(tempCpp);
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

   // set the args
   packageSrcArgs_ = args;
}

void RCompilationDatabase::updateForStandalone(const core::FilePath& srcPath)
{
   // read the dependency attributes within the source file to compare to
   // previous sets of attributes we've used to generate compilation args.
   // bail if we've already generated based on these attributes
   std::string attributes = readDependencyAttributes(srcPath);
   AttribsMap::const_iterator it = attribsMap_.find(srcPath.absolutePath());
   if (it != attribsMap_.end() && it->second == attributes)
      return;

   // arguments for this translation unit
   std::vector<std::string> args = computeArgsForSourceFile(srcPath);

   // if we got args then update
   if (!args.empty())
   {
      argsMap_[srcPath.absolutePath()] = args;

      // save attributes to prevent recomputation
      attribsMap_[srcPath.absolutePath()] = attributes;
   }
}

std::vector<std::string> RCompilationDatabase::computeArgsForSourceFile(
                                                     const FilePath& srcFile)
{
   // is this a c++ file
   std::string ext = srcFile.extensionLowerCase();
   bool isCppFile = (ext == ".cc") || (ext == ".cpp");

   // compile args
   std::vector<std::string> compileArgs;

   // clang compile args
   std::vector<std::string> clangCompileArgs = clang().compileArgs(isCppFile);
   std::copy(clangCompileArgs.begin(),
             clangCompileArgs.end(),
             std::back_inserter(compileArgs));

   // rtools on windows
   core::system::Options env;
   core::system::environment(&env);
#if defined(_WIN32)
   std::vector<std::string> rtoolsArgs = rToolsArgs();
   std::copy(rtoolsArgs.begin(),
             rtoolsArgs.end(),
             std::back_inserter(compileArgs));

   std::string warning;
   module_context::addRtoolsToPathIfNecessary(&env, &warning);
#endif 

   // handle C++ and C differently
   Error error;
   core::system::ProcessResult result;
   if (isCppFile)
       error = executeSourceCpp(env, srcFile, &result);
   else
       error = executeRCmdSHLIB(env, srcFile, &result);

   // process results
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
   std::string compile = "-c " + srcFile.filename() + " -o " + srcFile.stem();
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

Error RCompilationDatabase::executeSourceCpp(
                                      core::system::Options env,
                                      const core::FilePath& srcPath,
                                      core::system::ProcessResult* pResult)
{
   // get path to R script
   FilePath rScriptPath;
   Error error = module_context::rScriptPath(&rScriptPath);
   if (error)
      return error;

   // establish options
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


   // set environment into options
   options.environment = env;

   // add command to arguments
   args.push_back("-e");
   boost::format fmt("Rcpp::sourceCpp('%1%', showOutput = TRUE, dryRun = TRUE)");
   args.push_back(boost::str(fmt % srcPath.absolutePath()));

   // execute and capture output
   return core::system::runProgram(
            core::string_utils::utf8ToSystem(rScriptPath.absolutePath()),
            args,
            "",
            options,
            pResult);
}

core::Error RCompilationDatabase::executeRCmdSHLIB(
                                 core::system::Options env,
                                 const core::FilePath& srcPath,
                                 core::system::ProcessResult* pResult)
{
   // get R bin directory
   FilePath rBinDir;
   Error error = module_context::rBinDir(&rBinDir);
   if (error)
      return error;

   // compile the file as dry-run
   module_context::RCommand rCmd(rBinDir);
   rCmd << "SHLIB";
   rCmd << "--dry-run";
   rCmd << srcPath.filename();

   // set options and run
   core::system::ProcessOptions options;
   options.workingDir = srcPath.parent();
   options.environment = env;
   return core::system::runCommand(rCmd.commandString(), options, pResult);
}

namespace {

inline bool endsWith(const std::string& input, const std::string& test)
{
   return boost::algorithm::ends_with(input, test);
}

std::string determinePCHPackage(const std::vector<std::string>&)
{
   return "Rcpp";
}

} // anonymous namespace


std::vector<std::string> RCompilationDatabase::compileArgsForTranslationUnit(
                                            const std::string& filename)
{
   // args to return
   std::vector<std::string> args;

   // get a file path object
   FilePath filePath(filename);

   // if this is a package source file then return the package args
   std::string packageName;
   using namespace projects;
   FilePath srcDirPath = projectContext().buildTargetPath().childPath("src");
   if ((projectContext().config().buildType == r_util::kBuildTypePackage) &&
       !filePath.relativePath(srcDirPath).empty())
   {
      // (re-)create on demand
      updateForCurrentPackage();

      args = packageSrcArgs_;

      packageName = projectContext().packageInfo().name();
   }
   // otherwise lookup in the global dictionary
   else
   {
      // (re-)create on demand
      updateForStandalone(filePath);

      ArgsMap::const_iterator it = argsMap_.find(filePath.absolutePath());
      if (it != argsMap_.end())
         args = it->second;
   }

   // bail if we have no args
   if (args.empty())
      return args;

   // determine which precompiled headers to include (default to Rcpp
   // but also check for RcppArmadillo)
   std::string pchPackage = determinePCHPackage(args);

   // remove automatically included directory for the package that we are
   // currently working on (we'll rely on inst/include which should be
   // picked up from the scan of Makevars)
   if (!packageName.empty())
   {
      std::vector<std::string>::iterator it = std::find_if(
               args.begin(),
               args.end(),
               boost::bind(endsWith, _1, "/" + packageName + "/include"));
      if (it != args.end())
         args.erase(it);
   }

   // add precompiled headers if they are not for this package
   if (packageName != pchPackage)
   {
      std::string ext = filePath.extensionLowerCase();
      bool isCppFile = (ext == ".cc") || (ext == ".cpp");
      if (isCppFile)
      {
         // extract any -std= argument
         std::string stdArg = extractStdArg(args);

         std::vector<std::string> pchArgs = precompiledHeaderArgs(pchPackage,
                                                                  stdArg);
         std::copy(pchArgs.begin(),
                   pchArgs.end(),
                   std::back_inserter(args));
      }
   }

   // return args
   return args;
}

std::vector<std::string> RCompilationDatabase::translationUnits()
{
   using namespace projects;
   std::vector<FilePath> allSrcFiles;
   if (projectContext().config().buildType == r_util::kBuildTypePackage)
   {
      FilePath srcPath = projectContext().buildTargetPath().childPath("src");
      if (srcPath.exists())
      {
         Error error = srcPath.children(&allSrcFiles);
         if (!error)
         {
            std::vector<std::string> srcFiles;
            BOOST_FOREACH(const FilePath& srcFile, allSrcFiles)
            {
               std::string filename = srcFile.absolutePath();
               if (SourceIndex::isTranslationUnit(filename))
                  srcFiles.push_back(filename);
            }
            return srcFiles;
         }
         else
         {
            LOG_ERROR(error);
         }
      }
   }

   // no love
   return std::vector<std::string>();
}

std::vector<std::string> RCompilationDatabase::rToolsArgs() const
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

std::vector<std::string> RCompilationDatabase::precompiledHeaderArgs(
                                                  const std::string& pkgName,
                                                  const std::string& stdArg)
{
   // args to return
   std::vector<std::string> args;

   // precompiled rcpp dir
   const std::string kPrecompiledDir = "libclang/precompiled/" + pkgName;
   FilePath precompiledDir = module_context::userScratchPath().
                                            childPath(kPrecompiledDir);

   // platform/rcpp version specific directory name
   std::string clangVersion = clang().version().asString();
   std::string platformDir;
   Error error = r::exec::RFunction(".rs.clangPCHPath", pkgName, clangVersion)
                                                         .call(&platformDir);
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // if this path doesn't exist then blow away all precompiled paths
   // and re-create this one. this enforces only storing precompiled headers
   // for the current version of R/Rcpp/pkg -- if we didn't do this then the
   // storage cost could really pile up over time (~25MB per PCH)
   FilePath platformPath = precompiledDir.childPath(platformDir);
   if (!platformPath.exists())
   {
      // delete root directory
      Error error = precompiledDir.removeIfExists();
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }

      // create platform directory
      error = platformPath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }
   }

   // now create the PCH if we need to
   FilePath pchPath = platformPath.childPath(pkgName + stdArg + ".pch");
   if (!pchPath.exists())
   {
      // state cpp file for creating precompiled headers
      FilePath cppPath = platformPath.childPath(pkgName + stdArg + ".cpp");
      std::string contents;
      boost::format fmt("#include <%1%.h>\n");
      contents.append(boost::str(fmt % pkgName));
      error = core::writeStringToFile(cppPath, contents);
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }

      // compute args (including -std= if we have one)
      std::vector<std::string> args = computeArgsForSourceFile(cppPath);
      if (!stdArg.empty())
         args.push_back(stdArg);
      core::system::ProcessArgs argsArray(args);

      CXIndex index = clang().createIndex(0,0);

      CXTranslationUnit tu = clang().parseTranslationUnit(
                            index,
                            cppPath.absolutePath().c_str(),
                            argsArray.args(),
                            argsArray.argCount(),
                            0,
                            0,
                            CXTranslationUnit_ForSerialization);
      if (tu == NULL)
      {
         LOG_ERROR_MESSAGE("Error parsing translation unit " +
                           cppPath.absolutePath());
         clang().disposeIndex(index);
         return std::vector<std::string>();
      }

      int ret = clang().saveTranslationUnit(tu,
                                            pchPath.absolutePath().c_str(),
                                            clang().defaultSaveOptions(tu));
      if (ret != CXSaveError_None)
      {
         boost::format fmt("Error %1% saving translation unit %2%");
         std::string msg = boost::str(fmt % ret % pchPath.absolutePath());
         LOG_ERROR_MESSAGE(msg);
      }

      clang().disposeTranslationUnit(tu);

      clang().disposeIndex(index);
   }

   // reutrn the pch header file args
   args.push_back("-include-pch");
   args.push_back(pchPath.absolutePath());
   return args;
}



RCompilationDatabase& rCompilationDatabase()
{
   static RCompilationDatabase instance;
   return instance;
}

} // namespace clang
} // namespace modules
} // namesapce session

