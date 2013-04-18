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
#include <core/StringUtils.hpp>

#include <core/system/RegistryKey.hpp>

#ifndef KEY_WOW64_32KEY
#define KEY_WOW64_32KEY 0x0200
#endif

namespace core {
namespace r_util {

namespace {

} // anonymous namespace


RToolsInfo::RToolsInfo(const std::string& name, const FilePath& installPath)
   : name_(name), installPath_(installPath)
{
   std::string versionMin, versionMax;
   std::vector<std::string> relativePathEntries;
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
      versionMax = "3.1.0";
      relativePathEntries.push_back("bin");
      relativePathEntries.push_back("gcc-4.6.3/bin");
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
   }
}

std::ostream& operator<<(std::ostream& os, const RToolsInfo& info)
{
   os << "Rtools " << info.name() << std::endl;
   os << info.versionPredicate() << std::endl;
   BOOST_FOREACH(const FilePath& pathEntry, info.pathEntries())
   {
     os << pathEntry << std::endl;
   }
   return os;
}

Error scanRegistryForRTools(std::vector<RToolsInfo>* pRTools)
{
   core::system::RegistryKey regKey;
   Error error = regKey.open(HKEY_LOCAL_MACHINE,
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
         RToolsInfo toolsInfo(name, FilePath(utf8InstallPath));
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


} // namespace r_util
} // namespace core 



