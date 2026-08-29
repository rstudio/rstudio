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
#include <core/Macros.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <session/SessionOptions.hpp>
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

core::FilePath systemPositAssistantInstallPath()
{
   // An administrator may install Posit Assistant outside the XDG config
   // directory; when posit-assistant-path is set it replaces that location
   // rather than adding another one to search.
   core::FilePath configuredPath = options().positAssistantPath();
   if (!configuredPath.isEmpty())
      return configuredPath;

   return core::system::xdg::systemConfigDir().completePath(kPositAiDirName);
}

core::FilePath bundledPositAssistantInstallPath(const core::FilePath& resourcePath)
{
   // Mirrors the Copilot Language Server layout: the directory is installed
   // beside the session binary, except in the macOS app bundle where it sits
   // next to bin/ rather than inside it.
   core::FilePath binPath =
      resourcePath.completePath("bin").completePath(kBundledPositAiDirName);
   if (binPath.exists())
      return binPath;

   return resourcePath.completePath(kBundledPositAiDirName);
}

core::FilePath bundledPositAssistantInstallPath()
{
   return bundledPositAssistantInstallPath(options().resourcePath());
}

core::FilePath locatePositAssistantInstallation()
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
   // Linux/macOS: ~/.local/share/rstudio/pai/bin
   // Windows: %LOCALAPPDATA%/rstudio/pai/bin
   core::FilePath userPositAiPath = core::system::xdg::userDataDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(userPositAiPath))
   {
      DLOG("Using user-level AI installation: {}", userPositAiPath.getAbsolutePath());
      return userPositAiPath;
   }

   // 3. Check the system-wide installation: posit-assistant-path when set, and
   // otherwise the XDG config directory (/etc/rstudio/pai/bin on Linux and
   // macOS, C:/ProgramData/rstudio/pai/bin on Windows)
   core::FilePath systemPositAiPath = systemPositAssistantInstallPath();
   if (verifyPositAiInstallation(systemPositAiPath))
   {
      DLOG("Using system-wide AI installation: {}", systemPositAiPath.getAbsolutePath());
      return systemPositAiPath;
   }

   // A path the administrator pinned but that holds no installation ends the
   // search: falling through to the bundled copy would answer a typo or an
   // unmounted share with a silent downgrade to whatever version shipped
   // with RStudio.
   bool pinnedInstall = !options().positAssistantPath().isEmpty();
   if (pinnedInstall)
   {
      // Warn once per session: locate() runs on every status, verify, and chat
      // request, and a misconfigured path would otherwise flood the log.
      if (RS_ONCE())
         WLOG("posit-assistant-path set but installation invalid: {}", systemPositAiPath.getAbsolutePath());
   }
   else
   {
      // 4. Check the copy bundled with RStudio. It ranks last: a
      // manifest-installed update lands in the user data directory and an
      // administrator's own install is deliberate, so both outrank it.
      // Open-source builds ship no bundle and always fall through here.
      core::FilePath bundledPositAiPath = bundledPositAssistantInstallPath();
      if (verifyPositAiInstallation(bundledPositAiPath))
      {
         DLOG("Using AI installation bundled with RStudio: {}", bundledPositAiPath.getAbsolutePath());
         return bundledPositAiPath;
      }
   }

   DLOG("No valid AI installation found. Checked locations:");
   if (!rstudioPositAiPath.empty())
      DLOG("  - RSTUDIO_POSIT_AI_PATH: {}", rstudioPositAiPath);
   DLOG("  - User data dir: {}", userPositAiPath.getAbsolutePath());
   DLOG("  - System install dir: {}", systemPositAiPath.getAbsolutePath());
   if (!pinnedInstall)
      DLOG("  - Bundled with RStudio: {}", bundledPositAssistantInstallPath().getAbsolutePath());

   return core::FilePath(); // Not found
}

std::string getInstalledVersion()
{
   core::FilePath positAiPath = locatePositAssistantInstallation();
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

std::string getInstalledProtocolVersion()
{
   core::FilePath positAiPath = locatePositAssistantInstallation();
   if (positAiPath.isEmpty())
      return "";

   core::FilePath protoFile =
      positAiPath.completeChildPath(kProtocolVersionFileName);
   if (!protoFile.exists())
   {
      DLOG("No protocol.json found (legacy install)");
      return "";
   }

   std::string content;
   core::Error error = core::readStringFromFile(protoFile, &content);
   if (error)
   {
      ELOG("Failed to read protocol.json: {}", error.getMessage());
      return "";
   }

   core::json::Value jsonValue;
   if (jsonValue.parse(content))
   {
      ELOG("Failed to parse protocol.json");
      return "";
   }

   if (!jsonValue.isObject())
   {
      ELOG("protocol.json is not a JSON object");
      return "";
   }

   core::json::Object obj = jsonValue.getObject();
   if (!obj.hasMember("protocol") ||
       !obj["protocol"].isString())
   {
      ELOG("protocol.json missing \"protocol\" string field");
      return "";
   }

   std::string version = obj["protocol"].getString();
   DLOG("Installed protocol version: {}", version);
   return version;
}

core::Error writeProtocolVersionFileIfMissing(const core::FilePath& positAiPath)
{
   core::FilePath protoFile =
      positAiPath.completeChildPath(kProtocolVersionFileName);

   // Newer packages bundle their own protocol.json; preserve it so we record
   // the protocol the package actually declares rather than RStudio's default.
   if (protoFile.exists())
   {
      DLOG("protocol.json already present; leaving package-provided file intact");
      return core::Success();
   }

   core::json::Object protoJson;
   protoJson["protocol"] = kProtocolVersion;
   return core::writeStringToFile(protoFile, protoJson.write());
}

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
