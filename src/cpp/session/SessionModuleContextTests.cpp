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

#ifdef __APPLE__
# include <climits>
# include <cstdlib>
# include <unistd.h>
# include <CoreFoundation/CoreFoundation.h>
#endif

#ifdef _WIN32
# include <windows.h>
# include <objbase.h>
# include <shlobj.h>
# include <algorithm>
# include <core/FileSerializer.hpp>
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

#ifdef _WIN32

// --- Windows shortcuts (#7327) -----------------------------------------------
//
// Windows .lnk shortcuts are shell objects, not symlinks. The fixture creates
// real .lnk files through IShellLinkW + IPersistFile::Save -- the same data
// Explorer writes -- so these tests exercise genuine shortcut resolution.
//
// COM note: rsession --run-tests reaches gtest on the main thread, after the
// eager CoInitializeEx in SessionMain.cpp, so the fixture (like the resolver
// itself) uses COM without initializing it here.

namespace {

// the shell APIs store and expect native (backslash) separators, while
// FilePath::getAbsolutePathW() may contain forward-slash separators (it
// exposes the stored representation, which FilePath builds in generic form)
std::wstring toNativeSeparators(std::wstring path)
{
   std::replace(path.begin(), path.end(), L'/', L'\\');
   return path;
}

// each failing COM step records its own failure so a CI break names the
// step and HRESULT instead of a bare "Actual: false"
bool createWindowsShortcut(const core::FilePath& targetPath,
                           const core::FilePath& shortcutPath)
{
   IShellLinkW* pShellLink = nullptr;
   HRESULT hr = ::CoCreateInstance(CLSID_ShellLink,
                                   nullptr,
                                   CLSCTX_INPROC_SERVER,
                                   IID_IShellLinkW,
                                   (void**) &pShellLink);
   if (FAILED(hr))
   {
      ADD_FAILURE() << "CoCreateInstance(CLSID_ShellLink) failed, hr=0x"
                    << std::hex << hr;
      return false;
   }

   std::wstring target = toNativeSeparators(targetPath.getAbsolutePathW());
   hr = pShellLink->SetPath(target.c_str());
   if (FAILED(hr))
   {
      ADD_FAILURE() << "IShellLinkW::SetPath failed, hr=0x" << std::hex << hr;
      pShellLink->Release();
      return false;
   }

   IPersistFile* pPersistFile = nullptr;
   hr = pShellLink->QueryInterface(IID_IPersistFile, (void**) &pPersistFile);
   if (FAILED(hr))
   {
      ADD_FAILURE() << "QueryInterface(IID_IPersistFile) failed, hr=0x"
                    << std::hex << hr;
      pShellLink->Release();
      return false;
   }

   std::wstring shortcut = toNativeSeparators(shortcutPath.getAbsolutePathW());
   hr = pPersistFile->Save(shortcut.c_str(), TRUE);
   if (FAILED(hr))
      ADD_FAILURE() << "IPersistFile::Save failed, hr=0x" << std::hex << hr;

   pPersistFile->Release();
   pShellLink->Release();
   return SUCCEEDED(hr);
}

} // anonymous namespace

class WindowsShortcutTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      core::FilePath tempPath;
      core::Error error = core::FilePath::tempFilePath(tempPath);
      ASSERT_FALSE(error) << error.asString();
      error = tempPath.ensureDirectory();
      ASSERT_FALSE(error) << error.asString();

      // canonicalize (8.3 short names, e.g. RUNNER~1 in TEMP) so fixture
      // paths compare equal to targets read back from the shell, which
      // stores long-form paths
      baseDir_ = core::FilePath(tempPath.getCanonicalPath());

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

   core::FilePath makeShortcut(const core::FilePath& targetPath,
                               const std::string& name)
   {
      core::FilePath shortcutPath = baseDir_.completeChildPath(name);
      // createWindowsShortcut records its own failure (step + HRESULT)
      createWindowsShortcut(targetPath, shortcutPath);
      return shortcutPath;
   }

   core::FilePath baseDir_;
   core::FilePath targetFile_;
   core::FilePath targetDir_;
};

TEST_F(WindowsShortcutTest, DetectsShortcutToFile)
{
   core::FilePath shortcut = makeShortcut(targetFile_, "file-shortcut.lnk");
   EXPECT_TRUE(isWindowsShortcut(shortcut));
}

TEST_F(WindowsShortcutTest, DetectsShortcutToDirectory)
{
   core::FilePath shortcut = makeShortcut(targetDir_, "dir-shortcut.lnk");
   EXPECT_TRUE(isWindowsShortcut(shortcut));
}

