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
   // next to bin/ rather than inside it. The bin candidate is verified rather
   // than merely tested for existence, so a partial directory left there does
   // not mask a usable bundle at the other location.
   core::FilePath binPath =
      resourcePath.completePath("bin").completePath(kBundledPositAiDirName);
   if (verifyPositAiInstallation(binPath))
      return binPath;

   return resourcePath.completePath(kBundledPositAiDirName);
}

core::FilePath bundledPositAssistantInstallPath()
{
   return bundledPositAssistantInstallPath(options().resourcePath());
}

InstallSearchPaths positAssistantSearchPaths()
{
   InstallSearchPaths paths;
   paths.userDataPath = core::system::xdg::userDataDir().completePath(kPositAiDirName);
   paths.systemPath = systemPositAssistantInstallPath();
   paths.bundledPath = bundledPositAssistantInstallPath();
   paths.pinnedSystemPath = !options().positAssistantPath().isEmpty();
   return paths;
}

core::FilePath locatePositAssistantInstallation(const InstallSearchPaths& paths)
{
   // 1. Check user data directory (XDG-based, platform-appropriate)
   // Linux/macOS: ~/.local/share/rstudio/pai/bin
   // Windows: %LOCALAPPDATA%/rstudio/pai/bin
   if (verifyPositAiInstallation(paths.userDataPath))
   {
      DLOG("Using user-level AI installation: {}", paths.userDataPath.getAbsolutePath());
      return paths.userDataPath;
   }

   // 2. Check the system-wide installation: posit-assistant-path when set, and
   // otherwise the XDG config directory (/etc/rstudio/pai/bin on Linux and
   // macOS, C:/ProgramData/rstudio/pai/bin on Windows)
   if (verifyPositAiInstallation(paths.systemPath))
   {
      DLOG("Using system-wide AI installation: {}", paths.systemPath.getAbsolutePath());
      return paths.systemPath;
   }

   // A path the administrator pinned but that holds no installation ends the
   // search: falling through to the bundled copy would answer a typo or an
   // unmounted share with a silent downgrade to whatever version shipped
   // with RStudio.
   if (paths.pinnedSystemPath)
   {
      // Warn once per session: locate() runs on every status, verify, and chat
      // request, and a misconfigured path would otherwise flood the log.
      if (RS_ONCE())
         WLOG("posit-assistant-path set but installation invalid: {}",
              paths.systemPath.getAbsolutePath());
   }
   else
   {
      // 3. Check the copy bundled with RStudio. It ranks last: a
      // manifest-installed update lands in the user data directory and an
      // administrator's own install is deliberate, so both outrank it.
      // Open-source builds ship no bundle and always fall through here.
      if (verifyPositAiInstallation(paths.bundledPath))
      {
         DLOG("Using AI installation bundled with RStudio: {}",
              paths.bundledPath.getAbsolutePath());
         return paths.bundledPath;
      }
   }

   DLOG("No valid AI installation found. Checked locations:");
   DLOG("  - User data dir: {}", paths.userDataPath.getAbsolutePath());
   DLOG("  - System install dir: {}", paths.systemPath.getAbsolutePath());
   if (!paths.pinnedSystemPath)
      DLOG("  - Bundled with RStudio: {}", paths.bundledPath.getAbsolutePath());

   return core::FilePath(); // Not found
}

core::FilePath locatePositAssistantInstallation()
{
   return locatePositAssistantInstallation(positAssistantSearchPaths());
}

std::string getInstalledVersion(const core::FilePath& positAiPath)
{
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

std::string getInstalledVersion()
{
   return getInstalledVersion(locatePositAssistantInstallation());
}

std::string getInstalledProtocolVersion(const core::FilePath& positAiPath)
{
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

std::string getInstalledProtocolVersion()
{
   return getInstalledProtocolVersion(locatePositAssistantInstallation());
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
