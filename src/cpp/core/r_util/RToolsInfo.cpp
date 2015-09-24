/*
 * RToolsInfo.cpp
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

#include <core/r_util/RToolsInfo.hpp>

#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <core/http/URL.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Types.hpp>

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
   std::string path = filePath.absolutePath();
   boost::algorithm::replace_all(path, "\\", "/");
   if (!boost::algorithm::ends_with(path, "/"))
      path += "/";
   return path;
}

} // anonymous namespace


RToolsInfo::RToolsInfo(const std::string& name,
                       const FilePath& installPath,
                       bool usingMingwGcc49)
   : name_(name), installPath_(installPath)
{
   std::string versionMin, versionMax;
   std::vector<std::string> relativePathEntries;
   std::vector<core::system::Option> environmentVars;
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
   }
   else if (name == "2.16" || name == "3.0")
   {
      versionMin = "2.15.2";
      versionMax = "3.0.99";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
   }
   else if (name == "3.1")
   {
      versionMin = "3.0.0";
      versionMax = "3.1.99";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
   }
   else if (name == "3.2")
   {
      versionMin = "3.1.0";
      versionMax = "3.2.0";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
   }
   else if (name == "3.3" && !usingMingwGcc49)
   {
      versionMin = "3.2.0";
      versionMax = "3.3.0";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
   }
   else if (name == "3.3" && usingMingwGcc49)
   {
      // confirm that this Rtools has the new gcc 4.9 toolchain
      if (installPath_.childPath("mingw_32").exists())
      {
         versionMin = "3.3.0";
         versionMax = "3.3.99";
         relativePathEntries.push_back("bin");

         // set environment variables
         // TODO: work out which ones to actually set
         // TODO: figure out how these propagate into libclang compliation db
         FilePath gccPath = installPath_.childPath("mingw_$(WIN)/bin");
         environmentVars.push_back(
               std::make_pair("BINPREF", asRBuildPath(gccPath)));
      }
      else
      {
         // use versions that will never be satisfied (so this
         // Rtools is never used with this version of R)
         versionMin = "1.0.0";
         versionMax = "1.0.0";
      }
   }

   // build version predicate and path list if we can
   if (!versionMin.empty())
   {
      boost::format fmt("getRversion() >= \"%1%\" && getRversion() <= \"%2%\"");
      versionPredicate_ = boost::str(fmt % versionMin % versionMax);

      BOOST_FOREACH(const std::string& relativePath, relativePathEntries)
      {
         pathEntries_.push_back(installPath_.childPath(relativePath));
      }

      environmentVars_ = environmentVars;
   }
}

std::string RToolsInfo::url(const std::string& repos) const
{
   // strip period from name
   std::string ver = boost::algorithm::replace_all_copy(name(), ".", "");
   std::string url = core::http::URL::complete(
                        repos, "bin/windows/Rtools/Rtools" + ver + ".exe");
   return url;
}

std::ostream& operator<<(std::ostream& os, const RToolsInfo& info)
{
   os << "Rtools " << info.name() << std::endl;
   os << info.versionPredicate() << std::endl;
   BOOST_FOREACH(const FilePath& pathEntry, info.pathEntries())
   {
     os << pathEntry << std::endl;
   }
   BOOST_FOREACH(const core::system::Option& var, info.environmentVars())
   {
      os << var.first << "=" << var.second << std::endl;
   }

   return os;
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
      if (error.code() != boost::system::errc::no_such_file_or_directory)
         return error;
      else
         return Success();
   }

   std::vector<std::string> keys = regKey.keyNames();
   for (int i = 0; i < keys.size(); i++)
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

Error scanRegistryForRTools(bool usingMingwGcc49,
                            std::vector<RToolsInfo>* pRTools)
{
   // try HKLM first (backwards compatible with previous code)
   Error error = scanRegistryForRTools(HKEY_LOCAL_MACHINE,
                                       usingMingwGcc49,
                                       pRTools);
   if (error)
      return error;

   // try HKCU as a fallback
   if (pRTools->empty())
      return scanRegistryForRTools(HKEY_CURRENT_USER,
                                   usingMingwGcc49,
                                   pRTools);
   else
      return Success();
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



