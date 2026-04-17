/*
 * ChatIntegrity.hpp
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

#ifndef SESSION_CHAT_INTEGRITY_HPP
#define SESSION_CHAT_INTEGRITY_HPP

#include <string>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace integrity {

/**
 * Parse manifest to get package info for current protocol version.
 *
 * Selects the highest minor version that matches the major version of the
 * given protocol version string.
 *
 * @param manifest The parsed JSON manifest object
 * @param protocolVersion RStudio's protocol version (e.g. "1.0")
 * @param pPackageVersion Output: the package version string
 * @param pDownloadUrl Output: the package download URL
 * @param pSha256 Output: the SHA-256 hash (optional, may be nullptr)
 * @return Success() or an error if no compatible version is found
 */
core::Error getPackageInfoFromManifest(
    const core::json::Object& manifest,
    const std::string& protocolVersion,
    std::string* pPackageVersion,
    std::string* pDownloadUrl,
    std::string* pSha256 = nullptr);

/**
 * Verify SHA-256 hash of a downloaded package file.
 *
 * @param packagePath Path to the downloaded package file
 * @param expectedSha256 Expected hex-encoded SHA-256 hash
 * @return Success() if hash matches, error on mismatch or I/O failure
 */
core::Error verifyPackageSha256(const core::FilePath& packagePath,
                                const std::string& expectedSha256);

} // namespace integrity
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INTEGRITY_HPP
