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

#include <gtest/gtest.h>

#include <core/http/Request.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace module_context {
namespace tests {

namespace {

core::http::Request makeRequest(const std::string& uri,
                                const std::string& fetchDest = "")
{
   core::http::Request request;
   request.setUri(uri);
   if (!fetchDest.empty())
      request.setHeader("Sec-Fetch-Dest", fetchDest);
   return request;
}

} // anonymous namespace

// --- Returns false: ?show=1 marker -----------------------------------------

TEST(ShouldAuditFileDownloadTest, ShowOneSkipsForNonRenderable)
{
   // ?show=1 forces a skip even for binary types that the browser would
   // otherwise download (file.show() of a zip, etc.).
   core::http::Request request = makeRequest("/files/archive.zip?show=1");
   core::FilePath file("archive.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ShowOneSkipsRegardlessOfFetchDest)
{
   // The preview marker wins over Sec-Fetch-Dest analysis.
   core::http::Request request = makeRequest("/files/archive.zip?show=1",
                                             "document");
   core::FilePath file("archive.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ShowZeroStillAudits)
{
   // Only the exact value "1" trips the gate; other values fall through.
   core::http::Request request = makeRequest("/files/archive.zip?show=0");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

// --- Returns false: Sec-Fetch-Dest sub-resource ----------------------------

TEST(ShouldAuditFileDownloadTest, FetchDestStyleSkips)
{
   core::http::Request request = makeRequest("/files/main.css", "style");
   core::FilePath file("main.css");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, FetchDestScriptSkips)
{
   core::http::Request request = makeRequest("/files/bundle.js", "script");
   core::FilePath file("bundle.js");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, FetchDestImageSkipsEvenForBinaryExtension)
{
   // Sub-resource Sec-Fetch-Dest skips even when the file is a non-
   // renderable type, since the browser is fetching it as an embedded
   // asset of another page rather than as a download.
   core::http::Request request = makeRequest("/files/embedded.zip", "image");
   core::FilePath file("embedded.zip");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

// --- Returns false: renderable Content-Type --------------------------------

TEST(ShouldAuditFileDownloadTest, PdfMimeSkips)
{
   core::http::Request request = makeRequest("/files/report.pdf");
   core::FilePath file("report.pdf");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, HtmlMimeSkips)
{
   core::http::Request request = makeRequest("/files/index.html");
   core::FilePath file("index.html");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, ImageMimeSkips)
{
   core::http::Request request = makeRequest("/files/photo.png");
   core::FilePath file("photo.png");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, JsonMimeSkips)
{
   core::http::Request request = makeRequest("/files/data.json");
   core::FilePath file("data.json");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, NoExtensionDefaultsToTextAndSkips)
{
   // FilePath::getMimeContentType defaults to "text/plain" for files with
   // an unknown / missing extension. The browser will render those inline.
   core::http::Request request = makeRequest("/files/README");
   core::FilePath file("README");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

// --- Returns true: audited downloads ---------------------------------------

TEST(ShouldAuditFileDownloadTest, ZipAudits)
{
   core::http::Request request = makeRequest("/files/archive.zip");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, OfficeDocxAudits)
{
   // Office formats serve as application/vnd.openxmlformats-officedocument.*
   // - not in the renderable set, so they audit.
   core::http::Request request = makeRequest("/files/report.docx");
   core::FilePath file("report.docx");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, DocumentFetchDestForBinaryAudits)
{
   // Top-level navigation to a non-renderable type is the prototypical
   // "user typed a download URL" case and must produce an audit row.
   core::http::Request request = makeRequest("/files/archive.zip", "document");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, MissingFetchDestForBinaryAudits)
{
   // Older browsers don't send Sec-Fetch-Dest; treat absence as
   // "top-level navigation" and audit when the type is non-renderable.
   core::http::Request request = makeRequest("/files/archive.zip");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, IframeFetchDestForBinaryAudits)
{
   // iframe is intentionally not in the sub-resource skip set - if an
   // iframe loads a binary, it's effectively exfiltration and should be
   // audited.
   core::http::Request request = makeRequest("/files/archive.zip", "iframe");
   core::FilePath file("archive.zip");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST(ShouldAuditFileDownloadTest, IframeFetchDestForRenderableSkips)
{
   // iframe loading a renderable type (HTML, etc.) is just sub-view
   // behavior; skip via the Content-Type branch.
   core::http::Request request = makeRequest("/files/page.html", "iframe");
   core::FilePath file("page.html");
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

} // namespace tests
} // namespace module_context
} // namespace session
} // namespace rstudio
