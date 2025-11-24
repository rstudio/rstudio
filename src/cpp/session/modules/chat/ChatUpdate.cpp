/*
 * ChatUpdate.cpp
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

#include "ChatUpdate.hpp"
#include "ChatInternal.hpp"
#include "ChatInstallation.hpp"

#include <vector>

#include <boost/algorithm/string.hpp>
#include <boost/thread/mutex.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

namespace {

// ============================================================================
// Update State
// ============================================================================

struct UpdateState
{
   bool updateAvailable;
   std::string currentVersion;
   std::string newVersion;
   std::string downloadUrl;
   std::string errorMessage;

   enum class Status
   {
      Idle,
      Downloading,
      Installing,
      Complete,
      Error
   };
   Status installStatus;
   std::string installMessage;

   UpdateState()
      : updateAvailable(false),
        installStatus(Status::Idle)
   {
   }
};

UpdateState s_updateState;
boost::mutex s_updateStateMutex;

// ============================================================================
// Helper Functions
// ============================================================================

bool isHttpsUrl(const std::string& url)
{
   return boost::starts_with(url, "https://");
}

Error downloadManifest(json::Object* pManifest)
{
   if (!pManifest)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   std::string downloadUri = prefs::userPrefs().paiDownloadUri();
   boost::algorithm::trim(downloadUri);

   if (downloadUri.empty())
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "pai_download_uri preference not set",
                        ERROR_LOCATION);
   }

   if (!isHttpsUrl(downloadUri))
   {
      WLOG("Manifest download URL must use HTTPS, rejecting: {}", downloadUri);
      return systemError(boost::system::errc::protocol_error,
                        "Manifest URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   DLOG("Downloading manifest from: {}", downloadUri);

   FilePath tempFile = module_context::tempFile("manifest", "json");

   r::exec::RFunction downloadFunc("download.file");
   downloadFunc.addParam("url", downloadUri);
   downloadFunc.addParam("destfile", tempFile.getAbsolutePath());
   downloadFunc.addParam("quiet", true);
   downloadFunc.addParam("method", "libcurl");
   downloadFunc.addParam("timeout", 30);

   Error error = downloadFunc.call();
   if (error)
   {
      WLOG("Failed to download manifest: {}", error.getMessage());
      return error;
   }

   std::string manifestContent;
   error = core::readStringFromFile(tempFile, &manifestContent);
   if (error)
   {
      WLOG("Failed to read manifest file: {}", error.getMessage());
      return error;
   }

   json::Value manifestValue;
   if (manifestValue.parse(manifestContent))
   {
      WLOG("Failed to parse manifest JSON");
      return systemError(boost::system::errc::protocol_error,
                        "Invalid JSON in manifest",
                        ERROR_LOCATION);
   }

   if (!manifestValue.isObject())
   {
      return systemError(boost::system::errc::protocol_error,
                        "Manifest must be a JSON object",
                        ERROR_LOCATION);
   }

   *pManifest = manifestValue.getObject();
   DLOG("Successfully downloaded and parsed manifest");

   return Success();
}

Error getPackageInfoFromManifest(
    const json::Object& manifest,
    const std::string& protocolVersion,
    std::string* pPackageVersion,
    std::string* pDownloadUrl)
{
   if (!pPackageVersion || !pDownloadUrl)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   json::Object versions;
   Error error = json::readObject(manifest, "versions", versions);
   if (error)
   {
      WLOG("Manifest missing 'versions' field");
      return error;
   }

   auto it = versions.find(protocolVersion);
   if (it == versions.end())
   {
      WLOG("Manifest does not contain entry for protocol version: {}", protocolVersion);
      return systemError(boost::system::errc::protocol_not_supported,
                        "Protocol version not found in manifest",
                        ERROR_LOCATION);
   }

   json::Object::Member member = *it;
   json::Value versionValue = member.getValue();

   if (!versionValue.isObject())
   {
      return systemError(boost::system::errc::protocol_error,
                        "Protocol version entry must be an object",
                        ERROR_LOCATION);
   }

   json::Object versionInfo = versionValue.getObject();

   std::string version;
   std::string url;
   error = json::readObject(versionInfo, "version", version);
   if (error)
   {
      WLOG("Version info missing 'version' field");
      return error;
   }

   error = json::readObject(versionInfo, "url", url);
   if (error)
   {
      WLOG("Version info missing 'url' field");
      return error;
   }

   if (!isHttpsUrl(url))
   {
      WLOG("Package download URL must use HTTPS, rejecting: {}", url);
      return systemError(boost::system::errc::protocol_error,
                        "Package download URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   *pPackageVersion = version;
   *pDownloadUrl = url;

   DLOG("Found package info: version={}, url={}", version, url);

   return Success();
}

std::string getInstalledVersion()
{
   FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
      return "";

   FilePath packageJson = positAiPath.completeChildPath("package.json");
   if (!packageJson.exists())
   {
      WLOG("package.json not found in AI installation");
      return "";
   }

   std::string content;
   Error error = core::readStringFromFile(packageJson, &content);
   if (error)
   {
      WLOG("Failed to read package.json: {}", error.getMessage());
      return "";
   }

   json::Value packageValue;
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

   json::Object packageObj = packageValue.getObject();
   std::string version;
   error = json::readObject(packageObj, "version", version);
   if (error)
   {
      WLOG("package.json missing 'version' field");
      return "";
   }

   DLOG("Installed version: {}", version);
   return version;
}

struct SemanticVersion
{
   int major;
   int minor;
   int patch;

   SemanticVersion() : major(0), minor(0), patch(0) {}

   bool parse(const std::string& versionStr)
   {
      std::string cleanVersion = versionStr;
      if (!cleanVersion.empty() && cleanVersion[0] == 'v')
         cleanVersion = cleanVersion.substr(1);

      std::vector<std::string> parts;
      boost::split(parts, cleanVersion, boost::is_any_of("."));

      if (parts.size() < 1)
         return false;

      major = safe_convert::stringTo<int>(parts[0], -1);
      if (major < 0)
         return false;

      if (parts.size() >= 2)
      {
         minor = safe_convert::stringTo<int>(parts[1], -1);
         if (minor < 0)
            return false;
      }

      if (parts.size() >= 3)
      {
         patch = safe_convert::stringTo<int>(parts[2], -1);
         if (patch < 0)
            return false;
      }

      return true;
   }

   bool operator>(const SemanticVersion& other) const
   {
      if (major != other.major)
         return major > other.major;
      if (minor != other.minor)
         return minor > other.minor;
      return patch > other.patch;
   }
};

bool isNewerVersionAvailable(
    const std::string& installedVersion,
    const std::string& availableVersion)
{
   SemanticVersion installed, available;

   if (!installed.parse(installedVersion))
   {
      WLOG("Failed to parse installed version: {}", installedVersion);
      return false;
   }

   if (!available.parse(availableVersion))
   {
      WLOG("Failed to parse available version: {}", availableVersion);
      return false;
   }

   return available > installed;
}

Error downloadPackage(const std::string& url, const FilePath& destPath)
{
   if (!isHttpsUrl(url))
   {
      WLOG("Package download URL must use HTTPS, rejecting: {}", url);
      return systemError(boost::system::errc::protocol_error,
                        "Package download URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   DLOG("Downloading package from: {} to: {}", url, destPath.getAbsolutePath());

   r::exec::RFunction downloadFunc("download.file");
   downloadFunc.addParam("url", url);
   downloadFunc.addParam("destfile", destPath.getAbsolutePath());
   downloadFunc.addParam("quiet", false);
   downloadFunc.addParam("method", "libcurl");
   downloadFunc.addParam("mode", "wb");
   downloadFunc.addParam("timeout", 60);

   Error error = downloadFunc.call();
   if (error)
   {
      WLOG("Failed to download package: {}", error.getMessage());
      return error;
   }

   if (!destPath.exists() || destPath.getSize() == 0)
   {
      return systemError(boost::system::errc::io_error,
                        "Downloaded file is empty or missing",
                        ERROR_LOCATION);
   }

   DLOG("Successfully downloaded package ({} bytes)", destPath.getSize());
   return Success();
}

Error installPackage(const FilePath& packagePath)
{
   FilePath userDataDir = xdg::userDataDir();
   FilePath aiDir = userDataDir.completePath(kPositAiDirName);
   FilePath aiPrevDir = userDataDir.completePath("ai.prev");

   DLOG("Installing package from: {}", packagePath.getAbsolutePath());

   // Step 1: Remove old backup if it exists
   if (aiPrevDir.exists())
   {
      DLOG("Removing old backup directory: {}", aiPrevDir.getAbsolutePath());
      Error error = aiPrevDir.removeIfExists();
      if (error)
      {
         WLOG("Failed to remove old backup: {}", error.getMessage());
         return error;
      }
   }

   // Step 2: Backup current installation if it exists
   if (aiDir.exists())
   {
      DLOG("Backing up current installation to: {}", aiPrevDir.getAbsolutePath());
      Error error = aiDir.move(aiPrevDir);
      if (error)
      {
         WLOG("Failed to backup current installation: {}", error.getMessage());
         return error;
      }
   }

   // Step 3: Create new ai directory
   Error error = aiDir.ensureDirectory();
   if (error)
   {
      WLOG("Failed to create ai directory: {}", error.getMessage());
      if (aiPrevDir.exists())
      {
         aiPrevDir.move(aiDir);
      }
      return error;
   }

   // Step 4: Extract package using R's unzip()
   DLOG("Extracting package to: {}", aiDir.getAbsolutePath());
   r::exec::RFunction unzipFunc("unzip");
   unzipFunc.addParam("zipfile", packagePath.getAbsolutePath());
   unzipFunc.addParam("exdir", aiDir.getAbsolutePath());

   error = unzipFunc.call();
   if (error)
   {
      WLOG("Failed to extract package: {}", error.getMessage());
      Error cleanupError = aiDir.removeIfExists();
      if (cleanupError)
      {
         ELOG("Failed to clean up failed extraction directory: {}", cleanupError.getMessage());
      }
      if (aiPrevDir.exists())
      {
         Error restoreError = aiPrevDir.move(aiDir);
         if (restoreError)
         {
            ELOG("Failed to restore backup after extraction failure: {}", restoreError.getMessage());
         }
      }
      return error;
   }

   // Step 5: Verify installation
   if (!verifyPositAiInstallation(aiDir))
   {
      WLOG("Extracted package failed verification");
      Error cleanupError = aiDir.removeIfExists();
      if (cleanupError)
      {
         ELOG("Failed to clean up invalid extraction directory: {}", cleanupError.getMessage());
      }
      if (aiPrevDir.exists())
      {
         Error restoreError = aiPrevDir.move(aiDir);
         if (restoreError)
         {
            ELOG("Failed to restore backup after verification failure: {}", restoreError.getMessage());
         }
      }
      return systemError(boost::system::errc::io_error,
                        "Extracted package is incomplete or invalid",
                        ERROR_LOCATION);
   }

   // Step 6: Success - remove backup
   if (aiPrevDir.exists())
   {
      DLOG("Installation successful, removing backup");
      Error backupCleanup = aiPrevDir.removeIfExists();
      if (backupCleanup)
      {
         WLOG("Failed to remove backup directory after successful install: {}", backupCleanup.getMessage());
      }
   }

   DLOG("Package installation complete");
   return Success();
}

} // end anonymous namespace

// ============================================================================
// Public API
// ============================================================================

Error checkForUpdatesOnStartup()
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      DLOG("Update check skipped: pai preferences not configured");
      return Success();
   }

   DLOG("Checking for updates on startup");

   std::string installedVersion = getInstalledVersion();
   if (installedVersion.empty())
   {
      DLOG("No installation found, checking for initial install");
      installedVersion = "0.0.0";
   }

   s_updateState.currentVersion = installedVersion;

   json::Object manifest;
   Error error = downloadManifest(&manifest);
   if (error)
   {
      WLOG("Failed to download manifest: {}", error.getMessage());
      return Success();
   }

   std::string packageVersion;
   std::string downloadUrl;
   error = getPackageInfoFromManifest(manifest, kProtocolVersion, &packageVersion, &downloadUrl);
   if (error)
   {
      WLOG("Failed to parse manifest: {}", error.getMessage());
      return Success();
   }

   if (isNewerVersionAvailable(installedVersion, packageVersion))
   {
      DLOG("Update available: {} -> {}", installedVersion, packageVersion);
      s_updateState.updateAvailable = true;
      s_updateState.newVersion = packageVersion;
      s_updateState.downloadUrl = downloadUrl;
   }
   else
   {
      DLOG("No update available (installed: {}, available: {})", installedVersion, packageVersion);
      s_updateState.updateAvailable = false;
   }

   return Success();
}

Error chatCheckForUpdates(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      json::Object result;
      result["updateAvailable"] = false;
      pResponse->setResult(result);
      return Success();
   }

   json::Object result;
   result["updateAvailable"] = s_updateState.updateAvailable;
   result["currentVersion"] = s_updateState.currentVersion;
   result["newVersion"] = s_updateState.newVersion;
   result["downloadUrl"] = s_updateState.downloadUrl;
   result["isInitialInstall"] = (s_updateState.currentVersion == "0.0.0");

   pResponse->setResult(result);
   return Success();
}

Error chatInstallUpdate(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "Feature not enabled",
                        ERROR_LOCATION);
   }

   if (!s_updateState.updateAvailable)
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "No update available",
                        ERROR_LOCATION);
   }

   if (s_updateState.installStatus != UpdateState::Status::Idle &&
       s_updateState.installStatus != UpdateState::Status::Complete &&
       s_updateState.installStatus != UpdateState::Status::Error)
   {
      return systemError(boost::system::errc::operation_in_progress,
                        "Update already in progress",
                        ERROR_LOCATION);
   }

   s_updateState.installStatus = UpdateState::Status::Downloading;
   s_updateState.installMessage = "Downloading update...";

   lock.unlock();

   // Stop backend if running
   if (s_chatBackendPid != -1)
   {
      DLOG("Stopping backend for update");
      Error error = core::system::terminateProcess(s_chatBackendPid);
      if (error)
      {
         WLOG("Failed to stop backend: {}", error.getMessage());
      }
      s_chatBackendPid = -1;
      s_chatBackendPort = -1;
      s_chatBackendUrl.clear();
   }

   FilePath tempPackage = module_context::tempFile("pai-update", "zip");
   Error error = downloadPackage(s_updateState.downloadUrl, tempPackage);

   if (error)
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Error;
      s_updateState.installMessage = "Download failed: " + error.getMessage();

      Error cleanupError = tempPackage.removeIfExists();
      if (cleanupError)
         WLOG("Failed to remove temp package after download failure: {}", cleanupError.getMessage());

      return error;
   }

   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Installing;
      s_updateState.installMessage = "Installing update...";
   }

   error = installPackage(tempPackage);

   Error cleanupError = tempPackage.removeIfExists();
   if (cleanupError)
   {
      WLOG("Failed to remove temp package: {}", cleanupError.getMessage());
   }

   if (error)
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Error;
      s_updateState.installMessage = "Installation failed: " + error.getMessage();

      FilePath userDataDir = xdg::userDataDir();
      FilePath aiPrevDir = userDataDir.completePath("ai.prev");

      if (aiPrevDir.exists())
      {
         Error prevCleanup = aiPrevDir.removeIfExists();
         if (prevCleanup)
            WLOG("Failed to clean up backup directory after failed install: {}", prevCleanup.getMessage());
      }

      return error;
   }

   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);

      FilePath userDataDir = xdg::userDataDir();
      FilePath aiPrevDir = userDataDir.completePath("ai.prev");
      if (aiPrevDir.exists())
      {
         WLOG("Backup directory still exists after successful install, cleaning up");
         Error prevCleanup = aiPrevDir.removeIfExists();
         if (prevCleanup)
            WLOG("Failed to clean up backup directory: {}", prevCleanup.getMessage());
      }

      s_updateState.installStatus = UpdateState::Status::Complete;
      s_updateState.installMessage = "Update complete";
      s_updateState.updateAvailable = false;
      s_updateState.currentVersion = s_updateState.newVersion;
   }

   pResponse->setResult(json::Value());
   return Success();
}

Error chatGetUpdateStatus(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      json::Object result;
      result["status"] = "idle";
      pResponse->setResult(result);
      return Success();
   }

   json::Object result;

   switch (s_updateState.installStatus)
   {
      case UpdateState::Status::Idle:
         result["status"] = "idle";
         break;
      case UpdateState::Status::Downloading:
         result["status"] = "downloading";
         break;
      case UpdateState::Status::Installing:
         result["status"] = "installing";
         break;
      case UpdateState::Status::Complete:
         result["status"] = "complete";
         break;
      case UpdateState::Status::Error:
         result["status"] = "error";
         break;
   }

   result["message"] = s_updateState.installMessage;
   if (s_updateState.installStatus == UpdateState::Status::Error)
   {
      result["error"] = s_updateState.errorMessage;
   }

   pResponse->setResult(result);
   return Success();
}

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio
