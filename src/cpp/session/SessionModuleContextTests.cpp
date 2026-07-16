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

#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <shared_core/FilePath.hpp>

#include <session/SessionOptions.hpp>

#ifdef __APPLE__
# include <climits>
# include <cstdlib>
# include <unistd.h>
# include <CoreFoundation/CoreFoundation.h>
#endif

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

// The Sec-Fetch-Dest and Content-Type based skips below only apply when the
// track-resource-downloads option is disabled, which is its default value.
// The FileDownloadGrantTest fixture further down exercises the opt-in-enabled
// behavior and the server-initiated preview grant.

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

// --- Grant + opt-in behavior ------------------------------------------------
//
// These need real files (registerDownloadGrant stamps size/mtime) and toggle
// the process-wide track-resource-downloads option, so they use a fixture that
// creates temp files and resets the option after each case.

class FileDownloadGrantTest : public ::testing::Test
{
protected:
   void TearDown() override
   {
      // options() is a process-wide singleton; make sure an opt-in enabled by
      // one test doesn't leak into the next.
      session::options().setTrackResourceDownloads(false);
      for (core::FilePath& file : tempFiles_)
         file.removeIfExists();
   }

   core::FilePath makeTempFile(const std::string& extension,
                               const std::string& contents)
   {
      core::FilePath path;
      core::Error error = core::FilePath::tempFilePath(extension, path);
      EXPECT_FALSE(error) << error.asString();
      error = core::writeStringToFile(path, contents);
      EXPECT_FALSE(error) << error.asString();
      tempFiles_.push_back(path);
      return path;
   }

   void enableResourceTracking()
   {
      session::options().setTrackResourceDownloads(true);
   }

   std::vector<core::FilePath> tempFiles_;
};

TEST_F(FileDownloadGrantTest, RegisteredPreviewSkipsAudit)
{
   // A server-initiated preview registers a grant; the subsequent request for
   // that exact file (a non-renderable .zip, so nothing else would skip it) is
   // not audited. This is the unforgeable replacement for the old ?show=1
   // marker.
   core::FilePath file = makeTempFile(".zip", "payload");
   registerDownloadGrant(file);

   core::http::Request request;
   EXPECT_FALSE(shouldAuditFileDownload(request, file));
}

TEST_F(FileDownloadGrantTest, GrantIsPathSpecific)
{
   // A grant for one file must not suppress the audit for a different file.
   core::FilePath granted = makeTempFile(".zip", "payload");
   core::FilePath other = makeTempFile(".zip", "secret");
   registerDownloadGrant(granted);

   core::http::Request request;
   EXPECT_TRUE(shouldAuditFileDownload(request, other));
}

