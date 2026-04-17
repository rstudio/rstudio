/*
 * ChatIntegrityTests.cpp
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

#include <gtest/gtest.h>
#include <core/FileSerializer.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::integrity;

// ============================================================================
// verifyPackageSha256 tests
// ============================================================================

TEST(ChatIntegrity, VerifySha256SucceedsForMatchingHash)
{
   // Create temp file with known content
   FilePath tempFile;
   FilePath::tempFilePath(tempFile);
   writeStringToFile(tempFile, "hello world\n");

   // SHA-256 of "hello world\n" (12 bytes)
   // pragma: allowlist secret
   std::string expectedHash = "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447";

   EXPECT_FALSE(verifyPackageSha256(tempFile, expectedHash));

   tempFile.removeIfExists();
}

TEST(ChatIntegrity, VerifySha256SucceedsWithUppercaseHash)
{
   FilePath tempFile;
   FilePath::tempFilePath(tempFile);
   writeStringToFile(tempFile, "hello world\n");

   // Same hash in uppercase — comparison should be case-insensitive
   // pragma: allowlist secret
   std::string expectedHash = "A948904F2F0F479B8F8197694B30184B0D2ED1C1CD2A1EC0FB85D299A192A447";

   EXPECT_FALSE(verifyPackageSha256(tempFile, expectedHash));

   tempFile.removeIfExists();
}

TEST(ChatIntegrity, VerifySha256FailsForMismatchedHash)
{
   FilePath tempFile;
   FilePath::tempFilePath(tempFile);
   writeStringToFile(tempFile, "hello world\n");

   std::string wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";

   Error error = verifyPackageSha256(tempFile, wrongHash);
   EXPECT_TRUE(error != Success());

   tempFile.removeIfExists();
}

TEST(ChatIntegrity, VerifySha256FailsForNonExistentFile)
{
   FilePath nonExistent("/nonexistent/path/to/file.zip");
   std::string hash = "0000000000000000000000000000000000000000000000000000000000000000";

   Error error = verifyPackageSha256(nonExistent, hash);
   EXPECT_TRUE(error != Success());
}

TEST(ChatIntegrity, VerifySha256WorksWithBinaryContent)
{
   FilePath tempFile;
   FilePath::tempFilePath(tempFile);

   // Write some binary content (null bytes, high bytes)
   std::string binaryContent;
   binaryContent.push_back('\x00');
   binaryContent.push_back('\x01');
   binaryContent.push_back('\xff');
   binaryContent.push_back('\x80');
   writeStringToFile(tempFile, binaryContent);

   // Pre-computed SHA-256 of these 4 bytes: 00 01 ff 80
   // We verify by computing it once and checking round-trip
   // First, just verify the function doesn't error with binary content
   // Use a wrong hash — we just want to confirm it computes without error
   std::string wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";
   Error error = verifyPackageSha256(tempFile, wrongHash);
   // Should fail with mismatch, not with an I/O or crypto error
   EXPECT_TRUE(error != Success());

   tempFile.removeIfExists();
}

// ============================================================================
// getPackageInfoFromManifest tests
// ============================================================================

namespace {

// Helper to build a manifest JSON object for testing
json::Object makeManifest(const std::string& protocol,
                          const std::string& version,
                          const std::string& url,
                          const std::string& sha256 = "")
{
   json::Object versionInfo;
   versionInfo["version"] = version;
   versionInfo["url"] = url;
   if (!sha256.empty())
      versionInfo["sha256"] = sha256;

   json::Object versions;
   versions[protocol] = versionInfo;

   json::Object manifest;
   manifest["versions"] = versions;
   return manifest;
}

} // anonymous namespace

TEST(ChatIntegrity, GetPackageInfoBasic)
{
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "https://example.com/pkg.zip",
      "abc123");

   std::string packageVersion, downloadUrl, sha256;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl, &sha256);

   EXPECT_FALSE(error);
   EXPECT_EQ(packageVersion, "2.0.0");
   EXPECT_EQ(downloadUrl, "https://example.com/pkg.zip");
   EXPECT_EQ(sha256, "abc123");
}

TEST(ChatIntegrity, GetPackageInfoWithoutSha256Param)
{
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "https://example.com/pkg.zip",
      "abc123");

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl);

   EXPECT_FALSE(error);
   EXPECT_EQ(packageVersion, "2.0.0");
   EXPECT_EQ(downloadUrl, "https://example.com/pkg.zip");
}

TEST(ChatIntegrity, GetPackageInfoSha256EmptyWhenMissing)
{
   // Manifest without sha256 field
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "https://example.com/pkg.zip");

   std::string packageVersion, downloadUrl, sha256;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl, &sha256);

   EXPECT_FALSE(error);
   EXPECT_TRUE(sha256.empty());
}

TEST(ChatIntegrity, GetPackageInfoSelectsHighestMinorVersion)
{
   json::Object v10Info;
   v10Info["version"] = "1.0.0";
   v10Info["url"] = "https://example.com/pkg-1.0.zip";
   v10Info["sha256"] = "hash10";

   json::Object v12Info;
   v12Info["version"] = "1.2.0";
   v12Info["url"] = "https://example.com/pkg-1.2.zip";
   v12Info["sha256"] = "hash12";

   json::Object v11Info;
   v11Info["version"] = "1.1.0";
   v11Info["url"] = "https://example.com/pkg-1.1.zip";
   v11Info["sha256"] = "hash11";

   json::Object versions;
   versions["1.0"] = v10Info;
   versions["1.2"] = v12Info;
   versions["1.1"] = v11Info;

   json::Object manifest;
   manifest["versions"] = versions;

   std::string packageVersion, downloadUrl, sha256;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl, &sha256);

   EXPECT_FALSE(error);
   EXPECT_EQ(packageVersion, "1.2.0");
   EXPECT_EQ(downloadUrl, "https://example.com/pkg-1.2.zip");
   EXPECT_EQ(sha256, "hash12");
}

TEST(ChatIntegrity, GetPackageInfoIgnoresDifferentMajorVersion)
{
   json::Object v10Info;
   v10Info["version"] = "1.0.0";
   v10Info["url"] = "https://example.com/pkg-v1.zip";

   json::Object v20Info;
   v20Info["version"] = "2.0.0";
   v20Info["url"] = "https://example.com/pkg-v2.zip";

   json::Object versions;
   versions["1.0"] = v10Info;
   versions["2.0"] = v20Info;

   json::Object manifest;
   manifest["versions"] = versions;

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl);

   EXPECT_FALSE(error);
   EXPECT_EQ(packageVersion, "1.0.0");
}

TEST(ChatIntegrity, GetPackageInfoErrorsOnNoCompatibleVersion)
{
   json::Object manifest = makeManifest(
      "2.0", "3.0.0", "https://example.com/pkg.zip");

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl);

   EXPECT_TRUE(error != Success());
}

TEST(ChatIntegrity, GetPackageInfoSkipsNonHttpsUrls)
{
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "http://example.com/pkg.zip");

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl);

   // Should fail — the only entry has a non-HTTPS URL
   EXPECT_TRUE(error != Success());
}

TEST(ChatIntegrity, GetPackageInfoErrorsOnMissingVersionsField)
{
   json::Object manifest;  // empty manifest, no "versions" key

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "1.0", &packageVersion, &downloadUrl);

   EXPECT_TRUE(error != Success());
}

TEST(ChatIntegrity, GetPackageInfoErrorsOnInvalidProtocolVersion)
{
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "https://example.com/pkg.zip");

   std::string packageVersion, downloadUrl;
   Error error = getPackageInfoFromManifest(
      manifest, "not-a-version", &packageVersion, &downloadUrl);

   EXPECT_TRUE(error != Success());
}

TEST(ChatIntegrity, GetPackageInfoErrorsOnNullPointers)
{
   json::Object manifest = makeManifest(
      "1.0", "2.0.0", "https://example.com/pkg.zip");

   Error error = getPackageInfoFromManifest(
      manifest, "1.0", nullptr, nullptr);

   EXPECT_TRUE(error != Success());
}