TEST_F(WindowsShortcutTest, DetectsUppercaseLnkExtension)
{
   core::FilePath shortcut = makeShortcut(targetFile_, "FILE-SHORTCUT.LNK");
   EXPECT_TRUE(isWindowsShortcut(shortcut));
}

TEST_F(WindowsShortcutTest, RegularFileIsNotShortcut)
{
   EXPECT_FALSE(isWindowsShortcut(targetFile_));
}

TEST_F(WindowsShortcutTest, DirectoryIsNotShortcut)
{
   EXPECT_FALSE(isWindowsShortcut(targetDir_));
}

TEST_F(WindowsShortcutTest, DirectoryNamedLnkIsNotShortcut)
{
   // a directory can be named *.lnk; only regular files are shortcuts
   core::FilePath dir = baseDir_.completeChildPath("folder.lnk");
   core::Error error = dir.ensureDirectory();
   ASSERT_FALSE(error) << error.asString();
   EXPECT_FALSE(isWindowsShortcut(dir));
}

TEST_F(WindowsShortcutTest, NonexistentPathIsNotShortcut)
{
   EXPECT_FALSE(isWindowsShortcut(baseDir_.completeChildPath("missing.lnk")));
}

TEST_F(WindowsShortcutTest, ResolvesShortcutToFile)
{
   core::FilePath shortcut = makeShortcut(targetFile_, "file-shortcut.lnk");
   core::FilePath target;
   bool targetIsDir = true;
   core::Error error = resolveWindowsShortcut(shortcut, &target, &targetIsDir);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(targetFile_.getAbsolutePath(), target.getAbsolutePath());
   EXPECT_FALSE(targetIsDir);
}

TEST_F(WindowsShortcutTest, ResolvesShortcutToDirectory)
{
   core::FilePath shortcut = makeShortcut(targetDir_, "dir-shortcut.lnk");
   core::FilePath target;
   bool targetIsDir = false;
   core::Error error = resolveWindowsShortcut(shortcut, &target, &targetIsDir);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(targetDir_.getAbsolutePath(), target.getAbsolutePath());
   EXPECT_TRUE(target.isDirectory());

   // the link's stored attributes carry the target's directory-ness, so a
   // caller can badge/navigate correctly without touching the target
   EXPECT_TRUE(targetIsDir);
}

TEST_F(WindowsShortcutTest, ResolvesShortcutWithNonAsciiNames)
{
   // exercise the UTF-8 <-> UTF-16 boundary that every shortcut path
   // crosses: FilePath stores UTF-8 while the shell link APIs speak
   // UTF-16. "\xC3\xA9" is e-acute and "\xE6\x97\xA5\xE6\x9C\xAC" is
   // Japanese 'nihon'; byte escapes keep this source file ASCII-only.
   const std::string suffix = "-\xC3\xA9\xE6\x97\xA5\xE6\x9C\xAC";

   core::FilePath target = baseDir_.completeChildPath("cible" + suffix + ".txt");
   core::Error error = target.ensureFile();
   ASSERT_FALSE(error) << error.asString();

   core::FilePath shortcut = makeShortcut(target, "raccourci" + suffix + ".lnk");
   EXPECT_TRUE(isWindowsShortcut(shortcut));

   core::FilePath resolved;
   bool targetIsDir = true;
   error = resolveWindowsShortcut(shortcut, &resolved, &targetIsDir);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(target.getAbsolutePath(), resolved.getAbsolutePath());
   EXPECT_FALSE(targetIsDir);

   core::json::Object item = createFileSystemItem(shortcut);
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(createAliasedPath(target), item["alias_target"].getString());
}

TEST_F(WindowsShortcutTest, GarbageContentFailsToResolve)
{
   // any *.lnk regular file is treated as a shortcut (like Explorer), so
   // non-shortcut content must surface as a resolution error, not a crash
   core::FilePath garbage = baseDir_.completeChildPath("garbage.lnk");
   core::Error error = core::writeStringToFile(garbage, "this is not a shell link");
   ASSERT_FALSE(error) << error.asString();

   EXPECT_TRUE(isWindowsShortcut(garbage));

   core::FilePath target;
   bool targetIsDir = false;
   error = resolveWindowsShortcut(garbage, &target, &targetIsDir);
   EXPECT_TRUE(static_cast<bool>(error));
}