TEST_F(FileDownloadGrantTest, StaleGrantAudits)
{
   // If the file is replaced after the grant is recorded, the size/mtime stamp
   // no longer matches, so the download is audited -- a grant can't be reused
   // to smuggle out content swapped in at the same path.
   core::FilePath file = makeTempFile(".zip", "preview");
   registerDownloadGrant(file);

   // Rewrite with different-length content to change the stamp.
   core::Error error = core::writeStringToFile(file, "swapped-in-secret-data");
   ASSERT_FALSE(error) << error.asString();

   core::http::Request request;
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST_F(FileDownloadGrantTest, CreateFileUrlRegistersGrantAndDropsShowMarker)
{
   // Producer side: createFileUrl registers the grant and no longer appends a
   // ?show=1 marker. Regression guard for the producer/consumer contract.
   core::FilePath file = makeTempFile(".zip", "preview");
   const std::string url = createFileUrl(file);

   EXPECT_EQ(url.find("show=1"), std::string::npos)
      << "createFileUrl should no longer emit a show=1 marker: " << url;

   core::http::Request request;
   EXPECT_FALSE(shouldAuditFileDownload(request, file))
      << "createFileUrl should have registered a download grant for " << url;
}

TEST_F(FileDownloadGrantTest, TrackingEnabledAuditsRenamedText)
{
   // #3: renaming a binary to a text extension lands a text/* Content-Type,
   // which is skipped by default. With resource tracking enabled that skip is
   // off, so the download is audited regardless of the client-influenced type.
   core::FilePath file = makeTempFile(".txt", "actually-not-text");
   enableResourceTracking();

   core::http::Request request;
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST_F(FileDownloadGrantTest, TrackingEnabledAuditsSpoofedFetchDest)
{
   // #2: a client can send Sec-Fetch-Dest: image to masquerade as a sub-
   // resource. With resource tracking enabled that skip is off, so the
   // download is audited.
   core::FilePath file = makeTempFile(".zip", "payload");
   enableResourceTracking();

   core::http::Request request;
   request.setHeader("Sec-Fetch-Dest", "image");
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
}

TEST_F(FileDownloadGrantTest, TrackingEnabledAuditsRenderableType)
{
   // A genuinely renderable type (PDF) is skipped by default but audited once
   // resource tracking is enabled.
   core::FilePath file = makeTempFile(".pdf", "%PDF-1.4");

   core::http::Request request;
   EXPECT_FALSE(shouldAuditFileDownload(request, file));

   enableResourceTracking();
   EXPECT_TRUE(shouldAuditFileDownload(request, file));
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

#ifdef __APPLE__

// --- Finder aliases (#18158) -------------------------------------------------
//
// macOS Finder aliases are bookmark files, not symlinks. The fixture creates
// real alias files through the CoreFoundation bookmark API -- the same data
// Finder writes -- so these tests exercise genuine alias resolution.

namespace {

CFURLRef createCFURL(const core::FilePath& path, bool isDirectory)
{
   const std::string& absolutePath = path.getAbsolutePath();
   return CFURLCreateFromFileSystemRepresentation(
            kCFAllocatorDefault,
            reinterpret_cast<const UInt8*>(absolutePath.c_str()),
            absolutePath.size(),
            isDirectory);
}

bool createFinderAlias(const core::FilePath& targetPath,
                       const core::FilePath& aliasPath)
{
   CFURLRef targetUrl = createCFURL(targetPath, targetPath.isDirectory());
   if (targetUrl == nullptr)
      return false;

   CFDataRef bookmark = CFURLCreateBookmarkData(
            kCFAllocatorDefault,
            targetUrl,
            kCFURLBookmarkCreationSuitableForBookmarkFile,
            nullptr,
            nullptr,
            nullptr);
   CFRelease(targetUrl);
   if (bookmark == nullptr)
      return false;

   CFURLRef aliasUrl = createCFURL(aliasPath, false);
   bool written = aliasUrl != nullptr &&
                  CFURLWriteBookmarkDataToFile(bookmark, aliasUrl, 0, nullptr);
   if (aliasUrl != nullptr)
      CFRelease(aliasUrl);
   CFRelease(bookmark);
   return written;
}

} // anonymous namespace

class FinderAliasTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      core::FilePath tempPath;
      core::Error error = core::FilePath::tempFilePath(tempPath);
      ASSERT_FALSE(error) << error.asString();
      error = tempPath.ensureDirectory();
      ASSERT_FALSE(error) << error.asString();

      // canonicalize (/var -> /private/var) so fixture paths compare equal
      // to resolved alias targets, which come back canonicalized
      char realPath[PATH_MAX];
      ASSERT_NE(nullptr,
                ::realpath(tempPath.getAbsolutePath().c_str(), realPath));
      baseDir_ = core::FilePath(realPath);

      targetFile_ = baseDir_.completeChildPath("target.txt");
      error = targetFile_.ensureFile();
      ASSERT_FALSE(error) << error.asString();

      targetDir_ = baseDir_.completeChildPath("target-dir");
      error = targetDir_.ensureDirectory();
      ASSERT_FALSE(error) << error.asString();
   }

   void TearDown() override
   {
      baseDir_.removeIfExists();
   }

   core::FilePath makeAlias(const core::FilePath& targetPath,
                            const std::string& name)
   {
      core::FilePath aliasPath = baseDir_.completeChildPath(name);
      EXPECT_TRUE(createFinderAlias(targetPath, aliasPath));
      return aliasPath;
   }

   core::FilePath baseDir_;
   core::FilePath targetFile_;
   core::FilePath targetDir_;
};

TEST_F(FinderAliasTest, DetectsAliasToFile)
{
   core::FilePath alias = makeAlias(targetFile_, "file-alias");
   EXPECT_TRUE(isFinderAlias(alias));
}

TEST_F(FinderAliasTest, DetectsAliasToDirectory)
{
   core::FilePath alias = makeAlias(targetDir_, "dir-alias");
   EXPECT_TRUE(isFinderAlias(alias));
}

TEST_F(FinderAliasTest, RegularFileIsNotAlias)
{
   EXPECT_FALSE(isFinderAlias(targetFile_));
}

TEST_F(FinderAliasTest, DirectoryIsNotAlias)
{
   EXPECT_FALSE(isFinderAlias(targetDir_));
}

TEST_F(FinderAliasTest, SymlinkIsNotAlias)
{
   // symlinks already navigate correctly (the filesystem follows them);
   // they must not be reported as aliases even though NSURLIsAliasFileKey
   // is true for both
   core::FilePath link = baseDir_.completeChildPath("symlink");
   ASSERT_EQ(0, ::symlink(targetFile_.getAbsolutePath().c_str(),
                          link.getAbsolutePath().c_str()));
   EXPECT_FALSE(isFinderAlias(link));
}

TEST_F(FinderAliasTest, NonexistentPathIsNotAlias)
{
   EXPECT_FALSE(isFinderAlias(baseDir_.completeChildPath("does-not-exist")));
}

TEST_F(FinderAliasTest, ResolvesAliasToFile)
{
   core::FilePath alias = makeAlias(targetFile_, "file-alias");
   core::FilePath target;
   core::Error error = resolveFinderAlias(alias, &target);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(targetFile_.getAbsolutePath(), target.getAbsolutePath());
}

TEST_F(FinderAliasTest, ResolvesAliasToDirectory)
{
   core::FilePath alias = makeAlias(targetDir_, "dir-alias");
   core::FilePath target;
   core::Error error = resolveFinderAlias(alias, &target);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(targetDir_.getAbsolutePath(), target.getAbsolutePath());
   EXPECT_TRUE(target.isDirectory());
}

TEST_F(FinderAliasTest, ResolveFailsWhenTargetDeleted)
{
   core::FilePath doomed = baseDir_.completeChildPath("doomed.txt");
   core::Error error = doomed.ensureFile();
   ASSERT_FALSE(error) << error.asString();
   core::FilePath alias = makeAlias(doomed, "doomed-alias");
   error = doomed.remove();
   ASSERT_FALSE(error) << error.asString();

   core::FilePath target;
   error = resolveFinderAlias(alias, &target);
   EXPECT_TRUE(static_cast<bool>(error));
}

// --- createFileSystemItem alias contract -------------------------------------
//
// The Files pane decides navigate-vs-open from 'dir' and substitutes
// 'alias_target' for the clicked path, so a directory alias must carry the
// target's directory-ness, not the alias file's.

TEST_F(FinderAliasTest, FileSystemItemForDirectoryAlias)
{
   core::FilePath alias = makeAlias(targetDir_, "dir-alias");
   core::json::Object item = createFileSystemItem(alias);
   EXPECT_TRUE(item["dir"].getBool());
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(targetDir_.getAbsolutePath(), item["alias_target"].getString());
}

TEST_F(FinderAliasTest, FileSystemItemForFileAlias)
{
   core::FilePath alias = makeAlias(targetFile_, "file-alias");
   core::json::Object item = createFileSystemItem(alias);
   EXPECT_FALSE(item["dir"].getBool());
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(targetFile_.getAbsolutePath(), item["alias_target"].getString());
}

TEST_F(FinderAliasTest, FileSystemItemForRegularFileHasNoAliasTarget)
{
   core::json::Object item = createFileSystemItem(targetFile_);
   EXPECT_FALSE(item.hasMember("alias_target"));
}

TEST_F(FinderAliasTest, FileSystemItemForBrokenAliasHasNoAliasTarget)
{
   core::FilePath doomed = baseDir_.completeChildPath("doomed.txt");
   core::Error error = doomed.ensureFile();
   ASSERT_FALSE(error) << error.asString();
   core::FilePath alias = makeAlias(doomed, "doomed-alias");
   error = doomed.remove();
   ASSERT_FALSE(error) << error.asString();

   core::json::Object item = createFileSystemItem(alias);
   EXPECT_FALSE(item.hasMember("alias_target"));
   EXPECT_FALSE(item["dir"].getBool());
}

#endif // __APPLE__

} // namespace tests
} // namespace module_context
} // namespace session
} // namespace rstudio
