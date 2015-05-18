/*
 * RSessionContext.cpp
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

#include <core/r_util/RSessionContext.hpp>

#include <iostream>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/regex.hpp>

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Util.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RProjectFile.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {


} // anonymous namespace


UserDirectories userDirectories(SessionType sessionType,
                                const std::string& homePath)
{
   UserDirectories dirs;
   if (!homePath.empty())
      dirs.homePath = homePath;
   else
      dirs.homePath = core::system::userHomePath("R_USER|HOME").absolutePath();

   // compute user scratch path
   std::string scratchPathName;
   if (sessionType == SessionTypeDesktop)
      scratchPathName = "RStudio-Desktop";
   else
      scratchPathName = "RStudio";

   dirs.scratchPath = core::system::userSettingsPath(
                                          FilePath(dirs.homePath),
                                          scratchPathName).absolutePath();

   return dirs;
}

FilePath projectsSettingsPath(const FilePath& userScratchPath)
{
   FilePath settingsPath = userScratchPath.complete(kProjectsSettings);
   Error error = settingsPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   return settingsPath;
}

std::string readProjectsSetting(const FilePath& settingsPath,
                                const char * const settingName)
{
   FilePath readPath = settingsPath.complete(settingName);
   if (readPath.exists())
   {
      std::string value;
      Error error = core::readStringFromFile(readPath, &value);
      if (error)
      {
         LOG_ERROR(error);
         return std::string();
      }
      boost::algorithm::trim(value);
      return value;
   }
   else
   {
      return std::string();
   }
}

void writeProjectsSetting(const FilePath& settingsPath,
                          const char * const settingName,
                          const std::string& value)
{
   FilePath writePath = settingsPath.complete(settingName);
   Error error = core::writeStringToFile(writePath, value);
   if (error)
      LOG_ERROR(error);
}

namespace {

const char* const kSessionContextDelimeter = ":::";

} // anonymous namespace


std::ostream& operator<< (std::ostream& os, const SessionContext& context)
{
   os << context.username;
   if (!context.scope.project.empty())
      os << " -- " << context.scope.project;
   if (!context.scope.id.empty())
      os << " [" << context.scope.id << "]";
   return os;
}


std::string sessionContextToStreamFile(const SessionContext& context)
{
   std::string streamFile = context.username;
   if (!context.scope.project.empty())
      streamFile += kSessionContextDelimeter + context.scope.project;
   if (!context.scope.id.empty())
      streamFile += kSessionContextDelimeter + context.scope.id;
   return http::util::urlEncode(streamFile);
}

SessionContext streamFileToSessionContext(const std::string& file)
{
   std::vector<std::string> result;
   boost::algorithm::split_regex(result,
                                 http::util::urlDecode(file),
                                 boost::regex("\\:\\:\\:"));
   SessionContext context = SessionContext(result[0]);
   if (result.size() > 1)
      context.scope.project = result[1];
   if (result.size() > 2)
      context.scope.id = result[2];
   return context;
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