TEST_F(WindowsShortcutTest, ResolveReturnsStoredPathWhenTargetDeleted)
{
   // DIVERGENCE from FinderAliasTest.ResolveFailsWhenTargetDeleted: a .lnk
   // stores its target path, and resolveWindowsShortcut deliberately skips
   // IShellLink::Resolve (no disk/network search during listings), so
   // resolution still succeeds after the target is deleted. Broken-shortcut
   // detection is createFileSystemItem's exists() check on the result.
   core::FilePath doomed = baseDir_.completeChildPath("doomed.txt");
   core::Error error = doomed.ensureFile();
   ASSERT_FALSE(error) << error.asString();
   core::FilePath shortcut = makeShortcut(doomed, "doomed-shortcut.lnk");
   error = doomed.remove();
   ASSERT_FALSE(error) << error.asString();

   core::FilePath target;
   bool targetIsDir = true;
   error = resolveWindowsShortcut(shortcut, &target, &targetIsDir);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(doomed.getAbsolutePath(), target.getAbsolutePath());
   EXPECT_FALSE(target.exists());
   EXPECT_FALSE(targetIsDir);
}

// --- createFileSystemItem shortcut contract -----------------------------------
//
// The Files pane decides navigate-vs-open from 'dir' and substitutes
// 'alias_target' for the clicked path, so a directory shortcut must carry the
// target's directory-ness, not the .lnk file's. Unlike the macOS fixture,
// %TEMP% lives under the user home, so emitted paths are home-aliased --
// compare against createAliasedPath rather than raw absolute paths.

TEST_F(WindowsShortcutTest, FileSystemItemForDirectoryShortcut)
{
   core::FilePath shortcut = makeShortcut(targetDir_, "dir-shortcut.lnk");
   core::json::Object item = createFileSystemItem(shortcut);
   EXPECT_TRUE(item["dir"].getBool());
   ASSERT_TRUE(item.hasMember("is_shortcut"));
   EXPECT_TRUE(item["is_shortcut"].getBool());
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(createAliasedPath(targetDir_), item["alias_target"].getString());
}

TEST_F(WindowsShortcutTest, FileSystemItemForFileShortcut)
{
   core::FilePath shortcut = makeShortcut(targetFile_, "file-shortcut.lnk");
   core::json::Object item = createFileSystemItem(shortcut);
   EXPECT_FALSE(item["dir"].getBool());
   ASSERT_TRUE(item.hasMember("is_shortcut"));
   EXPECT_TRUE(item["is_shortcut"].getBool());
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(createAliasedPath(targetFile_), item["alias_target"].getString());
}

TEST_F(WindowsShortcutTest, FileSystemItemForRegularFileHasNoShortcutFields)
{
   core::json::Object item = createFileSystemItem(targetFile_);
   EXPECT_FALSE(item.hasMember("is_shortcut"));
   EXPECT_FALSE(item.hasMember("alias_target"));
}

TEST_F(WindowsShortcutTest, FileSystemItemForNetworkShortcutKeepsTarget)
{
   // shortcuts to network targets are followed without probing the target
   // during the listing -- stat'ing a disconnected share can block the
   // session for seconds -- so even an unreachable UNC target keeps its
   // alias_target. Directory-ness comes from the attributes stored in the
   // link (none for an unreachable target, so false).
   core::FilePath uncTarget("//pw-nonexistent-host-1f3a/share/data");
   core::FilePath shortcut = makeShortcut(uncTarget, "unc-shortcut.lnk");

   core::json::Object item = createFileSystemItem(shortcut);
   ASSERT_TRUE(item.hasMember("is_shortcut"));
   EXPECT_TRUE(item["is_shortcut"].getBool());
   ASSERT_TRUE(item.hasMember("alias_target"));
   EXPECT_EQ(createAliasedPath(uncTarget), item["alias_target"].getString());
   EXPECT_FALSE(item["dir"].getBool());
}

TEST_F(WindowsShortcutTest, FileSystemItemForBrokenShortcut)
{
   // deleted target: still flagged for the badge, but no alias_target and
   // plain-file dir-ness (resolution succeeds; the exists() check filters it)
   core::FilePath doomed = baseDir_.completeChildPath("doomed.txt");
   core::Error error = doomed.ensureFile();
   ASSERT_FALSE(error) << error.asString();
   core::FilePath shortcut = makeShortcut(doomed, "doomed-shortcut.lnk");
   error = doomed.remove();
   ASSERT_FALSE(error) << error.asString();

   core::json::Object item = createFileSystemItem(shortcut);
   ASSERT_TRUE(item.hasMember("is_shortcut"));
   EXPECT_TRUE(item["is_shortcut"].getBool());
   EXPECT_FALSE(item.hasMember("alias_target"));
   EXPECT_FALSE(item["dir"].getBool());
}

#endif // _WIN32

} // namespace tests
} // namespace module_context
} // namespace session
} // namespace rstudio
