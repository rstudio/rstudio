/*
 * SessionProjectsTests.cpp
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

#include <session/projects/SessionProjects.hpp>

#include "../modules/SessionDiagnostics.hpp"
#include "../../core/system/file_monitor/FileMonitorImpl.hpp"

#include <session/prefs/UserPrefs.hpp>

#include <boost/optional.hpp>

#ifndef _WIN32
#include <unistd.h>
#endif

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/FileScanner.hpp>
#include <core/r_util/RPackageInfo.hpp>

#include <gtest/gtest.h>
#include <git2.h>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace projects {
namespace {

TEST(SessionProjectsTests, IsPackageProjectFalseWithoutProject)
{
   // with no project open, isPackageProject() used to look for DESCRIPTION
   // in the working directory (#19056); this only applies to sessions that
   // have no project open, which is how the test session runs
   if (projectContext().hasProject())
      GTEST_SKIP() << "a project is open";

   FilePath packageDir;
   Error error = FilePath::tempFilePath(packageDir);
   ASSERT_FALSE(error) << error.asString();
   error = packageDir.ensureDirectory();
   ASSERT_FALSE(error) << error.asString();
   error = writeStringToFile(
            packageDir.completeChildPath("DESCRIPTION"),
            "Package: foo\nVersion: 0.1.0\nType: Package\n");
   ASSERT_FALSE(error) << error.asString();

   // declared in reverse order of teardown: restore the working directory
   // before removing the temporary package directory
   RemoveOnExitScope removeScope(packageDir, ERROR_LOCATION);
   RestoreCurrentPathScope restoreScope(FilePath::safeCurrentPath(packageDir), ERROR_LOCATION);

   error = packageDir.makeCurrentPath();
   ASSERT_FALSE(error) << error.asString();
   ASSERT_TRUE(r_util::isPackageDirectory(FilePath::safeCurrentPath(packageDir)));

   EXPECT_FALSE(projectContext().isPackageProject());
}

} // anonymous namespace

class PackageProjectInfoTests : public ::testing::Test
{
protected:
   void SetUp() override
   {
      fileMonitorUseGitignore_ = prefs::userPrefs().fileMonitorUseGitignore();
      ignoredComponents_ = prefs::userPrefs().fileMonitorIgnoredComponents();
      importedPackages_ = r_util::RSourceIndex::getImportedPackages();
      importFromDirectives_ = r_util::RSourceIndex::getImportFromDirectives();
      ASSERT_FALSE(FilePath::tempFilePath(projectDir_));
      ASSERT_FALSE(projectDir_.ensureDirectory());
   }

   void TearDown() override
   {
      EXPECT_FALSE(prefs::userPrefs().setFileMonitorUseGitignore(fileMonitorUseGitignore_));
      EXPECT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignoredComponents_));
      r_util::RSourceIndex::setImportedPackages(importedPackages_);
      r_util::RSourceIndex::setImportFromDirectives(importFromDirectives_);
      EXPECT_FALSE(projectDir_.remove());
   }

   // Unmonitored metadata re-reads DESCRIPTION only when its size or mtime
   // changes, so successive contents written by a test must differ in length.
   void writeDescription(const std::string& contents, const std::string& subdir = "")
   {
      FilePath dir = projectDir_.completeChildPath(subdir);
      ASSERT_FALSE(dir.ensureDirectory());
      ASSERT_FALSE(writeStringToFile(dir.completeChildPath("DESCRIPTION"), contents));
   }

   void openProject(ProjectContext* pContext,
                    const std::string& buildType,
                    const std::string& packagePath = "",
                    bool enableCodeIndexing = false,
                    bool registerMonitor = true)
   {
      FilePath projectFile = projectDir_.completeChildPath("test.Rproj");
      ASSERT_FALSE(writeStringToFile(
            projectFile,
            std::string("Version: 1.0\nEnableCodeIndexing: ") +
            (enableCodeIndexing ? "Yes" : "No") + "\nBuildType: " + buildType +
            "\nPackagePath: " + packagePath + "\n"));
      std::string userError;
      Error error = pContext->startup(projectFile, &userError);
      ASSERT_FALSE(error) << error.asString() << " " << userError;

      // Resolve build paths and metadata without initializing session-wide
      // callbacks or starting a monitor for this temporary project context.
      pContext->setConfig(pContext->config());
      if (enableCodeIndexing && registerMonitor)
         registerFileMonitor(pContext);
   }

   void notifyFileChanged(ProjectContext* pContext,
                          const FilePath& path,
                          core::system::FileChangeEvent::Type type)
   {
      pContext->fileMonitorFilesChanged({
         core::system::FileChangeEvent(type, FileInfo(path.getAbsolutePath(), false))
      });
   }

   bool filterMonitorEntry(ProjectContext* pContext, const FilePath& path)
   {
      return pContext->fileMonitorFilterWithCoverage(
            FileInfo(path), pContext->createFileMonitorFilterContext());
   }

   void notifyDirectoryStatus(ProjectContext* pContext, const FilePath& path, bool failed)
   {
      FileInfo info(path.getAbsolutePath(), true, path.isSymlink());
      info.setScanFailed(failed);
      pContext->fileMonitorFilesChanged({
         core::system::FileChangeEvent(core::system::FileChangeEvent::FileModified, info)
      });
   }

   void invalidateFilteredDirectory(ProjectContext* pContext,
                                    const FilePath& path,
                                    tree<FileInfo>* pFiles)
   {
      ASSERT_FALSE(filterMonitorEntry(pContext, path));
      core::system::file_monitor::impl::invalidateDirectory(
            FileInfo(path),
            pFiles,
            [pContext](const std::vector<core::system::FileChangeEvent>& events)
            {
               pContext->fileMonitorFilesChanged(events);
            });
   }

   void reconcileScanStatus(ProjectContext* pContext, bool failed)
   {
      // The R directory was observed in both snapshots; only an unrelated
      // metadata-read failure changed the containing directory's scan status.
      std::vector<FileInfo> before = {
         FileInfo(projectDir_), FileInfo(projectDir_.completeChildPath("R"))
      };
      before.front().setScanFailed(!failed);
      std::vector<FileInfo> after = before;
      after.front().setScanFailed(failed);
      std::vector<core::system::FileChangeEvent> events;
      core::system::collectFileChangeEvents(
            before.begin(), before.end(), after.begin(), after.end(), &events);
      ASSERT_EQ(1, events.size());
      pContext->fileMonitorFilesChanged(events);
   }

   void augmentBuildIgnores(ProjectContext* pContext)
   {
      pContext->augmentRbuildignore();
   }

   std::size_t unmonitoredSourceCount(ProjectContext* pContext)
   {
      std::lock_guard<std::mutex> lock(pContext->unmonitoredSourcesMutex_);
      return pContext->unmonitoredSources_.size();
   }

   void registerFileMonitor(ProjectContext* pContext, const FilePath& failedDirectory = FilePath())
   {
      pContext->resetFileMonitorCoverage();
      tree<FileInfo> files;
      core::system::FileScannerOptions options;
      options.recursive = true;
      options.onBeforeScanDir = [failedDirectory](const FileInfo& info) -> Error
      {
         if (!failedDirectory.isEmpty() && FilePath(info.absolutePath()) == failedDirectory)
            return systemError(boost::system::errc::no_space_on_device, ERROR_LOCATION);
         return Success();
      };
      FileMonitorFilterContext filter = pContext->createFileMonitorFilterContext();
      options.filter = [pContext, filter](const FileInfo& info)
      {
         return pContext->fileMonitorFilterWithCoverage(info, filter);
      };
      ASSERT_FALSE(core::system::scanFiles(
            FileInfo(pContext->directory().getAbsolutePath(), true),
            options,
            &files));
      pContext->fileMonitorRegistered(core::system::file_monitor::Handle(), files);
   }

   void initializeGit()
   {
      ASSERT_FALSE(prefs::userPrefs().setFileMonitorUseGitignore(true));
      ASSERT_TRUE(prefs::userPrefs().fileMonitorUseGitignore());
      // Match libgit2's workdir spelling (macOS temporary paths can use /var).
      projectDir_ = FilePath(projectDir_.getCanonicalPath());
      git_repository* repo = nullptr;
      ASSERT_EQ(0, git_repository_init(&repo, projectDir_.getAbsolutePath().c_str(), 0));
      git_repository_free(repo);
   }

   FilePath projectDir_;
   bool fileMonitorUseGitignore_;
   json::Array ignoredComponents_;
   std::set<std::string> importedPackages_;
   r_util::RSourceIndex::ImportFromMap importFromDirectives_;
};

TEST_F(PackageProjectInfoTests, ReadsPackageWithNonPackageBuildTypes)
{
   writeDescription(R"(Package: localpkg
Version: 0.1.0
Imports: utils
LinkingTo: Rcpp
)");

   for (const std::string& buildType : {"None", "Makefile", "Custom"})
   {
      SCOPED_TRACE(buildType);
      ProjectContext context;
      openProject(&context, buildType);
      r_util::RProjectConfig config = context.config();
      config.makefilePath = "build";
      config.customScriptPath = "build.sh";
      context.setConfig(config);
      ASSERT_TRUE(context.isPackageProject());
      EXPECT_EQ("localpkg", context.packageInfo().name());
      EXPECT_EQ("utils", context.packageInfo().imports());
      EXPECT_EQ("Rcpp", context.packageInfo().linkingTo());
   }
}

TEST_F(PackageProjectInfoTests, RecognizesPackagePathBeforeFileMonitoring)
{
   writeDescription("Package: nestedpkg\nVersion: 0.1.0\n", "pkg");
   ProjectContext context;
   openProject(&context, "Package", "pkg");

   EXPECT_EQ(projectDir_.completeChildPath("pkg"), context.buildTargetPath());
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("nestedpkg", context.packageInfo().name());
}

TEST_F(PackageProjectInfoTests, RefreshesMetadataWithoutFileMonitoring)
{
   writeDescription("Package: before\nVersion: 0.1.0\nImports: utils\nLinkingTo: Rcpp\n");
   ProjectContext context;
   openProject(&context, "Package");
   ASSERT_FALSE(context.hasFileMonitor());
   ASSERT_EQ("utils", context.packageInfo().imports());

   writeDescription("Package: after\nVersion: 0.2.0\n");
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("after", context.packageInfo().name());
   EXPECT_EQ("0.2.0", context.packageInfo().version());
   EXPECT_TRUE(context.packageInfo().imports().empty());
   EXPECT_TRUE(context.packageInfo().linkingTo().empty());
}

TEST_F(PackageProjectInfoTests, ClearsMetadataForInvalidOrRemovedDescription)
{
   writeDescription("Package: valid\nVersion: 0.1.0\n");
   ProjectContext context;
   openProject(&context, "Package");
   ASSERT_TRUE(context.isPackageProject());

   writeDescription("Package: missingversion\n");
   EXPECT_FALSE(context.isPackageProject());
   EXPECT_TRUE(context.packageInfo().empty());

   writeDescription("Package: \nVersion: 0.1.0\n");
   EXPECT_FALSE(context.isPackageProject());
   EXPECT_TRUE(context.packageInfo().empty());

   writeDescription("Package: restored\nVersion: 0.2.0\n");
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("restored", context.packageInfo().name());

   ASSERT_FALSE(projectDir_.completeChildPath("DESCRIPTION").remove());
   EXPECT_FALSE(context.isPackageProject());
   EXPECT_TRUE(context.packageInfo().empty());
}

TEST_F(PackageProjectInfoTests, ChangesPackageDirectoryWithBuildConfiguration)
{
   writeDescription("Package: rootpkg\nVersion: 0.1.0\nImports: utils\n");
   writeDescription("Package: nestedpkg\nVersion: 0.2.0\n", "pkg");
   ProjectContext context;
   openProject(&context, "None");
   EXPECT_EQ("rootpkg", context.packageInfo().name());

   r_util::RProjectConfig config = context.config();
   config.buildType = r_util::kBuildTypePackage;
   config.packagePath = "pkg";
   context.setConfig(config);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("nestedpkg", context.packageInfo().name());
   EXPECT_TRUE(context.packageInfo().imports().empty());

   config.buildType = r_util::kBuildTypeNone;
   context.setConfig(config);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("rootpkg", context.packageInfo().name());
   EXPECT_EQ("utils", context.packageInfo().imports());
}

TEST_F(PackageProjectInfoTests, RefreshesSharedMetadataWhenDescriptionChanges)
{
   writeDescription("Package: before\nVersion: 0.1.0\nImports: utils\n");
   ProjectContext context;
   openProject(&context, "None", "", true);
   ASSERT_TRUE(context.hasFileMonitor());
   ASSERT_TRUE(context.isPackageProject());
   ASSERT_EQ("before", context.packageInfo().name());

   writeDescription("Package: after\nVersion: 0.2.0\n");
   notifyFileChanged(
         &context,
         projectDir_.completeChildPath("DESCRIPTION"),
         core::system::FileChangeEvent::FileModified);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("after", context.packageInfo().name());
   EXPECT_TRUE(context.packageInfo().imports().empty());

   ASSERT_FALSE(projectDir_.completeChildPath("DESCRIPTION").remove());
   notifyFileChanged(
         &context,
         projectDir_.completeChildPath("DESCRIPTION"),
         core::system::FileChangeEvent::FileRemoved);
   EXPECT_FALSE(context.isPackageProject());
   EXPECT_TRUE(context.packageInfo().empty());
}

TEST_F(PackageProjectInfoTests, RefreshesDescriptionBeforeNotifyingMonitorSubscribers)
{
   ProjectContext context;
   openProject(&context, "None", "", true, false);
   ASSERT_TRUE(context.hasFileMonitor());
   ASSERT_FALSE(context.isPackageProject());

   // A file created after the initial read can appear only in the initial
   // monitor snapshot, without a later file-added event to refresh the cache.
   writeDescription("Package: created\nVersion: 0.1.0\n");
   bool notified = false;
   FileMonitorCallbacks callbacks;
   callbacks.onMonitoringEnabled = [&](const tree<FileInfo>&)
   {
      notified = true;
      EXPECT_TRUE(context.isPackageProject());
      EXPECT_EQ("created", context.packageInfo().name());
      EXPECT_TRUE(projectDir_.completeChildPath(".Rbuildignore").exists());
   };
   context.subscribeToFileMonitor("Test", callbacks);
   registerFileMonitor(&context);
   EXPECT_TRUE(notified);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("created", context.packageInfo().name());
}

TEST_F(PackageProjectInfoTests, RespectsDescriptionType)
{
   writeDescription("Package: book\nVersion: 0.1.0\nType: Book\n");
   ProjectContext context;
   openProject(&context, "None");
   EXPECT_FALSE(context.isPackageProject());
}

TEST_F(PackageProjectInfoTests, AugmentsBuildIgnoresWithDotPackagePath)
{
   writeDescription("Package: rootpkg\nVersion: 0.1.0\n");
   ProjectContext context;
   openProject(&context, "Package", ".");
   augmentBuildIgnores(&context);

   std::string contents;
   ASSERT_FALSE(readStringFromFile(projectDir_.completeChildPath(".Rbuildignore"), &contents));
   EXPECT_NE(std::string::npos, contents.find(R"(^.*\.Rproj$)"));
   EXPECT_NE(std::string::npos, contents.find(R"(^\.Rproj\.user$)"));
}

TEST_F(PackageProjectInfoTests, DoesNotAugmentBuildIgnoresForSeparatePackagePath)
{
   writeDescription("Package: nestedpkg\nVersion: 0.1.0\n", "pkg");
   ProjectContext context;
   openProject(&context, "Package", "pkg");
   augmentBuildIgnores(&context);

   EXPECT_FALSE(projectDir_.completeChildPath(".Rbuildignore").exists());
   EXPECT_FALSE(projectDir_.completeChildPath("pkg/.Rbuildignore").exists());
}

TEST_F(PackageProjectInfoTests, RefreshesNamespaceImportsWhenPackagePathChanges)
{
   writeDescription("Package: first\nVersion: 0.1.0\n", "first");
   writeDescription("Package: second\nVersion: 0.1.0\n", "second");
   writeDescription("Package: third\nVersion: 0.1.0\n", "third");
   ASSERT_FALSE(writeStringToFile(
         projectDir_.completeChildPath("first/NAMESPACE"),
         "import(compiler)\nimportFrom(utils, head)\n"));
   ASSERT_FALSE(writeStringToFile(
         projectDir_.completeChildPath("second/NAMESPACE"),
         "import(splines)\nimportFrom(stats, median)\n"));

   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);

   r_util::RProjectConfig config = context.config();
   config.buildType = r_util::kBuildTypePackage;
   config.packagePath = "first";
   context.setConfig(config);
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
   EXPECT_EQ(1u, r_util::RSourceIndex::getImportFromDirectives().at("utils").count("head"));

   config.packagePath = "second";
   context.setConfig(config);
   EXPECT_EQ(std::set<std::string>{"splines"}, r_util::RSourceIndex::getImportedPackages());
   EXPECT_EQ(0u, r_util::RSourceIndex::getImportFromDirectives().count("utils"));
   EXPECT_EQ(1u, r_util::RSourceIndex::getImportFromDirectives().at("stats").count("median"));

   config.packagePath = "third";
   context.setConfig(config);
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());
}

TEST_F(PackageProjectInfoTests, RefreshesDescriptionEventsWithDotPackagePath)
{
   writeDescription("Package: before\nVersion: 0.1.0\n");
   ProjectContext context;
   openProject(&context, "Package", ".", true);
   ASSERT_TRUE(context.hasFileMonitor());
   ASSERT_EQ("before", context.packageInfo().name());

   FilePath description = projectDir_.completeChildPath("DESCRIPTION");
   writeDescription("Package: after\nVersion: 0.2.0\n");
   notifyFileChanged(&context, description, core::system::FileChangeEvent::FileModified);
   EXPECT_EQ("after", context.packageInfo().name());

   ASSERT_FALSE(description.remove());
   notifyFileChanged(&context, description, core::system::FileChangeEvent::FileRemoved);
   EXPECT_FALSE(context.isPackageProject());
   EXPECT_TRUE(context.packageInfo().empty());
}

TEST_F(PackageProjectInfoTests, RefreshesNamespaceEventsWithDotPackagePath)
{
   writeDescription("Package: rootpkg\nVersion: 0.1.0\n");
   ProjectContext context;
   openProject(&context, "Package", ".", true);
   modules::diagnostics::monitorProjectNamespace(context);

   FilePath namespaceFile = projectDir_.completeChildPath("NAMESPACE");
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(compiler)\nimportFrom(utils, head)\n"));
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileAdded);
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
   EXPECT_EQ(1u, r_util::RSourceIndex::getImportFromDirectives().at("utils").count("head"));

   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(splines)\nimportFrom(stats, median)\n"));
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileModified);
   EXPECT_EQ(std::set<std::string>{"splines"}, r_util::RSourceIndex::getImportedPackages());
   EXPECT_EQ(0u, r_util::RSourceIndex::getImportFromDirectives().count("utils"));
   EXPECT_EQ(1u, r_util::RSourceIndex::getImportFromDirectives().at("stats").count("median"));

   ASSERT_FALSE(namespaceFile.remove());
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileRemoved);
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());
}

TEST_F(PackageProjectInfoTests, KeepsMetadataWithoutCodeIndexing)
{
   writeDescription("Package: before\nVersion: 0.1.0\n");
   ASSERT_FALSE(writeStringToFile(
         projectDir_.completeChildPath("NAMESPACE"),
         "import(splines)\nimportFrom(stats, median)\n"));
   ProjectContext context;
   openProject(&context, "None");
   modules::diagnostics::monitorProjectNamespace(context);
   ASSERT_FALSE(context.hasFileMonitor());
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

   r_util::RSourceIndex::setImportedPackages({});
   r_util::RSourceIndex::setImportFromDirectives({});
   notifyFileChanged(
         &context,
         projectDir_.completeChildPath("NAMESPACE"),
         core::system::FileChangeEvent::FileModified);
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());

   writeDescription("Package: after\nVersion: 0.2.0\n");
   EXPECT_EQ("after", context.packageInfo().name());
}

TEST_F(PackageProjectInfoTests, KeepsMetadataForExcludedPackageDirectories)
{
   initializeGit();
   ASSERT_FALSE(writeStringToFile(
         projectDir_.completeChildPath(".gitignore"),
         "/pkg/\n/ignored/\n/ignored-r/R/\n"));

   ASSERT_FALSE(projectDir_.completeChildPath("build").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("build/CMakeCache.txt"), "cache"));
   for (const std::string& packagePath : {"pkg", "ignored/pkg", ".hidden/pkg", "node_modules/pkg", "ignored-r", "build/pkg"})
   {
      SCOPED_TRACE(packagePath);
      writeDescription("Package: before\nVersion: 0.1.0\n", packagePath);
      FilePath packageDir = projectDir_.completeChildPath(packagePath);
      ASSERT_FALSE(packageDir.completeChildPath("R").ensureDirectory());
      ASSERT_FALSE(writeStringToFile(
            packageDir.completeChildPath("NAMESPACE"),
            "import(splines)\nimportFrom(stats, median)\n"));
      ProjectContext context;
      openProject(&context, "Package", packagePath, true);
      modules::diagnostics::monitorProjectNamespace(context);
      EXPECT_TRUE(context.hasFileMonitor());
      EXPECT_TRUE(context.isPackageProject());
      EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

      r_util::RSourceIndex::setImportedPackages({});
      r_util::RSourceIndex::setImportFromDirectives({});
      notifyFileChanged(
            &context,
            packageDir.completeChildPath("NAMESPACE"),
            core::system::FileChangeEvent::FileModified);
      EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
      EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());

      writeDescription("Package: after\nVersion: 0.2.0\n", packagePath);
      EXPECT_EQ("after", context.packageInfo().name());
   }

   ASSERT_FALSE(prefs::userPrefs().setFileMonitorUseGitignore(false));
   ProjectContext context;
   openProject(&context, "Package", "pkg", true);
   EXPECT_EQ(projectDir_.completeChildPath("pkg"), context.packageDirectoryInProject());
}

TEST_F(PackageProjectInfoTests, ExcludesIgnoredRDirectoryContents)
{
   json::Array ignores;
   ignores.push_back("/R/");
   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignores));
   writeDescription("Package: before\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   writeDescription("Package: after\nVersion: 0.2.0\n");
   EXPECT_EQ("after", context.packageInfo().name());
}

TEST_F(PackageProjectInfoTests, ExcludedHelperWaitsForSourceCoverage)
{
   json::Array ignores;
   ignores.push_back("/R/helper.R");
   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignores));
   writeDescription("Package: before\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   FilePath helper = projectDir_.completeChildPath("R/helper.R");
   ASSERT_FALSE(writeStringToFile(helper, "helper <- function() TRUE\n"));
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("NAMESPACE"), "import(compiler)\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   writeDescription("Package: after\nVersion: 0.2.0\n");
   EXPECT_EQ("after", context.packageInfo().name());

   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(json::Array()));
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   registerFileMonitor(&context);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, NewlyFilteredHelperRefreshesOnNextMonitorBatch)
{
   json::Array ignores;
   ignores.push_back("/R/helper.R");
   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignores));
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   FilePath rDirectory = projectDir_.completeChildPath("R");
   ASSERT_FALSE(rDirectory.ensureDirectory());
   ProjectContext context;
   openProject(&context, "None", "", true);
   ASSERT_EQ(projectDir_, context.packageDirectoryInProject());

   // The monitor thread rejects the helper while scanning its parent. The
   // delivered batch carries no entry for it, yet still refreshes the scope.
   FilePath helper = rDirectory.completeChildPath("helper.R");
   ASSERT_FALSE(writeStringToFile(helper, "helper <- function() TRUE\n"));
   ASSERT_FALSE(filterMonitorEntry(&context, helper));
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   notifyDirectoryStatus(&context, rDirectory, false);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

   // Ignored removals need not reach the subscriber either.
   ASSERT_FALSE(helper.remove());
   notifyDirectoryStatus(&context, rDirectory, false);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());

   ASSERT_FALSE(writeStringToFile(helper, "helper <- function() TRUE\n"));
   ASSERT_FALSE(filterMonitorEntry(&context, helper));
   notifyFileChanged(&context, helper, core::system::FileChangeEvent::FileRemoved);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
}

TEST_F(PackageProjectInfoTests, ExcludedSourceSubdirectoryDisablesPackageCoverage)
{
   json::Array ignores;
   ignores.push_back("/R/private");
   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignores));
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R/private").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("R/private/helper.R"), "helper <- function() TRUE\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
}

TEST_F(PackageProjectInfoTests, FilteredWorktreeChildDoesNotDisablePackageCoverage)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   FilePath child = projectDir_.completeChildPath(".worktrees/feature--foo/R/helper.R");
   ASSERT_FALSE(child.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(child, "helper <- function() TRUE\n"));
   FilePath namespaceFile = projectDir_.completeChildPath("NAMESPACE");
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(compiler)\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileModified);
   ASSERT_EQ(projectDir_, context.packageDirectoryInProject());
   ASSERT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());

   // Windows can deliver child events even inside an excluded directory.
   ASSERT_FALSE(filterMonitorEntry(&context, child));
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, FilteringObservedNonSourceDirectoryKeepsPackageCoverage)
{
   initializeGit();
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   FilePath scripts = projectDir_.completeChildPath("scripts");
   ASSERT_FALSE(scripts.ensureDirectory());
   ASSERT_FALSE(writeStringToFile(scripts.completeChildPath("helper.R"), "helper <- function() TRUE\n"));
   FilePath namespaceFile = projectDir_.completeChildPath("NAMESPACE");
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(compiler)\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileModified);
   ASSERT_EQ(projectDir_, context.packageDirectoryInProject());

   tree<FileInfo> files;
   core::system::FileScannerOptions options;
   options.recursive = true;
   ASSERT_FALSE(core::system::scanFiles(FileInfo(projectDir_), options, &files));
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath(".git/info/exclude"), "/scripts/\n"));
   invalidateFilteredDirectory(&context, scripts, &files);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, IgnoredNonSourceFileDoesNotDisablePackageCoverage)
{
   json::Array ignores;
   ignores.push_back("/R/notes.txt");
   ASSERT_FALSE(prefs::userPrefs().setFileMonitorIgnoredComponents(ignores));
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("R/notes.txt"), "notes"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
}

#ifndef _WIN32
TEST_F(PackageProjectInfoTests, LinkedNonSourceFixtureKeepsPackageCoverage)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(projectDir_.completeChildPath("tests/fixtures").ensureDirectory());
   FilePath external;
   ASSERT_FALSE(FilePath::tempFilePath(external));
   ASSERT_FALSE(writeStringToFile(external, "x,y\n1,2\n"));
   RemoveOnExitScope removeExternal(external, ERROR_LOCATION);
   FilePath fixture = projectDir_.completeChildPath("tests/fixtures/data.csv");
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(), fixture.getAbsolutePath().c_str()));
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());

   // A target can become a directory without any project monitor event; the
   // retained link observation is checked again on the next batch.
   FilePath fixtures = fixture.getParent();
   ASSERT_FALSE(external.remove());
   ASSERT_FALSE(external.ensureDirectory());
   ASSERT_FALSE(writeStringToFile(external.completeChildPath("helper.R"), "helper <- function() TRUE\n"));
   notifyDirectoryStatus(&context, fixtures, false);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   ASSERT_FALSE(external.remove());
   ASSERT_FALSE(writeStringToFile(external, "x,y\n1,2\n"));
   notifyDirectoryStatus(&context, fixtures, false);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
}

TEST_F(PackageProjectInfoTests, LinkedScriptOutsideSourceDirectoriesKeepsPackageCoverage)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(projectDir_.completeChildPath("inst/examples").ensureDirectory());
   ASSERT_FALSE(projectDir_.completeChildPath("tests/testthat").ensureDirectory());
   FilePath external;
   ASSERT_FALSE(FilePath::tempFilePath(external));
   ASSERT_FALSE(writeStringToFile(external, "helper <- function() TRUE\n"));
   RemoveOnExitScope removeExternal(external, ERROR_LOCATION);

   // Only R/ and tests/ feed package diagnostics; a linked example script
   // elsewhere in the package is not a reason to fall back to the search path.
   FilePath demo = projectDir_.completeChildPath("inst/examples/demo.R");
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(), demo.getAbsolutePath().c_str()));
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());

   FilePath helper = projectDir_.completeChildPath("tests/testthat/helper-linked.R");
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(), helper.getAbsolutePath().c_str()));
   registerFileMonitor(&context);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
}

TEST_F(PackageProjectInfoTests, LinkedHelperUsesFallbackUntilReplacedByObservedSource)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   FilePath external;
   ASSERT_FALSE(FilePath::tempFilePath(external));
   ASSERT_FALSE(writeStringToFile(external, "helper <- function() TRUE\n"));
   RemoveOnExitScope removeExternal(external, ERROR_LOCATION);
   FilePath helper = projectDir_.completeChildPath("R/helper.R");
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(), helper.getAbsolutePath().c_str()));
   ProjectContext context;
   openProject(&context, "None", "", true);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

   ASSERT_EQ(0, ::unlink(helper.getAbsolutePath().c_str()));
   ASSERT_FALSE(writeStringToFile(helper, "helper <- function() TRUE\n"));
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   notifyFileChanged(&context, helper, core::system::FileChangeEvent::FileModified);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());

   ASSERT_FALSE(helper.remove());
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(), helper.getAbsolutePath().c_str()));
   notifyFileChanged(&context, helper, core::system::FileChangeEvent::FileModified);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
}
#endif

TEST_F(PackageProjectInfoTests, UnignoredPackageWaitsForMonitorCoverage)
{
   initializeGit();
   writeDescription("Package: before\nVersion: 0.1.0\n", "pkg");
   FilePath packageDir = projectDir_.completeChildPath("pkg");
   ASSERT_FALSE(packageDir.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(packageDir.completeChildPath("NAMESPACE"), "import(compiler)\n"));
   FilePath ignoreFile = projectDir_.completeChildPath(".gitignore");
   ASSERT_FALSE(writeStringToFile(ignoreFile, "/pkg/\n"));
   ProjectContext context;
   openProject(&context, "Package", "pkg", true);
   modules::diagnostics::monitorProjectNamespace(context);
   r_util::RSourceIndex::setImportedPackages({});
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

   ASSERT_FALSE(writeStringToFile(ignoreFile, "# now included\n"));
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   writeDescription("Package: after\nVersion: 0.2.0\n", "pkg");
   EXPECT_EQ("after", context.packageInfo().name());
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());

   registerFileMonitor(&context);
   EXPECT_EQ(packageDir, context.packageDirectoryInProject());
   EXPECT_EQ("after", context.packageInfo().name());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, RemovedCMakeMarkerWaitsForFreshIndexSnapshot)
{
   writeDescription("Package: before\nVersion: 0.1.0\n", "build/pkg");
   FilePath packageDir = projectDir_.completeChildPath("build/pkg");
   ASSERT_FALSE(packageDir.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(packageDir.completeChildPath("NAMESPACE"), "import(compiler)\n"));
   FilePath marker = projectDir_.completeChildPath("build/CMakeCache.txt");
   ASSERT_FALSE(writeStringToFile(marker, "cache"));
   ProjectContext context;
   openProject(&context, "Package", "build/pkg", true);
   modules::diagnostics::monitorProjectNamespace(context);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());

   ASSERT_FALSE(marker.remove());
   notifyFileChanged(&context, marker, core::system::FileChangeEvent::FileRemoved);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   writeDescription("Package: after\nVersion: 0.2.0\n", "build/pkg");
   EXPECT_EQ("after", context.packageInfo().name());

   registerFileMonitor(&context);
   EXPECT_EQ(packageDir, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, FailedDirectoryRegistrationDisablesPackageCoverage)
{
   writeDescription("Package: before\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R/sub").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("R/sub/helper.R"), "helper <- function() TRUE\n"));
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("NAMESPACE"), "import(compiler)\n"));

   for (const std::string& path : {"R", "R/sub"})
   {
      SCOPED_TRACE(path);
      ProjectContext context;
      openProject(&context, "None", "", true, false);
      modules::diagnostics::monitorProjectNamespace(context);
      registerFileMonitor(&context, projectDir_.completeChildPath(path));
      EXPECT_TRUE(context.hasFileMonitor());
      EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
      EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());

      writeDescription("Package: after\nVersion: 0.2.0\n");
      EXPECT_EQ("after", context.packageInfo().name());
      registerFileMonitor(&context);
      EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
      EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
   }
}

TEST_F(PackageProjectInfoTests, FailedNonSourceDirectoryKeepsPackageCoverage)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("R/helper.R"), "helper <- function() TRUE\n"));
   FilePath data = projectDir_.completeChildPath("data");
   ASSERT_FALSE(data.ensureDirectory());
   ProjectContext context;
   openProject(&context, "None", "", true, false);

   // With PackagePath: . an unreadable data/ would otherwise disable package
   // diagnostics for the whole project, the #19067 symptom.
   registerFileMonitor(&context, data);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   notifyDirectoryStatus(&context, data, true);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   notifyDirectoryStatus(&context, projectDir_.completeChildPath("R"), true);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
}

TEST_F(PackageProjectInfoTests, PrunesRemovedUnmonitoredEntries)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ProjectContext context;
   openProject(&context, "None", "", true);
   // The initial scan already records filtered directories such as .Rproj.user.
   std::size_t baseline = unmonitoredSourceCount(&context);

   // Rejected paths are recorded by the monitor thread and only ever replaced
   // by an accepted event for the same path, so deleted ones must be pruned.
   FilePath scratch = projectDir_.completeChildPath(".scratch");
   ASSERT_FALSE(scratch.ensureDirectory());
   ASSERT_FALSE(filterMonitorEntry(&context, scratch));
   EXPECT_EQ(baseline + 1, unmonitoredSourceCount(&context));
   notifyDirectoryStatus(&context, projectDir_, false);
   EXPECT_EQ(baseline + 1, unmonitoredSourceCount(&context));

   ASSERT_FALSE(scratch.remove());
   notifyDirectoryStatus(&context, projectDir_, false);
   EXPECT_EQ(baseline, unmonitoredSourceCount(&context));
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
}

TEST_F(PackageProjectInfoTests, PartialScanFailureRetainsHealthyDescendantCoverage)
{
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("R/helper.R"), "helper <- function() TRUE\n"));
   ASSERT_FALSE(writeStringToFile(projectDir_.completeChildPath("NAMESPACE"), "import(compiler)\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   ASSERT_EQ(projectDir_, context.packageDirectoryInProject());

   reconcileScanStatus(&context, true);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());

   reconcileScanStatus(&context, false);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, RefreshesImportsAcrossIgnoreRuleChanges)
{
   initializeGit();
   writeDescription("Package: localpkg\nVersion: 0.1.0\n");
   ASSERT_FALSE(projectDir_.completeChildPath("R").ensureDirectory());
   FilePath namespaceFile = projectDir_.completeChildPath("NAMESPACE");
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(compiler)\n"));
   FilePath ignoreFile = projectDir_.completeChildPath(".gitignore");
   ASSERT_FALSE(writeStringToFile(ignoreFile, "# included\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileModified);
   ASSERT_EQ(std::set<std::string>{"compiler"}, r_util::RSourceIndex::getImportedPackages());

   ASSERT_FALSE(writeStringToFile(ignoreFile, "/R/\n"));
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(splines)\n"));
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileModified);
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());

   ASSERT_FALSE(writeStringToFile(ignoreFile, "# included again\n"));
   notifyFileChanged(&context, ignoreFile, core::system::FileChangeEvent::FileModified);
   EXPECT_EQ(projectDir_, context.packageDirectoryInProject());
   EXPECT_EQ(std::set<std::string>{"splines"}, r_util::RSourceIndex::getImportedPackages());
}

TEST_F(PackageProjectInfoTests, ExternalPackageKeepsMetadataWithoutPackageDiagnostics)
{
   FilePath externalDir;
   ASSERT_FALSE(FilePath::tempFilePath(externalDir));
   ASSERT_FALSE(externalDir.ensureDirectory());
   RemoveOnExitScope removeScope(externalDir, ERROR_LOCATION);
   ASSERT_FALSE(writeStringToFile(
         externalDir.completeChildPath("DESCRIPTION"),
         "Package: externalpkg\nVersion: 0.1.0\n"));
   ASSERT_FALSE(writeStringToFile(
         externalDir.completeChildPath("NAMESPACE"),
         "import(splines)\nimportFrom(stats, median)\n"));

   writeDescription("Package: rootpkg\nVersion: 0.1.0\n");
   FilePath namespaceFile = projectDir_.completeChildPath("NAMESPACE");
   ASSERT_FALSE(writeStringToFile(namespaceFile, "import(compiler)\nimportFrom(utils, head)\n"));
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);
   notifyFileChanged(&context, namespaceFile, core::system::FileChangeEvent::FileAdded);
   ASSERT_FALSE(r_util::RSourceIndex::getImportedPackages().empty());
   ASSERT_FALSE(r_util::RSourceIndex::getImportFromDirectives().empty());

   r_util::RProjectConfig config = context.config();
   config.buildType = r_util::kBuildTypePackage;
   config.packagePath = externalDir.getAbsolutePath();
   context.setConfig(config);
   EXPECT_TRUE(context.isPackageProject());
   EXPECT_EQ("externalpkg", context.packageInfo().name());
   EXPECT_EQ(externalDir, context.packageDirectory());
   EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());

   // External files are not monitored by the project. Even an explicit event
   // must not populate imports for a directory outside the source index.
   notifyFileChanged(
         &context,
         externalDir.completeChildPath("NAMESPACE"),
         core::system::FileChangeEvent::FileModified);
   EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
   EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());
}

#ifndef _WIN32

TEST_F(PackageProjectInfoTests, LinkedPackageKeepsMetadataWithoutPackageDiagnostics)
{
   FilePath externalDir;
   ASSERT_FALSE(FilePath::tempFilePath(externalDir));
   ASSERT_FALSE(externalDir.ensureDirectory());
   RemoveOnExitScope removeScope(externalDir, ERROR_LOCATION);
   FilePath packageDir = externalDir.completeChildPath("pkg");
   ASSERT_FALSE(packageDir.ensureDirectory());
   ASSERT_FALSE(writeStringToFile(
         packageDir.completeChildPath("NAMESPACE"),
         "import(splines)\nimportFrom(stats, median)\n"));

   ASSERT_EQ(0, ::symlink(
         packageDir.getAbsolutePath().c_str(),
         projectDir_.completeChildPath("pkg-link").getAbsolutePath().c_str()));
   ASSERT_EQ(0, ::symlink(
         externalDir.getAbsolutePath().c_str(),
         projectDir_.completeChildPath("parent-link").getAbsolutePath().c_str()));

   FilePath subdir = externalDir.completeChildPath("subdir");
   ASSERT_FALSE(subdir.ensureDirectory());
   ASSERT_EQ(0, ::symlink(
         subdir.getAbsolutePath().c_str(),
         projectDir_.completeChildPath("up-link").getAbsolutePath().c_str()));
   ASSERT_FALSE(writeStringToFile(
         externalDir.completeChildPath("NAMESPACE"),
         "import(splines)\nimportFrom(stats, median)\n"));

   // Both normalized destinations exist, but refer to different packages.
   writeDescription("Package: rootpkg\nVersion: 0.1.0\n");
   writeDescription("Package: decoypkg\nVersion: 0.1.0\n", "pkg");
   ProjectContext context;
   openProject(&context, "None", "", true);
   modules::diagnostics::monitorProjectNamespace(context);

   for (const std::string& packagePath : {"pkg-link", "parent-link/pkg", "up-link/../pkg", "up-link/.."})
   {
      SCOPED_TRACE(packagePath);
      FilePath actualDir = packagePath == "up-link/.." ? externalDir : packageDir;
      FilePath description = actualDir.completeChildPath("DESCRIPTION");
      ASSERT_FALSE(writeStringToFile(description, "Package: linkedpkg\nVersion: 0.1.0\n"));
      r_util::RProjectConfig config = context.config();
      config.buildType = r_util::kBuildTypePackage;
      config.packagePath = packagePath;
      context.setConfig(config);
      EXPECT_TRUE(context.isPackageProject());
      EXPECT_EQ("linkedpkg", context.packageInfo().name());
      EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
      EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
      EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());

      notifyFileChanged(
            &context,
            context.packageDirectory().completeChildPath("NAMESPACE"),
            core::system::FileChangeEvent::FileModified);
      EXPECT_TRUE(r_util::RSourceIndex::getImportedPackages().empty());
      EXPECT_TRUE(r_util::RSourceIndex::getImportFromDirectives().empty());

      // Linked directories have no watch, so metadata must refresh on demand.
      ASSERT_FALSE(writeStringToFile(description, "Package: updatedpkg\nVersion: 0.2.0\n"));
      EXPECT_EQ("updatedpkg", context.packageInfo().name());
   }
}

TEST_F(PackageProjectInfoTests, DoesNotCacheLinkedPackageContents)
{
   FilePath externalDir;
   ASSERT_FALSE(FilePath::tempFilePath(externalDir));
   ASSERT_FALSE(externalDir.ensureDirectory());
   RemoveOnExitScope removeScope(externalDir, ERROR_LOCATION);
   ASSERT_FALSE(externalDir.completeChildPath("R").ensureDirectory());
   ASSERT_FALSE(writeStringToFile(externalDir.completeChildPath("NAMESPACE"), "import(compiler)\n"));
   ASSERT_FALSE(writeStringToFile(
         externalDir.completeChildPath("DESCRIPTION"),
         "Package: before\nVersion: 0.1.0\n"));

   for (const std::string& name : {"R", "DESCRIPTION", "NAMESPACE"})
   {
      SCOPED_TRACE(name);
      writeDescription("Package: before\nVersion: 0.1.0\n");
      FilePath path = projectDir_.completeChildPath(name);
      ASSERT_FALSE(path.removeIfExists());
      ASSERT_EQ(0, ::symlink(
            externalDir.completeChildPath(name).getAbsolutePath().c_str(),
            path.getAbsolutePath().c_str()));
      ProjectContext context;
      openProject(&context, "None", "", true);
      EXPECT_TRUE(context.isPackageProject());
      EXPECT_TRUE(context.packageDirectoryInProject().isEmpty());
      writeDescription("Package: after\nVersion: 0.2.0\n");
      EXPECT_EQ("after", context.packageInfo().name());
      ASSERT_FALSE(path.remove());
   }
}

TEST_F(PackageProjectInfoTests, AllowsLinkedProjectRoot)
{
   writeDescription("Package: rootpkg\nVersion: 0.1.0\n");
   ProjectContext originalContext;
   openProject(&originalContext, "None", "", true);

   FilePath linkedRoot = projectDir_.completeChildPath("root-link");
   ASSERT_EQ(0, ::symlink(projectDir_.getAbsolutePath().c_str(), linkedRoot.getAbsolutePath().c_str()));
   ProjectContext linkedContext;
   std::string userError;
   Error error = linkedContext.startup(linkedRoot.completeChildPath("test.Rproj"), &userError);
   ASSERT_FALSE(error) << error.asString() << " " << userError;
   linkedContext.setConfig(linkedContext.config());

   EXPECT_TRUE(linkedContext.isPackageProject());
   EXPECT_EQ("rootpkg", linkedContext.packageInfo().name());
   EXPECT_EQ(linkedRoot, linkedContext.packageDirectoryInProject());
   registerFileMonitor(&linkedContext);
   notifyDirectoryStatus(&linkedContext, linkedRoot, true);
   EXPECT_TRUE(linkedContext.packageDirectoryInProject().isEmpty());
   notifyDirectoryStatus(&linkedContext, linkedRoot, false);
   EXPECT_EQ(linkedRoot, linkedContext.packageDirectoryInProject());
}

#endif

namespace {

TEST(SessionProjectsTests, ResolveWrittenEditorThemePreservesWhenKeyOmitted)
{
   // No editor_theme key in the request -> keep the existing on-disk value.
   core::json::Object configJson;
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Cobalt");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemeEmptyStringClearsOverride)
{
   // Explicit "" from the Appearance pane's (Default) -> clear the override.
   core::json::Object configJson;
   configJson["editor_theme"] = "";
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemeNonEmptySetsValue)
{
   // Non-empty value present -> use it.
   core::json::Object configJson;
   configJson["editor_theme"] = "Monokai";
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Monokai");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemePreservesWhenValueNotString)
{
   // Present but not a string (malformed request) -> preserve existing, like a
   // missing key (and a warning is logged).
   core::json::Object configJson;
   configJson["editor_theme"] = 42;
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Cobalt");
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsLocalOverrideWins)
{
   // An explicit per-project override wins over the global preference and
   // detection, in both directions.
   EXPECT_TRUE(resolveReduceRemoteFilesystemOperations(true, false, false));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(false, true, true));
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsGlobalOffShortCircuits)
{
   // No override + global preference disabled -> never reduce, regardless of
   // whether a remote filesystem was detected.
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, false, false));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, false, true));
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsGlobalOnGatedByDetection)
{
   // No override + global preference enabled (automatic) -> reduce only when a
   // remote filesystem was detected.
   EXPECT_TRUE(resolveReduceRemoteFilesystemOperations(boost::none, true, true));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, true, false));
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideReadsBool)
{
   boost::optional<bool> enabled = parseReduceRemoteFilesystemOperationsOverride(
      "{\"reduce_remote_filesystem_operations\": true}");
   ASSERT_TRUE(enabled.has_value());
   EXPECT_TRUE(*enabled);

   boost::optional<bool> disabled = parseReduceRemoteFilesystemOperationsOverride(
      "{\"reduce_remote_filesystem_operations\": false}");
   ASSERT_TRUE(disabled.has_value());
   EXPECT_FALSE(*disabled);
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideAbsentKey)
{
   // Key omitted -> no override (fall back to the global preference).
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"some_other_pref\": true}").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideWrongType)
{
   // Present but not a boolean -> no override.
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"reduce_remote_filesystem_operations\": \"yes\"}").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideNonObject)
{
   // Valid JSON but not an object -> no override.
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride("[1, 2, 3]").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideMalformed)
{
   // Corrupt/truncated JSON -> no override (and the parse error is logged).
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"reduce_remote_filesystem_operations\":").has_value());
}

} // anonymous namespace
} // namespace projects
} // namespace session
} // namespace rstudio
