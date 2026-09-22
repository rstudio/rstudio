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
#include "ChatTypes.hpp"
#include "ChatConstants.hpp"
#include "ChatLogging.hpp"
#include "ChatSelector.hpp"
#include "ChatSlots.hpp"

#include <algorithm>
#include <mutex>
#include <vector>

#include <core/FileSerializer.hpp>
#include <core/Macros.hpp>
#include <core/system/Xdg.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::logging;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace installation {

namespace {

// Reads one string field out of a JSON file in an installation directory.
// Absent, unparseable, wrong-typed and empty all read as an empty string:
// every caller treats them the same way, and none can do anything about the
// difference. Quiet by design -- verifySlot() runs this over every slot on a
// resolve, and an unreadable file in one of them is not worth a warning each
// time.
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
      DLOG("Failed to read {}: {}", filePath.getAbsolutePath(), error.getMessage());
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

} // anonymous namespace

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

core::FilePath positAiStorageDir()
{
   return core::system::xdg::userDataDir().completePath(kPositAiStorageDirName);
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
   if (verifyInstallDir(binPath))
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
   paths.userStorageDir = positAiStorageDir();
   paths.systemStorageDir =
      core::system::xdg::systemConfigDir().completePath(kPositAiStorageDirName);
   paths.pinnedPath = options().positAssistantPath();
   paths.bundledPath = bundledPositAssistantInstallPath();
   paths.userInstallEnabled =
      module_context::isPositAssistantInstallationEnabledByAdmin();
   return paths;
}

namespace {

using types::SemanticVersion;

// One source's installation as the resolver ranks it.
struct InstallCandidate
{
   InstallCandidate() : tier(""), compatible(false) {}

   core::FilePath path;
   const char* tier;

   // protocol.json declares the protocol this build speaks; a missing file
   // counts as incompatible, matching hasProtocolMismatch()
   bool compatible;

   // package.json's version; 0.0.0 when the file is missing or unparsable, so
   // an install that cannot say what it is never claims to be newer
   SemanticVersion version;
   std::string versionText;
};

// Ranks a compatible installation above an incompatible one, and a newer
// version above an older one. Equal candidates are not ordered, so a stable
// sort keeps them in the source order they were collected in.
bool outranks(const InstallCandidate& lhs, const InstallCandidate& rhs)
{
   if (lhs.compatible != rhs.compatible)
      return lhs.compatible;
   return lhs.version > rhs.version;
}

InstallCandidate describeInstallation(const core::FilePath& path, const char* tier)
{
   InstallCandidate candidate;
   candidate.path = path;
   candidate.tier = tier;
   candidate.compatible = declaredProtocol(path) == kProtocolVersion;
   candidate.versionText = declaredVersion(path);
   if (!candidate.version.parse(candidate.versionText))
   {
      candidate.versionText.clear();
      candidate.version = SemanticVersion();
   }
   return candidate;
}

// The user's slots, chosen by protocol through the user's own selected.json,
// which a stale or missing entry repairs. The one source RStudio writes.
core::FilePath userSlot(const InstallSearchPaths& paths, selector::SelectorRepair repair)
{
   return selector::resolveSlot(paths.userStorageDir, kProtocolVersion, repair);
}

// The administrator's slots, chosen the same way through the administrator's
// selected.json -- which is theirs, so a stale entry is resolved around and
// never rewritten.
core::FilePath systemSlot(const InstallSearchPaths& paths)
{
   return selector::resolveSlot(paths.systemStorageDir,
                                kProtocolVersion,
                                selector::SelectorRepair::Disabled);
}

// The administrator's unversioned directory: what deployments have today.
core::FilePath legacySystemInstall(const InstallSearchPaths& paths)
{
   return paths.systemStorageDir.completeChildPath(kLegacyInstallDirName);
}

// The valid installations that compete by version, best first. Ties keep the
// order collected here: user, then the administrator's slots, then the
// administrator's legacy directory, then bundled. A pinned
// posit-assistant-path never competes -- it is used outright or ends the
// search -- so the read-only sources are left out when the path is pinned (a
// pinned path that holds no installation must not fall back to the shipped
// version).
std::vector<InstallCandidate> rankedCandidates(const InstallSearchPaths& paths,
                                               bool includeUserInstall)
{
   std::vector<InstallCandidate> candidates;

   if (includeUserInstall && paths.userInstallEnabled)
   {
      core::FilePath slot = userSlot(paths, selector::SelectorRepair::Enabled);
      if (!slot.isEmpty())
         candidates.push_back(describeInstallation(slot, "user-level"));
   }

   if (paths.pinnedPath.isEmpty())
   {
      core::FilePath slot = systemSlot(paths);
      if (!slot.isEmpty())
         candidates.push_back(describeInstallation(slot, "system-wide"));

      core::FilePath legacy = legacySystemInstall(paths);
      if (verifyInstallDir(legacy))
         candidates.push_back(describeInstallation(legacy, "system-wide (legacy)"));

      if (verifyInstallDir(paths.bundledPath))
         candidates.push_back(describeInstallation(paths.bundledPath, "bundled"));
   }

   std::stable_sort(candidates.begin(), candidates.end(), outranks);
   return candidates;
}

} // anonymous namespace

