/*
 * ChatIntegrity.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ChatIntegrity.hpp"
#include "ChatLogging.hpp"
#include "ChatTypes.hpp"

#include <boost/algorithm/string.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Crypto.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::types;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace integrity {

namespace {

bool isHttpsUrl(const std::string& url)
{
   return boost::starts_with(url, "https://");
}

} // anonymous namespace

Error getPackageInfoFromManifest(
    const json::Object& manifest,
    const std::string& protocolVersion,
    std::string* pPackageVersion,
    std::string* pDownloadUrl,
    std::string* pSha256)
{
   if (!pPackageVersion || !pDownloadUrl)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Get "versions" object
   json::Object versions;
   Error error = json::readObject(manifest, "versions", versions);
   if (error)
   {
      WLOG("Manifest missing 'versions' field");
      return error;
   }

   // Parse RStudio's protocol version to get major version
   SemanticVersion rstudioProtocol;
   if (!rstudioProtocol.parse(protocolVersion))
   {
      WLOG("Failed to parse RStudio protocol version: {}", protocolVersion);
      return systemError(boost::system::errc::invalid_argument,
                        "Invalid protocol version format",
                        ERROR_LOCATION);
   }

   DLOG("Looking for compatible protocols with major version {}", rstudioProtocol.major);

   // Find all compatible protocol versions (matching major version)
   // and select the highest minor version
   SemanticVersion bestProtocol;
   std::string bestPackageVersion;
   std::string bestDownloadUrl;
   std::string bestSha256;
   bool foundCompatible = false;

   for (const auto& entry : versions)
   {
      std::string manifestProtocol = entry.getName();

      // Parse this manifest protocol version
      SemanticVersion manifestProtocolVer;
      if (!manifestProtocolVer.parse(manifestProtocol))
      {
         WLOG("Skipping manifest entry with invalid protocol version: {}", manifestProtocol);
         continue;
      }

      // Check if major version matches
      if (manifestProtocolVer.major != rstudioProtocol.major)
      {
         DLOG("Skipping protocol {} (major version mismatch)", manifestProtocol);
         continue;
      }

      // Extract version info for this protocol
      json::Value versionValue = entry.getValue();
      if (!versionValue.isObject())
      {
         WLOG("Skipping protocol {} (value is not an object)", manifestProtocol);
         continue;
      }

      json::Object versionInfo = versionValue.getObject();

      std::string packageVersion;
      std::string downloadUrl;

      error = json::readObject(versionInfo, "version", packageVersion);
      if (error)
      {
         WLOG("Skipping protocol {} (missing 'version' field)", manifestProtocol);
         continue;
      }

      error = json::readObject(versionInfo, "url", downloadUrl);
      if (error)
      {
         WLOG("Skipping protocol {} (missing 'url' field)", manifestProtocol);
         continue;
      }

      // Validate download URL is HTTPS
      if (!isHttpsUrl(downloadUrl))
      {
         WLOG("Skipping protocol {} (non-HTTPS URL: {})", manifestProtocol, downloadUrl);
         continue;
      }

      // Read optional sha256 field
      std::string sha256;
      json::readObject(versionInfo, "sha256", sha256); // ignore error - field is optional

      // Check if this is the best (highest) protocol version so far
      if (!foundCompatible || manifestProtocolVer > bestProtocol)
      {
         bestProtocol = manifestProtocolVer;
         bestPackageVersion = packageVersion;
         bestDownloadUrl = downloadUrl;
         bestSha256 = sha256;
         foundCompatible = true;
         DLOG("Found compatible protocol {}.{} with package version {}",
              manifestProtocolVer.major, manifestProtocolVer.minor, packageVersion);
      }
   }

   if (!foundCompatible)
   {
      WLOG("No compatible protocol found in manifest for major version {}", rstudioProtocol.major);
      return systemError(boost::system::errc::protocol_not_supported,
                        "No compatible protocol version found in manifest",
                        ERROR_LOCATION);
   }

   *pPackageVersion = bestPackageVersion;
   *pDownloadUrl = bestDownloadUrl;
   if (pSha256)
      *pSha256 = bestSha256;

   DLOG("Selected best compatible protocol {}.{}: package version={}, url={}, sha256={}",
        bestProtocol.major, bestProtocol.minor, bestPackageVersion, bestDownloadUrl,
        bestSha256.empty() ? "(none)" : bestSha256);

   return Success();
}

Error verifyPackageSha256(const FilePath& packagePath,
                          const std::string& expectedSha256)
{
   // Read file content
   // Note: readStringFromFile with LineEndingPassthrough preserves binary content
   std::string fileContent;
   Error error = core::readStringFromFile(packagePath, &fileContent);
   if (error)
   {
      WLOG("Failed to read package file for SHA-256 verification: {}",
           error.getMessage());
      return error;
   }

   // Compute SHA-256
   std::string actualSha256;
   error = core::system::crypto::sha256Hex(fileContent, &actualSha256);
   if (error)
   {
      WLOG("Failed to compute SHA-256 of downloaded package: {}",
           error.getMessage());
      return error;
   }

   // Compare (case-insensitive)
   if (!boost::iequals(actualSha256, expectedSha256))
   {
      ELOG("SHA-256 mismatch for downloaded package at {}!\n"
           "  Expected: {}\n"
           "  Actual:   {}",
           packagePath.getAbsolutePath(), expectedSha256, actualSha256);
      return systemError(boost::system::errc::illegal_byte_sequence,
                        "Downloaded package integrity check failed "
                        "(SHA-256 mismatch)",
                        ERROR_LOCATION);
   }

   DLOG("SHA-256 verification passed: {}", actualSha256);
   return Success();
}

} // namespace integrity
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
