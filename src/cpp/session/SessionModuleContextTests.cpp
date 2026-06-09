/*
 * SessionModuleContextTests.cpp
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

#include <session/SessionModuleContext.hpp>

#include <string>
#include <vector>

#include <gtest/gtest.h>

#include <core/http/Request.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace module_context {
namespace tests {

namespace {

// http::Request is non-copyable (inherits from boost::noncopyable via
// Message), so the helper writes through a caller-owned instance rather
// than returning by value.
void initRequest(core::http::Request& request,
                 const std::string& uri,
                 const std::string& fetchDest = "")
{
   request.setUri(uri);
   if (!fetchDest.empty())
      request.setHeader("Sec-Fetch-Dest", fetchDest);
}

} // anonymous namespace

// --- Returns false: ?show=1 marker -----------------------------------------

TEST(ShouldAuditFileDownloadTest, ShowOneSkipsForNonRenderable)
{
   // ?show=1 forces a skip even for binary types that the browser would
   // otherwise download (file.show() of a zip, etc.).
   core::http::Request request;
   initRequest(request, "/files/archive.zip?show=1");
   core::FilePath file("archive.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ShowOneSkipsRegardlessOfFetchDest)
{
   // The preview marker wins over Sec-Fetch-Dest analysis.
   core::http::Request request;
   initRequest(request, "/files/archive.zip?show=1", "document");
   core::FilePath file("archive.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ShowZeroStillAudits)
{
   // Only the exact value "1" trips the gate; other values fall through.
   core::http::Request request;
   initRequest(request, "/files/archive.zip?show=0");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

// --- Returns false: Sec-Fetch-Dest sub-resource ----------------------------

TEST(ShouldAuditFileDownloadTest, FetchDestStyleSkips)
{
   core::http::Request request;
   initRequest(request, "/files/main.css", "style");
   core::FilePath file("main.css");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, FetchDestScriptSkips)
{
   core::http::Request request;
   initRequest(request, "/files/bundle.js", "script");
   core::FilePath file("bundle.js");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, FetchDestImageSkipsEvenForBinaryExtension)
{
   // Sub-resource Sec-Fetch-Dest skips even when the file is a non-
   // renderable type, since the browser is fetching it as an embedded
   // asset of another page rather than as a download.
   core::http::Request request;
   initRequest(request, "/files/embedded.zip", "image");
   core::FilePath file("embedded.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, AllSubResourceFetchDestsSkip)
{
   // Pins the contract that every value the helper recognizes as a
   // sub-resource actually skips. Uses a non-renderable file type so a
   // bug that drops the sub-resource branch would surface as a failure
   // (and not be masked by the renderable-Content-Type branch).
   const std::vector<std::string> subResourceDests = {
      "style", "script", "image", "font", "audio", "video",
      "track", "object", "embed", "manifest", "xslt", "report",
      "worker", "serviceworker", "audioworklet", "paintworklet"
   };
   for (const auto& dest : subResourceDests)
   {
      core::http::Request request;
      initRequest(request, "/files/asset.zip", dest);
      core::FilePath file("asset.zip");
      EXPECT_FALSE(shouldAuditFileDownload(request, file))
         << "Sec-Fetch-Dest=" << dest << " should skip audit";
   }
}

// --- Returns false: renderable Content-Type --------------------------------

TEST(ShouldAuditFileDownloadTest, PdfMimeSkips)
{
   core::http::Request request;
   initRequest(request, "/files/report.pdf");
   core::FilePath file("report.pdf");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, HtmlMimeSkips)
{
   core::http::Request request;
   initRequest(request, "/files/index.html");
   core::FilePath file("index.html");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ImageMimeSkips)
{
   core::http::Request request;
   initRequest(request, "/files/photo.png");
   core::FilePath file("photo.png");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, JsonMimeSkips)
{
   core::http::Request request;
   initRequest(request, "/files/data.json");
   core::FilePath file("data.json");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

// --- Returns true: audited downloads ---------------------------------------

TEST(ShouldAuditFileDownloadTest, ZipAudits)
{
   core::http::Request request;
   initRequest(request, "/files/archive.zip");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, OfficeDocxAudits)
{
   // Office formats serve as application/vnd.openxmlformats-officedocument.*
   // - not in the renderable set, so they audit.
   core::http::Request request;
   initRequest(request, "/files/report.docx");
   core::FilePath file("report.docx");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, OctetStreamAudits)
{
   // The canonical "binary, save to disk" Content-Type. Most installer-
   // style files (.exe, .iso, .dmg, .deb, .dll) resolve to this MIME.
   core::http::Request request;
   initRequest(request, "/files/installer.exe");
   core::FilePath file("installer.exe");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, NoExtensionAuditsAsOctetStream)
{
   // The helper passes "application/octet-stream" as the explicit
   // default to getMimeContentType, so files with unknown / missing
   // extensions audit. Closes the rename-bypass surface (e.g. renaming
   // secret.zip to "secret") at the cost of also auditing legitimate
   // extensionless text-file fetches (README, LICENSE, etc.).
   core::http::Request request;
   initRequest(request, "/files/README");
   core::FilePath file("README");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, DocumentFetchDestForBinaryAudits)
{
   // Top-level navigation to a non-renderable type is the prototypical
   // "user typed a download URL" case and must produce an audit row.
   core::http::Request request;
   initRequest(request, "/files/archive.zip", "document");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, MissingFetchDestForBinaryAudits)
{
   // Older browsers don't send Sec-Fetch-Dest; treat absence as
   // "top-level navigation" and audit when the type is non-renderable.
   core::http::Request request;
   initRequest(request, "/files/archive.zip");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, IframeFetchDestForBinaryAudits)
{
   // iframe is intentionally not in the sub-resource skip set - if an
   // iframe loads a binary, it's effectively exfiltration and should be
   // audited.
   core::http::Request request;
   initRequest(request, "/files/archive.zip", "iframe");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, IframeFetchDestForRenderableSkips)
{
   // iframe loading a renderable type (HTML, etc.) is just sub-view
   // behavior; skip via the Content-Type branch.
   core::http::Request request;
   initRequest(request, "/files/page.html", "iframe");
   core::FilePath file("page.html");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

// --- Producer-side: createFileUrl round-trip --------------------------------

TEST(CreateFileUrlTest, HomeFileRoundTripSkipsAudit)
{
   // Regression test for the producer-consumer contract between
   // createFileUrl and shouldAuditFileDownload. If a future edit drops
   // the ?show=1 marker in the in-home branch of createFileUrl, this
   // round trip will fail (the URL won't trigger the preview skip and
   // - because .zip resolves to application/zip - the helper will
   // return true instead of false).
   core::FilePath file = userHomePath().completeChildPath("preview.zip");
   const std::string url = createFileUrl(file);
   ASSERT_NE(url.find("show=1"), std::string::npos)
      << "createFileUrl output missing show=1: " << url;

   core::http::Request request;
   request.setUri("/" + url);
   EXPECT_FALSE(shouldAuditFileDownload(request, file))
      << "round-trip URL " << url
      << " should skip audit via the ?show=1 marker";
}

// --- shouldIgnoreOutputDir --------------------------------------------------
//
// Regression coverage for #17900: a website 'output-dir' that resolves to the
// project directory itself (e.g. 'output-dir: .') must NOT be added to the
// ignore list, or Find in Files / code search drops every result. The decision
// compares by filesystem identity, so '.', './', and 'sub/..' -- all naming the
// project root -- are recognized even though they do not normalize to the base
// path as strings (the bug in the original fix: lexically_normal("base/.") is
// "base/.", not "base").

class ShouldIgnoreOutputDirTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      // Create a unique temp directory to stand in for the project root.
      core::Error error = core::FilePath::tempFilePath(baseDir_);
      ASSERT_FALSE(error) << error.asString();
      error = baseDir_.removeIfExists();
      ASSERT_FALSE(error) << error.asString();
      error = baseDir_.ensureDirectory();
      ASSERT_FALSE(error) << error.asString();
   }

   void TearDown() override
   {
      baseDir_.removeIfExists();
   }

   core::FilePath baseDir_;
};

TEST_F(ShouldIgnoreOutputDirTest, EmptyOutputDirIsNotIgnored)
{
   EXPECT_FALSE(shouldIgnoreOutputDir(baseDir_, ""));
}

TEST_F(ShouldIgnoreOutputDirTest, DotResolvesToBaseAndIsNotIgnored)
{
   // 'output-dir: .' -- the exact #17900 repro.
   EXPECT_FALSE(shouldIgnoreOutputDir(baseDir_, "."));
}

TEST_F(ShouldIgnoreOutputDirTest, DotSlashResolvesToBaseAndIsNotIgnored)
{
   EXPECT_FALSE(shouldIgnoreOutputDir(baseDir_, "./"));
}

TEST_F(ShouldIgnoreOutputDirTest, SubdirParentResolvesToBaseAndIsNotIgnored)
{
   // 'sub/..' names the project root; boost::filesystem::equivalent needs the
   // intermediate 'sub' to exist on disk to resolve the path.
   core::Error error = baseDir_.completeChildPath("sub").ensureDirectory();
   ASSERT_FALSE(error) << error.asString();
   EXPECT_FALSE(shouldIgnoreOutputDir(baseDir_, "sub/.."));
}

TEST_F(ShouldIgnoreOutputDirTest, RealSubdirIsIgnored)
{
   // A genuine output dir (e.g. a built '_site') is distinct from the project
   // root and must still be ignored.
   core::Error error = baseDir_.completeChildPath("_site").ensureDirectory();
   ASSERT_FALSE(error) << error.asString();
   EXPECT_TRUE(shouldIgnoreOutputDir(baseDir_, "_site"));
}

TEST_F(ShouldIgnoreOutputDirTest, UnbuiltSubdirIsIgnored)
{
   // A not-yet-built output dir does not exist on disk; it must still be
   // ignored (isEquivalentTo returns false when a path is missing).
   EXPECT_TRUE(shouldIgnoreOutputDir(baseDir_, "_site_not_built"));
}

} // namespace tests
} // namespace module_context
} // namespace session
} // namespace rstudio
