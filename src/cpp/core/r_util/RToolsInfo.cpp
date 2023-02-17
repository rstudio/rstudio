/*
 * RToolsInfo.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/Version.hpp>
#include <core/r_util/RToolsInfo.hpp>

#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <core/http/URL.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <core/system/RegistryKey.hpp>

#ifndef KEY_WOW64_32KEY
#define KEY_WOW64_32KEY 0x0200
#endif

namespace rstudio {
namespace core {
namespace r_util {

namespace {

std::string asRBuildPath(const FilePath& filePath)
{
   std::string path = filePath.getAbsolutePath();
   boost::algorithm::replace_all(path, "\\", "/");
   if (!boost::algorithm::ends_with(path, "/"))
      path += "/";
   return path;
}

std::vector<std::string> gcc463ClangArgs(const FilePath& installPath)
{
   std::vector<std::string> clangArgs;
   clangArgs.push_back("-I" + installPath.completeChildPath(
      "gcc-4.6.3/i686-w64-mingw32/include").getAbsolutePath());

   clangArgs.push_back("-I" + installPath.completeChildPath(
      "gcc-4.6.3/include/c++/4.6.3").getAbsolutePath());

   std::string bits = "-I" + installPath.completeChildPath(
      "gcc-4.6.3/include/c++/4.6.3/i686-w64-mingw32").getAbsolutePath();
#ifdef _WIN64
   bits += "/64";
#endif
   clangArgs.push_back(bits);
   return clangArgs;
}

void gcc463Configuration(const FilePath& installPath,
                         std::vector<std::string>* pRelativePathEntries,
                         std::vector<std::string>* pClangArgs)
{
   pRelativePathEntries->push_back("bin");
   pRelativePathEntries->push_back("gcc-4.6.3/bin");
   *pClangArgs = gcc463ClangArgs(installPath);
}

} // anonymous namespace


RToolsInfo::RToolsInfo(const std::string& name,
                       const FilePath& installPath,
                       bool usingMingwGcc49)
   : name_(name), installPath_(installPath)
{
   std::string versionMin, versionMax;
   std::vector<std::string> relativePathEntries;
   std::vector<std::string> clangArgs;
   std::vector<core::system::Option> environmentVars;

   std::vector<std::string> cIncludePaths;
   std::vector<std::string> cppIncludePaths;

   if (name == "2.11")
   {
      versionMin = "2.10.0";
      versionMax = "2.11.1";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("perl/bin");
      relativePathEntries.push_back("MinGW/bin");
   }
   else if (name == "2.12")
   {
      versionMin = "2.12.0";
      versionMax = "2.12.2";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("perl/bin");
      relativePathEntries.push_back("MinGW/bin");
      relativePathEntries.push_back("MinGW64/bin");
   }
   else if (name == "2.13")
   {
      versionMin = "2.13.0";
      versionMax = "2.13.2";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("MinGW/bin");
      relativePathEntries.push_back("MinGW64/bin");
   }
   else if (name == "2.14")
   {
      versionMin = "2.13.0";
      versionMax = "2.14.2";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("MinGW/bin");
      relativePathEntries.push_back("MinGW64/bin");
   }
   else if (name == "2.15")
   {
      versionMin = "2.14.2";
      versionMax = "2.15.1";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
      clangArgs = gcc463ClangArgs(installPath);
   }
   else if (name == "2.16" || name == "3.0")
   {
      versionMin = "2.15.2";
      versionMax = "3.0.99";
      gcc463Configuration(installPath, &relativePathEntries, &clangArgs);
   }
   else if (name == "3.1")
   {
      versionMin = "3.0.0";
      versionMax = "3.1.99";
      gcc463Configuration(installPath, &relativePathEntries, &clangArgs);
   }
   else if (name == "3.2")
   {
      versionMin = "3.1.0";
      versionMax = "3.2.0";
      gcc463Configuration(installPath, &relativePathEntries, &clangArgs);
   }
   else if (name == "3.3")
   {
      versionMin = "3.2.0";
      versionMax = "3.2.99";
      gcc463Configuration(installPath, &relativePathEntries, &clangArgs);
   }
   else if (name == "3.4" || name == "3.5")
   {
      versionMin = "3.3.0";
      if (name == "3.4")
         versionMax = "3.5.99";  // Rtools 3.4
      else 
         versionMax = "3.6.99";  // Rtools 3.5

      relativePathEntries.push_back("bin");

      // set environment variables
      FilePath gccPath = installPath_.completeChildPath("mingw_$(WIN)/bin");
      environmentVars.push_back(
            std::make_pair("BINPREF", asRBuildPath(gccPath)));

      // set clang args
#ifdef _WIN64
      std::string baseDir = "mingw_64";
      std::string arch = "x86_64";
#else
      std::string baseDir = "mingw_32";
      std::string arch = "i686";
#endif

      boost::format mgwIncFmt("%1%/%2%-w64-mingw32/include");
      std::string mgwInc = boost::str(mgwIncFmt % baseDir % arch);
      clangArgs.push_back(
            "-I" + installPath.completeChildPath(mgwInc).getAbsolutePath());

      std::string cppInc = mgwInc + "/c++";
      clangArgs.push_back(
            "-I" + installPath.completeChildPath(cppInc).getAbsolutePath());

      boost::format bitsIncFmt("%1%/%2%-w64-mingw32");
      std::string bitsInc = boost::str(bitsIncFmt % cppInc % arch);
      clangArgs.push_back(
            "-I" + installPath.completeChildPath(bitsInc).getAbsolutePath());
   }
   else if (name == "4.0")
   {
      versionMin = "4.0.0";
      versionMax = "4.1.99";

      // PATH for utilities
      relativePathEntries.push_back("usr/bin");

      // set BINPREF
      environmentVars.push_back({"BINPREF", "/mingw$(WIN)/bin/"});

      // set RTOOLS40_HOME
      std::string rtoolsPath = installPath.getAbsolutePath();
      std::replace(rtoolsPath.begin(), rtoolsPath.end(), '/', '\\');
      environmentVars.push_back({"RTOOLS40_HOME", rtoolsPath});

      // undefine _MSC_VER, so that we can "pretend" to be gcc
      // this is important for C++ libraries which might try to use
      // MSVC-specific tools when _MSC_VER is defined (e.g. Eigen), which might
      // not actually be defined or available in Rtools
      clangArgs.push_back("-U_MSC_VER");

      // set GNUC levels
      // (required for _mingw.h, which otherwise tries to use incompatible MSVC defines)
      clangArgs.push_back("-D__GNUC__=8");
      clangArgs.push_back("-D__GNUC_MINOR__=3");
      clangArgs.push_back("-D__GNUC_PATCHLEVEL__=0");

      // set compiler include paths
#ifdef _WIN64
      std::string baseDir = "mingw64";
      std::string triple = "x86_64-w64-mingw32";
#else
      std::string baseDir = "mingw32";
      std::string triple = "i686-w64-mingw32";
#endif

      std::vector<std::string> cStems = {
         "lib/gcc/" + triple + "/8.3.0/include",
         "include",
         "lib/gcc/" + triple + "/8.3.0/include-fixed",
         triple + "/include"
      };

      for (auto&& cStem : cStems)
      {
         FilePath includePath = installPath.completeChildPath(baseDir + "/" + cStem);
         cIncludePaths.push_back(includePath.getAbsolutePath());
      }

      std::vector<std::string> cppStems = {
         "include/c++/8.3.0",
         "include/c++/8.3.0/" + triple,
         "include/c++/8.3.0/backward",
         "lib/gcc/" + triple + "/8.3.0/include",
         "include",
         "lib/gcc/" + triple + "/8.3.0/include-fixed",
         triple + "/include"
      };

      for (auto&& cppStem : cppStems)
      {
         FilePath includePath = installPath.completeChildPath(baseDir + "/" + cppStem);
         cppIncludePaths.push_back(includePath.getAbsolutePath());
      }
   }
   else if (name == "4.2")
   {
      versionMin = "4.2.0";
      versionMax = "4.2.99";

      // PATH for utilities
      relativePathEntries.push_back("usr/bin");

      // set RTOOLS42_HOME
      std::string rtoolsPath = installPath.getAbsolutePath();
      std::replace(rtoolsPath.begin(), rtoolsPath.end(), '/', '\\');
      environmentVars.push_back({"RTOOLS42_HOME", rtoolsPath});

      // undefine _MSC_VER, so that we can "pretend" to be gcc
      // this is important for C++ libraries which might try to use
      // MSVC-specific tools when _MSC_VER is defined (e.g. Eigen), which might
      // not actually be defined or available in Rtools
      clangArgs.push_back("-U_MSC_VER");

      // set GNUC levels
      // (required for _mingw.h, which otherwise tries to use incompatible MSVC defines)
      clangArgs.push_back("-D__GNUC__=10");
      clangArgs.push_back("-D__GNUC_MINOR__=3");
      clangArgs.push_back("-D__GNUC_PATCHLEVEL__=0");

      // get C headers paths
      auto cStems = {
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include",
         "x86_64-w64-mingw32.static.posix/include",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include-fixed"
      };

      for (auto&& stem : cStems)
         cIncludePaths.push_back(installPath.completeChildPath(stem).getAbsolutePath());

      // get C++ headers
      auto cppStems = {
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include/c++",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include/c++/x86_64-w64-mingw32.static.posix",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include/c++/backward",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include",
         "x86_64-w64-mingw32.static.posix/include",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/10.3.0/include-fixed",
      };

      for (auto&& stem : cppStems)
         cppIncludePaths.push_back(installPath.completeChildPath(stem).getAbsolutePath());
   }
   else if (name == "4.3")
   {
      versionMin = "4.3.0";
      versionMax = "5.0.0";

      // PATH for utilities
      relativePathEntries.push_back("usr/bin");

      // set RTOOLS43_HOME
      std::string rtoolsPath = installPath.getAbsolutePath();
      std::replace(rtoolsPath.begin(), rtoolsPath.end(), '/', '\\');
      environmentVars.push_back({"RTOOLS43_HOME", rtoolsPath});

      // undefine _MSC_VER, so that we can "pretend" to be gcc
      // this is important for C++ libraries which might try to use
      // MSVC-specific tools when _MSC_VER is defined (e.g. Eigen), which might
      // not actually be defined or available in Rtools
      clangArgs.push_back("-U_MSC_VER");

      // set GNUC levels
      // (required for _mingw.h, which otherwise tries to use incompatible MSVC defines)
      clangArgs.push_back("-D__GNUC__=12");
      clangArgs.push_back("-D__GNUC_MINOR__=2");
      clangArgs.push_back("-D__GNUC_PATCHLEVEL__=0");

      // get C headers paths
      auto cStems = {
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include",
         "x86_64-w64-mingw32.static.posix/include",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include-fixed"
      };

      for (auto&& stem : cStems)
         cIncludePaths.push_back(installPath.completeChildPath(stem).getAbsolutePath());

      // get C++ headers
      auto cppStems = {
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include/c++",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include/c++/x86_64-w64-mingw32.static.posix",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include/c++/backward",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include",
         "x86_64-w64-mingw32.static.posix/include",
         "x86_64-w64-mingw32.static.posix/lib/gcc/x86_64-w64-mingw32.static.posix/12.2.0/include-fixed",
      };

      for (auto&& stem : cppStems)
         cppIncludePaths.push_back(installPath.completeChildPath(stem).getAbsolutePath());
   }
   else
   {
      LOG_DEBUG_MESSAGE("Unrecognized Rtools installation at path '" + installPath.getAbsolutePath() + "'");
   }

   // build version predicate and path list if we can
   if (!versionMin.empty())
   {
      boost::format fmt("getRversion() >= \"%1%\" && getRversion() <= \"%2%\"");
      versionPredicate_ = boost::str(fmt % versionMin % versionMax);

      for (const std::string& relativePath : relativePathEntries)
         pathEntries_.push_back(installPath_.completeChildPath(relativePath));

      // assign C clang arguments
      auto cClangArgs = clangArgs;
      for (auto&& cIncludePath : cIncludePaths)
         cClangArgs.push_back("-I" + cIncludePath);
      cClangArgs_ = cClangArgs;

      // assign C++ clang arguments
      auto cppClangArgs = clangArgs;
      for (auto&& cppIncludePath : cppIncludePaths)
         cppClangArgs.push_back("-I" + cppIncludePath);
      cppClangArgs_ = cppClangArgs;

      environmentVars_ = environmentVars;
   }
}

std::string RToolsInfo::url(const std::string& repos) const
{
   std::string url;

   if (name() == "4.3")
   {
      std::string suffix = "bin/windows/Rtools/rtools43/rtools.html";
      url = core::http::URL::complete(repos, suffix);
   }
   else if (name() == "4.2")
   {
      std::string suffix = "bin/windows/Rtools/rtools42/rtools.html";
      url = core::http::URL::complete(repos, suffix);
   }
   else if (name() == "4.0")
   {
      std::string arch = core::system::isWin64() ? "x86_64" : "i686";
      std::string suffix = "bin/windows/Rtools/rtools40-" + arch + ".exe";
      url = core::http::URL::complete(repos, suffix);
   }
   else
   {
      std::string version = boost::algorithm::replace_all_copy(name(), ".", "");
      std::string suffix = "bin/windows/Rtools/Rtools" + version + ".exe";
      url = core::http::URL::complete(repos, suffix);
   }

   return url;
}

std::ostream& operator<<(std::ostream& os, const RToolsInfo& info)
{
   os << "Rtools " << info.name() << std::endl;
   os << info.versionPredicate() << std::endl;
   for (const FilePath& pathEntry : info.pathEntries())
   {
     os << pathEntry << std::endl;
   }
   for (const core::system::Option& var : info.environmentVars())
   {
      os << var.first << "=" << var.second << std::endl;
   }

   return os;
}

namespace {

Error useRtools(const std::string& rToolsVersion,
                const std::string& rToolsHomeEnv,
                const std::string& rToolsDefaultPath,
                std::vector<RToolsInfo>* pRTools)
{
   FilePath installPath(rToolsDefaultPath);

   // if the associated environment variable is set, and
   // it points to an existing directory, use that instead
   std::string rToolsHome = core::system::getenv(rToolsHomeEnv);
   if (!rToolsHome.empty())
   {
      FilePath candidatePath(rToolsHome);
      if (candidatePath.exists())
         installPath = candidatePath;
   }

   // build info
   RToolsInfo toolsInfo(rToolsVersion, installPath, false);

   // check that recorded path is valid
   bool ok =
       toolsInfo.isStillInstalled() &&
       toolsInfo.isRecognized();

   // use it if all looks well
   if (ok)
      pRTools->push_back(toolsInfo);

   return Success();

}

Error scanEnvironmentForRTools(const std::string& rVersion,
                               std::vector<RToolsInfo>* pRTools)
{
   using core::Version;
   Version version(rVersion);

   if (version < Version("4.0.0"))
   {
      // older versions of Rtools didn't record this home path via
      // any environment variables, so nothing to do here
   }
   else if (version < Version("4.2.0"))
   {
      // use RTOOLS40_HOME
      useRtools("4.0", "RTOOLS40_HOME", "C:/rtools40", pRTools);
   }
   else if (version < Version("4.3.0"))
   {
      // use RTOOLS42_HOME
      useRtools("4.2", "RTOOLS42_HOME", "C:/rtools42", pRTools);
   }
   else if (version < Version("5.0.0"))
   {
      // use RTOOLS43_HOME
      useRtools("4.3", "RTOOLS43_HOME", "C:/rtools43", pRTools);
   }

   return Success();
}

Error scanRegistryForRTools(HKEY key,
                            bool usingMingwGcc49,
                            std::vector<RToolsInfo>* pRTools)
{
   core::system::RegistryKey regKey;
   Error error = regKey.open(key,
                             "Software\\R-core\\Rtools",
                             KEY_READ | KEY_WOW64_32KEY);
   if (error)
   {
      if (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
         return error;
      else
         return Success();
   }

   std::vector<std::string> keys = regKey.keyNames();
   for (size_t i = 0; i < keys.size(); i++)
   {
      std::string name = keys.at(i);
      core::system::RegistryKey verKey;
      error = verKey.open(regKey.handle(),
                          name,
                          KEY_READ | KEY_WOW64_32KEY);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      std::string installPath = verKey.getStringValue("InstallPath", "");
      if (!installPath.empty())
      {
         std::string utf8InstallPath = string_utils::systemToUtf8(installPath);
         RToolsInfo toolsInfo(name, FilePath(utf8InstallPath), usingMingwGcc49);
         if (toolsInfo.isStillInstalled())
         {
            if (toolsInfo.isRecognized())
               pRTools->push_back(toolsInfo);
            else
               LOG_WARNING_MESSAGE("Unknown Rtools version: " + name);
         }
      }
   }

   return Success();
}

void scanRegistryForRTools(bool usingMingwGcc49,
                           std::vector<RToolsInfo>* pRTools)
{
   // try HKLM first (backwards compatible with previous code)
   Error error = scanRegistryForRTools(
            HKEY_LOCAL_MACHINE,
            usingMingwGcc49,
            pRTools);

   if (error)
      LOG_ERROR(error);

   // try HKCU as a fallback
   if (pRTools->empty())
   {
      Error error = scanRegistryForRTools(
               HKEY_CURRENT_USER,
               usingMingwGcc49,
               pRTools);
      if (error)
         LOG_ERROR(error);
   }
}

void scanFoldersForRTools(bool usingMingwGcc49, std::vector<RToolsInfo>* pRTools)
{
   // look for Rtools as installed by RStudio
   std::string systemDrive = core::system::getenv("SYSTEMDRIVE");
   FilePath buildDirRoot(systemDrive + "/RBuildTools");

   // ensure it exists (may not exist if the user has not installed
   // any copies of Rtools through RStudio yet)
   if (!buildDirRoot.exists())
      return;

   // find sub-directories
   std::vector<FilePath> buildDirs;
   Error error = buildDirRoot.getChildren(buildDirs);
   if (error)
      LOG_ERROR(error);

   // infer Rtools information from each directory
   for (const FilePath& buildDir : buildDirs)
   {
      RToolsInfo toolsInfo(buildDir.getFilename(), buildDir, usingMingwGcc49);
      if (toolsInfo.isRecognized())
      {
         pRTools->push_back(toolsInfo);
         LOG_DEBUG_MESSAGE("Found Rtools: " + toolsInfo.installPath().getAbsolutePath());
      }
      else
         LOG_WARNING_MESSAGE("Unknown Rtools version: " + buildDir.getFilename());
   }
}

} // end anonymous namespace

void scanForRTools(bool usingMingwGcc49,
                   const std::string& rVersion,
                   std::vector<RToolsInfo>* pRTools)
{
   std::vector<RToolsInfo> rtoolsInfo;

   // scan for Rtools
   scanEnvironmentForRTools(rVersion, &rtoolsInfo);
   scanRegistryForRTools(usingMingwGcc49, &rtoolsInfo);
   scanFoldersForRTools(usingMingwGcc49, &rtoolsInfo);

   // remove duplicates
   std::set<FilePath> knownPaths;
   for (const RToolsInfo& info : rtoolsInfo)
   {
      if (knownPaths.count(info.installPath()))
         continue;
      LOG_DEBUG_MESSAGE(info.installPath().getAbsolutePath());
      knownPaths.insert(info.installPath());
      pRTools->push_back(info);
   }

   // ensure sorted by version
   std::sort(
            pRTools->begin(),
            pRTools->end(),
            [](const RToolsInfo& lhs, const RToolsInfo& rhs)
   {
      return Version(lhs.name()) < Version(rhs.name());
   });
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



