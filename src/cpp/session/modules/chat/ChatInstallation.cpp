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
#include "ChatSelector.hpp"
#include "ChatSlots.hpp"

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

namespace {

// The installation this session runs, resolved on first use. See
// locatePositAssistantInstallation() for why it is held rather than recomputed.
bool s_pinned = false;
core::FilePath s_pinnedInstall;

// Reads one string field out of a JSON file in an installation directory.
// Absent, unparseable, wrong-typed and empty all read as an empty string:
// every caller treats them the same way, and none can do anything about the
// difference.
std::string readJsonStringField(const core::FilePath& installDir,
                                const char* fileName,
                                const char* fieldName)
{
   core::FilePath filePath = installDir.completeChildPath(fileName);
   if (!filePath.isRegularFile())
      return std::string();

   std::string content;
   core::Error error = core::readStringFromFile(filePath, &content);
   if (error)
   {
      WLOG("Failed to read {}: {}", filePath.getAbsolutePath(), error.getMessage());
      return std::string();
   }

   core::json::Value value;
   if (value.parse(content) || !value.isObject())
   {
      DLOG("{} is not a JSON object", filePath.getAbsolutePath());
      return std::string();
   }

   core::json::Object object = value.getObject();
   if (!object.hasMember(fieldName) || !object[fieldName].isString())
      return std::string();

   return object[fieldName].getString();
}

bool existsAndNonEmpty(const core::FilePath& filePath)
{
   return filePath.isRegularFile() && filePath.getSize() > 0;
}

// The system-wide install, which an administrator manages by hand. It is
// deliberately unversioned: nothing RStudio does ever writes here.
core::FilePath systemInstallDir()
{
   return core::system::xdg::systemConfigDir().completePath(kPositAiDirName);
}

// Source 1: the development override.
bool resolveFromEnvironment(core::FilePath* pInstallDir)
{
   std::string overridePath = core::system::getenv("RSTUDIO_POSIT_AI_PATH");
   if (overridePath.empty())
      return false;

   core::FilePath installDir(overridePath);
   if (!verifyInstallDir(installDir))
   {
      WLOG("RSTUDIO_POSIT_AI_PATH set but installation invalid: {}", overridePath);
      return false;
   }

   DLOG("Using Posit Assistant from RSTUDIO_POSIT_AI_PATH: {}",
        installDir.getAbsolutePath());
   *pInstallDir = installDir;
   return true;
}

// Source 2: the per-user version slots, chosen by protocol. The only source
// an install writes to.
bool resolveFromUserSlots(core::FilePath* pInstallDir)
{
   core::FilePath slotDir =
      selector::resolveSlot(positAiStorageDir(), kProtocolVersion);
   if (slotDir.isEmpty())
      return false;

   DLOG("Using Posit Assistant install slot: {}", slotDir.getAbsolutePath());
   *pInstallDir = slotDir;
   return true;
}

// Source 3: the admin-managed system install.
bool resolveFromSystemInstall(core::FilePath* pInstallDir)
{
   core::FilePath installDir = systemInstallDir();
   if (!verifyInstallDir(installDir))
      return false;

   DLOG("Using system-wide Posit Assistant install: {}",
        installDir.getAbsolutePath());
   *pInstallDir = installDir;
   return true;
}

// Each source is self-contained so the list can be changed without disturbing
// the others: #18443 disables the user-writable sources in admin-managed
// deployments, and the Workbench bundling work appends a fourth.
core::FilePath resolveInstallation()
{
   core::FilePath installDir;

   if (resolveFromEnvironment(&installDir))
      return installDir;

   if (resolveFromUserSlots(&installDir))
      return installDir;

   if (resolveFromSystemInstall(&installDir))
      return installDir;

   DLOG("No Posit Assistant installation found. Checked: RSTUDIO_POSIT_AI_PATH, "
        "{} (protocol {}), {}",
        slots::versionsDir(positAiStorageDir()).getAbsolutePath(),
        kProtocolVersion,
        systemInstallDir().getAbsolutePath());

   return core::FilePath();
}

} // anonymous namespace

core::FilePath positAiStorageDir()
{
   return core::system::xdg::userDataDir().completePath(kPositAiStorageDirName);
}

bool verifyInstallDir(const core::FilePath& installDir)
{
   if (!installDir.isDirectory())
      return false;

   core::FilePath clientDir = installDir.completeChildPath(kClientDirPath);
   if (!clientDir.isDirectory())
      return false;

   return existsAndNonEmpty(installDir.completeChildPath(kServerScriptPath)) &&
          existsAndNonEmpty(clientDir.completeChildPath(kIndexFileName));
}

std::string declaredVersion(const core::FilePath& installDir)
{
   return readJsonStringField(installDir, kPackageJsonFileName, "version");
}

std::string declaredProtocol(const core::FilePath& installDir)
{
   return readJsonStringField(installDir, kProtocolVersionFileName, "protocol");
}

core::FilePath locatePositAssistantInstallation()
{
   if (!s_pinned)
   {
      s_pinnedInstall = resolveInstallation();
      s_pinned = true;
   }

   return s_pinnedInstall;
}

void clearPinnedInstallation()
{
   s_pinned = false;
   s_pinnedInstall = core::FilePath();
}

std::string getInstalledVersion()
{
   core::FilePath installDir = locatePositAssistantInstallation();
   if (installDir.isEmpty())
      return std::string();

   std::string version = declaredVersion(installDir);
   if (version.empty())
      WLOG("No package version in {}", installDir.getAbsolutePath());

   return version;
}

std::string getInstalledProtocolVersion()
{
   core::FilePath installDir = locatePositAssistantInstallation();
   if (installDir.isEmpty())
      return std::string();

   return declaredProtocol(installDir);
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
