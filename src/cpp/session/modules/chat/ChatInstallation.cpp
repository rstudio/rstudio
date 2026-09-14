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

#include <algorithm>
#include <vector>

#include <core/FileSerializer.hpp>
#include <core/Macros.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <session/SessionModuleContext.hpp>
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
   paths.userInstallEnabled =
      module_context::isPositAssistantInstallationEnabledByAdmin();
   return paths;
}

namespace {

// Reads the version package.json declares. Quiet by design: the resolver
// reads every tier on every locate() call, so a tier that cannot report a
// version must not warn each time -- failures come back as the Error.
core::Error readDeclaredVersion(const core::FilePath& positAiPath, std::string* pVersion)
{
   core::FilePath packageJson = positAiPath.completeChildPath("package.json");
   if (!packageJson.exists())
      return core::fileNotFoundError(packageJson, ERROR_LOCATION);

   std::string content;
   core::Error error = core::readStringFromFile(packageJson, &content);
   if (error)
      return error;

   core::json::Value packageValue;
   if (packageValue.parse(content) || !packageValue.isObject())
      return core::systemError(boost::system::errc::bad_message,
                               "package.json is not a JSON object",
                               ERROR_LOCATION);

   return core::json::readObject(packageValue.getObject(), "version", *pVersion);
}

} // anonymous namespace

namespace {

using types::SemanticVersion;

// One tier's installation as the resolver ranks it.
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
// sort keeps them in the tier order they were collected in.
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
   candidate.compatible = getInstalledProtocolVersion(path) == kProtocolVersion;
   if (readDeclaredVersion(path, &candidate.versionText) ||
       !candidate.version.parse(candidate.versionText))
   {
      candidate.versionText.clear();
      candidate.version = SemanticVersion();
   }
   return candidate;
}

// The valid installations that compete by version, best first. Ties keep the
// order collected here: user, then system, then bundled. A pinned
// posit-assistant-path never competes -- it is used outright or ends the
// search -- so the system tier is left out when the path is pinned, and the
// bundled copy with it (a pinned path that holds no installation must not
// fall back to the shipped version).
std::vector<InstallCandidate> rankedCandidates(const InstallSearchPaths& paths,
                                               bool includeUserInstall)
{
   std::vector<InstallCandidate> candidates;

   if (includeUserInstall && paths.userInstallEnabled &&
       verifyPositAiInstallation(paths.userDataPath))
   {
      candidates.push_back(describeInstallation(paths.userDataPath, "user-level"));
   }

   if (!paths.pinnedSystemPath)
   {
      if (verifyPositAiInstallation(paths.systemPath))
         candidates.push_back(describeInstallation(paths.systemPath, "system-wide"));
      if (verifyPositAiInstallation(paths.bundledPath))
         candidates.push_back(describeInstallation(paths.bundledPath, "bundled"));
   }

   std::stable_sort(candidates.begin(), candidates.end(), outranks);
   return candidates;
}

} // anonymous namespace

core::FilePath locatePositAssistantInstallation(const InstallSearchPaths& paths)
{
   // An installation left in the user data directory before the administrator
   // disabled user-managed installs -- or copied there to get around the
   // setting -- is ignored, never removed. That silently changes which version
   // runs, and can be a downgrade, so say so once per session (locate() runs
   // on every status, verify, and chat request).
   if (!paths.userInstallEnabled && verifyPositAiInstallation(paths.userDataPath) &&
       RS_ONCE())
   {
      WLOG("Ignoring user-level AI installation at {}: Posit Assistant "
           "installation is managed by the administrator",
           paths.userDataPath.getAbsolutePath());
   }

   // posit-assistant-path is the administrator's explicit choice: it is used
   // as-is, even when a newer copy exists elsewhere. A pinned path that holds
   // no installation ends the search for read-only copies: falling through to
   // the bundled one would answer a typo or an unmounted share with a silent
   // downgrade to whatever version shipped with RStudio.
   if (paths.pinnedSystemPath)
   {
      if (verifyPositAiInstallation(paths.systemPath))
      {
         DLOG("Using AI installation pinned by posit-assistant-path: {}",
              paths.systemPath.getAbsolutePath());
         return paths.systemPath;
      }

      // Warn once per session: locate() runs on every status, verify, and chat
      // request, and a misconfigured path would otherwise flood the log.
      if (RS_ONCE())
         WLOG("posit-assistant-path set but installation invalid: {}",
              paths.systemPath.getAbsolutePath());
   }

   // Among the remaining tiers -- the user data directory (Linux/macOS:
   // ~/.local/share/rstudio/pai/bin, Windows: %LOCALAPPDATA%/rstudio/pai/bin),
   // the XDG system config directory (/etc/rstudio/pai/bin, or
   // C:/ProgramData/rstudio/pai/bin), and the copy bundled with RStudio --
   // the newest compatible installation wins, so a per-user install made
   // before a newer bundle shipped does not shadow it indefinitely.
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

   DLOG("No valid AI installation found. Checked locations:");
   if (paths.userInstallEnabled)
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

bool userInstallWouldBeSelected(const InstallSearchPaths& paths, const std::string& version)
{
   if (!paths.userInstallEnabled)
      return false;

   if (paths.pinnedSystemPath && verifyPositAiInstallation(paths.systemPath))
      return false;

   // The manifest only ever offers packages built for this build's protocol.
   InstallCandidate proposed;
   proposed.compatible = true;
   if (!proposed.version.parse(version))
      proposed.version = SemanticVersion();

   // The user install itself is what the install overwrites, so only the
   // read-only tiers compete against the proposed version.
   std::vector<InstallCandidate> readOnly = rankedCandidates(paths, false);
   return readOnly.empty() || !outranks(readOnly.front(), proposed);
}

bool userInstallWouldBeSelected(const std::string& version)
{
   return userInstallWouldBeSelected(positAssistantSearchPaths(), version);
}

std::string getInstalledVersion(const core::FilePath& positAiPath)
{
   if (positAiPath.isEmpty())
      return "";

   std::string version;
   core::Error error = readDeclaredVersion(positAiPath, &version);
   if (error)
   {
      WLOG("Could not read the Posit Assistant version at {}: {}",
           positAiPath.getAbsolutePath(), error.getSummary());
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
