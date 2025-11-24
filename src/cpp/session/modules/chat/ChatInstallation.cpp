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
#include "ChatInternal.hpp"

#include <shared_core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

bool verifyPositAiInstallation(const FilePath& positAiPath)
{
   if (!positAiPath.exists())
      return false;

   FilePath clientDir = positAiPath.completeChildPath(kClientDirPath);
   FilePath serverScript = positAiPath.completeChildPath(kServerScriptPath);
   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);

   return clientDir.exists() && serverScript.exists() && indexHtml.exists();
}

FilePath locatePositAiInstallation()
{
   // 1. Check environment variable override (for development/testing)
   std::string rstudioPositAiPath = getenv("RSTUDIO_POSIT_AI_PATH");
   if (!rstudioPositAiPath.empty())
   {
      FilePath positAiPath(rstudioPositAiPath);
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
   FilePath userPositAiPath = xdg::userDataDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(userPositAiPath))
   {
      DLOG("Using user-level AI installation: {}", userPositAiPath.getAbsolutePath());
      return userPositAiPath;
   }

   // 3. Check system-wide installation (XDG config directory)
   // Linux: /etc/rstudio/ai
   // Windows: C:/ProgramData/RStudio/ai
   FilePath systemPositAiPath = xdg::systemConfigDir().completePath(kPositAiDirName);
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

   return FilePath(); // Not found
}

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio
