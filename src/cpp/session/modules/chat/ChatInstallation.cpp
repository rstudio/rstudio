/*
 * ChatInstallation.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatInstallation.hpp"
#include "ChatConstants.hpp"
#include "ChatLogging.hpp"

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <shared_core/json/Json.hpp>

// Use qualified names for core:: to avoid conflicts with system getenv
using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::logging;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace installation {

bool verifyPositAiInstallation(const core::FilePath& positAiPath)
{
   if (!positAiPath.exists())
      return false;

   core::FilePath clientDir = positAiPath.completeChildPath(kClientDirPath);
   core::FilePath serverScript = positAiPath.completeChildPath(kServerScriptPath);
   core::FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);

   return clientDir.exists() && serverScript.exists() && indexHtml.exists();
}

core::FilePath locatePositAiInstallation()
{
   // 1. Check environment variable override (for development/testing)
   std::string rstudioPositAiPath = core::system::getenv("RSTUDIO_POSIT_AI_PATH");
   if (!rstudioPositAiPath.empty())
   {
      core::FilePath positAiPath(rstudioPositAiPath);
      if (verifyPositAiInstallation(positAiPath))
      {
         DLOG("Using AI installation from RSTUDIO_POSIT_AI_PATH: {}", positAiPath.getAbsolutePath());
         return positAiPath;
      }
      else
      {
         WLOG("RSTUDIO_POSIT_AI_PATH set but installation invalid: {}", rstudioPositAiPath);
      }
   }

   // 2. Check user data directory (XDG-based, platform-appropriate)
   // Linux: ~/.local/share/rstudio/ai
   // macOS: ~/Library/Application Support/RStudio/ai
   // Windows: %LOCALAPPDATA%/RStudio/ai
   core::FilePath userPositAiPath = core::system::xdg::userDataDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(userPositAiPath))
   {
      DLOG("Using user-level AI installation: {}", userPositAiPath.getAbsolutePath());
      return userPositAiPath;
   }

   // 3. Check system-wide installation (XDG config directory)
   // Linux: /etc/rstudio/ai
   // Windows: C:/ProgramData/RStudio/ai
   core::FilePath systemPositAiPath = core::system::xdg::systemConfigDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(systemPositAiPath))
   {
      DLOG("Using system-wide AI installation: {}", systemPositAiPath.getAbsolutePath());
      return systemPositAiPath;
   }

   DLOG("No valid AI installation found. Checked locations:");
   if (!rstudioPositAiPath.empty())
      DLOG("  - RSTUDIO_POSIT_AI_PATH: {}", rstudioPositAiPath);
   DLOG("  - User data dir: {}", userPositAiPath.getAbsolutePath());
   DLOG("  - System config dir: {}", systemPositAiPath.getAbsolutePath());

   return core::FilePath(); // Not found
}

std::string getInstalledVersion()
{
   core::FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
      return "";

   core::FilePath packageJson = positAiPath.completeChildPath("package.json");
   if (!packageJson.exists())
   {
      WLOG("package.json not found in AI installation");
      return "";
   }

   // Read and parse package.json
   std::string content;
   core::Error error = core::readStringFromFile(packageJson, &content);
   if (error)
   {
      WLOG("Failed to read package.json: {}", error.getMessage());
      return "";
   }

   core::json::Value packageValue;
   if (packageValue.parse(content))
   {
      WLOG("Failed to parse package.json");
      return "";
   }

   if (!packageValue.isObject())
   {
      WLOG("package.json is not a JSON object");
      return "";
   }

   core::json::Object packageObj = packageValue.getObject();
   std::string version;
   error = core::json::readObject(packageObj, "version", version);
   if (error)
   {
      WLOG("package.json missing 'version' field");
      return "";
   }

   DLOG("Installed version: {}", version);
   return version;
}

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