core::FilePath locatePositAssistantInstallation(const InstallSearchPaths& paths)
{
   // An installation left in the user's slots before the administrator
   // disabled user-managed installs -- or copied there to get around the
   // setting -- is ignored, never removed. That silently changes which version
   // runs, and can be a downgrade, so say so once per session. Read-only:
   // ignoring the directory includes not repairing its selector.
   if (!paths.userInstallEnabled &&
       !userSlot(paths, selector::SelectorRepair::Disabled).isEmpty() && RS_ONCE())
   {
      WLOG("Ignoring user-level AI installation under {}: Posit Assistant "
           "installation is managed by the administrator",
           paths.userStorageDir.getAbsolutePath());
   }

   // posit-assistant-path is the administrator's explicit choice: it is used
   // as-is, even when a newer copy exists elsewhere. A pinned path that holds
   // no installation ends the search for read-only copies: falling through to
   // the bundled one would answer a typo or an unmounted share with a silent
   // downgrade to whatever version shipped with RStudio.
   if (!paths.pinnedPath.isEmpty())
   {
      if (verifyInstallDir(paths.pinnedPath))
      {
         // Same silent version change as the managed-mode case above, so it
         // gets the same once-per-session notice.
         if (paths.userInstallEnabled &&
             !userSlot(paths, selector::SelectorRepair::Disabled).isEmpty() && RS_ONCE())
         {
            WLOG("Ignoring user-level AI installation under {}: posit-assistant-path "
                 "pins the installation to {}",
                 paths.userStorageDir.getAbsolutePath(),
                 paths.pinnedPath.getAbsolutePath());
         }

         DLOG("Using AI installation pinned by posit-assistant-path: {}",
              paths.pinnedPath.getAbsolutePath());
         return paths.pinnedPath;
      }

      if (RS_ONCE())
         WLOG("posit-assistant-path set but installation invalid: {}",
              paths.pinnedPath.getAbsolutePath());
   }

   // Among the remaining sources the newest compatible installation wins, so
   // a per-user install made before a newer bundle shipped does not shadow it
   // indefinitely.
   std::vector<InstallCandidate> candidates = rankedCandidates(paths, true);
   if (!candidates.empty())
   {
      const InstallCandidate& best = candidates.front();
      DLOG("Using {} AI installation (version {}): {}",
           best.tier,
           best.versionText.empty() ? "unknown" : best.versionText,
           best.path.getAbsolutePath());
      return best.path;
   }

   DLOG("No valid AI installation found (protocol {}). Checked locations:",
        kProtocolVersion);
   if (paths.userInstallEnabled)
      DLOG("  - User slots: {}",
           slots::versionsDir(paths.userStorageDir).getAbsolutePath());
   if (paths.pinnedPath.isEmpty())
   {
      DLOG("  - System slots: {}",
           slots::versionsDir(paths.systemStorageDir).getAbsolutePath());
      DLOG("  - System install dir: {}", legacySystemInstall(paths).getAbsolutePath());
      DLOG("  - Bundled with RStudio: {}", paths.bundledPath.getAbsolutePath());
   }
   else
   {
      DLOG("  - posit-assistant-path: {}", paths.pinnedPath.getAbsolutePath());
   }

   return core::FilePath(); // Not found
}

namespace {

// The installation this session runs, once resolved; see the header for the
// rules. Guarded because the asset handler reads it from HTTP threads while
// the main thread resolves, installs and clears.
std::mutex s_resolutionMutex;
core::FilePath s_resolvedPath;
boost::optional<InstallSearchPaths> s_searchPathsOverride;

} // anonymous namespace

core::FilePath locatePositAssistantInstallation()
{
   std::lock_guard<std::mutex> lock(s_resolutionMutex);

   if (!s_resolvedPath.isEmpty())
   {
      if (verifyInstallDir(s_resolvedPath))
         return s_resolvedPath;

      DLOG("Installation {} no longer holds a complete Posit Assistant; resolving again",
           s_resolvedPath.getAbsolutePath());
   }

   s_resolvedPath = locatePositAssistantInstallation(
      s_searchPathsOverride ? *s_searchPathsOverride : positAssistantSearchPaths());
   return s_resolvedPath;
}

void clearPinnedInstallation()
{
   std::lock_guard<std::mutex> lock(s_resolutionMutex);
   s_resolvedPath = core::FilePath();
}

void setSearchPathsForTesting(const boost::optional<InstallSearchPaths>& paths)
{
   std::lock_guard<std::mutex> lock(s_resolutionMutex);
   s_searchPathsOverride = paths;
   s_resolvedPath = core::FilePath();
}

bool userInstallWouldBeSelected(const InstallSearchPaths& paths, const std::string& version)
{
   if (!paths.userInstallEnabled)
      return false;

   if (!paths.pinnedPath.isEmpty() && verifyInstallDir(paths.pinnedPath))
      return false;

   // The manifest only ever offers packages built for this build's protocol.
   InstallCandidate proposed;
   proposed.compatible = true;
   if (!proposed.version.parse(version))
      proposed.version = SemanticVersion();

   // The user's own slot is what the install replaces as the selection, so
   // only the read-only sources compete against the proposed version.
   std::vector<InstallCandidate> readOnly = rankedCandidates(paths, false);
   return readOnly.empty() || !outranks(readOnly.front(), proposed);
}

bool userInstallWouldBeSelected(const std::string& version)
{
   return userInstallWouldBeSelected(positAssistantSearchPaths(), version);
}

std::string getInstalledVersion()
{
   core::FilePath installDir = locatePositAssistantInstallation();
   if (installDir.isEmpty())
      return std::string();

   std::string version = declaredVersion(installDir);
   if (version.empty())
      WLOG("No package version in {}", installDir.getAbsolutePath());
   else
      DLOG("Installed version: {}", version);

   return version;
}

std::string getInstalledProtocolVersion()
{
   core::FilePath installDir = locatePositAssistantInstallation();
   if (installDir.isEmpty())
      return std::string();

   std::string protocol = declaredProtocol(installDir);
   if (protocol.empty())
      DLOG("No protocol declared in {} (legacy install)", installDir.getAbsolutePath());
   else
      DLOG("Installed protocol version: {}", protocol);

   return protocol;
}

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
