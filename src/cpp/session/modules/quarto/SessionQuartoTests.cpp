/*
 * SessionQuartoTests.cpp
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

#include <session/SessionQuarto.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <gtest/gtest.h>

#include "SessionQuartoPreview.hpp"

namespace rstudio {
namespace session {
namespace quarto {
namespace tests {

using namespace rstudio::core;

class ProjectTypeResolution : public ::testing::Test
{
protected:
   void SetUp() override
   {
      FilePath::tempFilePath(projectDir_);
      projectDir_.ensureDirectory();
   }

   void TearDown() override
   {
      projectDir_.removeIfExists();
   }

   FilePath writeQuartoYml(const std::string& projectType)
   {
      FilePath quartoYml = projectDir_.completeChildPath("_quarto.yml");
      writeStringToFile(quartoYml, "project:\n  type: " + projectType + "\n");
      return quartoYml;
   }

   void writeExtensionYml(const std::string& relativePath,
                          const std::string& baseType)
   {
      FilePath extYml = projectDir_.completeChildPath(relativePath);
      extYml.getParent().ensureDirectory();
      writeStringToFile(
         extYml,
         "contributes:\n"
         "  project:\n"
         "    project:\n"
         "      type: " + baseType + "\n");
   }

   std::string resolvedType(const FilePath& quartoYml)
   {
      std::string type;
      readQuartoProjectConfig(quartoYml, &type);
      return type;
   }

   FilePath projectDir_;
};

TEST_F(ProjectTypeResolution, BuiltinTypePassesThrough)
{
   FilePath quartoYml = writeQuartoYml("website");
   EXPECT_EQ(resolvedType(quartoYml), "website");
}

TEST_F(ProjectTypeResolution, SiteAliasIsMigratedToWebsite)
{
   // 'site' is the legacy name for 'website'
   FilePath quartoYml = writeQuartoYml("site");
   EXPECT_EQ(resolvedType(quartoYml), "website");
}

TEST_F(ProjectTypeResolution, AuthoredExtensionResolvesToBaseType)
{
   FilePath quartoYml = writeQuartoYml("posit-docs");
   writeExtensionYml("_extensions/posit/posit-docs/_extension.yml", "website");
   EXPECT_EQ(resolvedType(quartoYml), "website");
}

TEST_F(ProjectTypeResolution, BareExtensionResolvesToBaseType)
{
   FilePath quartoYml = writeQuartoYml("custom");
   writeExtensionYml("_extensions/custom/_extension.yml", "book");
   EXPECT_EQ(resolvedType(quartoYml), "book");
}

TEST_F(ProjectTypeResolution, QualifiedTypeResolvesToBaseType)
{
   // user references the extension as <author>/<name> in _quarto.yml
   FilePath quartoYml = writeQuartoYml("posit/docs");
   writeExtensionYml("_extensions/posit/docs/_extension.yml", "manuscript");
   EXPECT_EQ(resolvedType(quartoYml), "manuscript");
}

TEST_F(ProjectTypeResolution, MissingExtensionsDirFallsBackToRawType)
{
   FilePath quartoYml = writeQuartoYml("unknown-type");
   EXPECT_EQ(resolvedType(quartoYml), "unknown-type");
}

TEST_F(ProjectTypeResolution, MalformedExtensionYmlFallsBackToRawType)
{
   FilePath quartoYml = writeQuartoYml("broken-ext");

   FilePath extYml = projectDir_.completeChildPath(
      "_extensions/broken-ext/_extension.yml");
   extYml.getParent().ensureDirectory();
   writeStringToFile(extYml, "this: is: not: valid: yaml: [\n");

   EXPECT_EQ(resolvedType(quartoYml), "broken-ext");
}

TEST_F(ProjectTypeResolution, ExtensionWithoutBaseTypeFallsBackToRawType)
{
   FilePath quartoYml = writeQuartoYml("partial-ext");

   FilePath extYml = projectDir_.completeChildPath(
      "_extensions/partial-ext/_extension.yml");
   extYml.getParent().ensureDirectory();
   writeStringToFile(extYml, "title: A partial extension\n");

   EXPECT_EQ(resolvedType(quartoYml), "partial-ext");
}

// Tests for the preview-reuse "did the project config change?" predicate that
// decides whether a running 'quarto preview' server can be reused for an
// in-place re-render. See https://github.com/rstudio/rstudio/issues/17874.
class PreviewConfigChange : public ::testing::Test
{
protected:
   void SetUp() override
   {
      FilePath::tempFilePath(projectDir_);
      projectDir_.ensureDirectory();

      previewTarget_ = projectDir_.completeChildPath("doc.qmd");
      writeStringToFile(previewTarget_, "---\ntitle: Test\n---\n");

      // quartoProjectConfigFile() walks parents up to userHomePath(); guard against
      // a polluted temp location (e.g. TMPDIR resolving under $HOME, or a stray
      // ancestor _quarto.yml) that would silently flip the no-config expectations
      ASSERT_TRUE(quartoProjectConfigFile(previewTarget_).isEmpty());
   }

   void TearDown() override
   {
      projectDir_.removeIfExists();
   }

   FilePath writeQuartoYml(const std::string& contents)
   {
      FilePath quartoYml = projectDir_.completeChildPath("_quarto.yml");
      writeStringToFile(quartoYml, contents);
      return quartoYml;
   }

   bool changed(const FilePath& priorConfig, const std::string& priorContents,
                bool priorCaptured = true)
   {
      return modules::quarto::preview::projectConfigChanged(
         previewTarget_, priorConfig, priorContents, priorCaptured);
   }

   FilePath projectDir_;
   FilePath previewTarget_;
};

TEST_F(PreviewConfigChange, UnchangedConfigIsNotChanged)
{
   std::string contents = "project:\n  type: default\n";
   FilePath quartoYml = writeQuartoYml(contents);
   EXPECT_FALSE(changed(quartoYml, contents));
}

TEST_F(PreviewConfigChange, ConfigContentsModifiedIsChanged)
{
   // contents are compared rather than timestamps, so a content edit is detected
   // even when it lands in the same second as the captured startup state
   FilePath quartoYml = writeQuartoYml("pdf-engine: pdflatex\n");
   EXPECT_TRUE(changed(quartoYml, "pdf-engine: xelatex\n"));
}

TEST_F(PreviewConfigChange, NoConfigIsNotChanged)
{
   // no _quarto.yml exists and none did when the preview started
   EXPECT_FALSE(changed(FilePath(), std::string()));
}

TEST_F(PreviewConfigChange, ConfigAddedIsChanged)
{
   // no config when the preview started, but one exists now
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   EXPECT_TRUE(changed(FilePath(), std::string()));
}

TEST_F(PreviewConfigChange, ConfigRemovedIsChanged)
{
   // a config governed the preview at startup, but none exists now
   FilePath priorConfig = projectDir_.completeChildPath("_quarto.yml");
   EXPECT_TRUE(changed(priorConfig, "project:\n  type: default\n"));
}

TEST_F(PreviewConfigChange, DifferentConfigFileIsChanged)
{
   // a different config file governs the file than did at startup
   std::string contents = "project:\n  type: default\n";
   FilePath quartoYml = writeQuartoYml(contents);
   FilePath priorConfig = projectDir_.completeChildPath("_quarto.yaml");
   EXPECT_TRUE(changed(priorConfig, contents));
}

TEST_F(PreviewConfigChange, UncapturedBaselineIsChanged)
{
   // the baseline config couldn't be captured when the preview started, so we
   // can't trust the comparison -- assume it changed and restart
   std::string contents = "project:\n  type: default\n";
   FilePath quartoYml = writeQuartoYml(contents);
   EXPECT_TRUE(changed(quartoYml, contents, /*priorCaptured=*/false));
}

TEST_F(PreviewConfigChange, UnreadableConfigIsChanged)
{
   // the config can't be read for comparison (here a directory sits where the
   // config file is expected) -- assume it changed and restart
   FilePath quartoYml = projectDir_.completeChildPath("_quarto.yml");
   quartoYml.ensureDirectory();
   EXPECT_TRUE(changed(quartoYml, "project:\n  type: default\n"));
}

// Tests for the working directory and target path used to launch a
// 'quarto preview' job. Files within a Quarto project are previewed from the
// project root with a project-relative path, as 'quarto preview' can fail to
// resolve paths relative to a project sub-directory. See
// https://github.com/rstudio/rstudio/issues/18197.
class PreviewWorkingDir : public ::testing::Test
{
protected:
   void SetUp() override
   {
      FilePath::tempFilePath(projectDir_);
      projectDir_.ensureDirectory();
   }

   void TearDown() override
   {
      projectDir_.removeIfExists();
   }

   FilePath writeFile(const std::string& relativePath)
   {
      FilePath file = projectDir_.completeChildPath(relativePath);
      file.getParent().ensureDirectory();
      writeStringToFile(file, "---\ntitle: Test\n---\n");
      return file;
   }

   FilePath projectDir_;
};

TEST_F(PreviewWorkingDir, FileInProjectSubdirectoryRunsFromProjectRoot)
{
   FilePath quartoYml = writeFile("_quarto.yml");
   FilePath previewTarget = writeFile("labs/doc.qmd");

   FilePath workingDir = modules::quarto::preview::previewWorkingDir(previewTarget, quartoYml);
   EXPECT_EQ(workingDir, projectDir_);
   EXPECT_EQ(modules::quarto::preview::previewTargetPath(previewTarget, workingDir), "labs/doc.qmd");
}

TEST_F(PreviewWorkingDir, FileAtProjectRootRunsFromProjectRoot)
{
   FilePath quartoYml = writeFile("_quarto.yml");
   FilePath previewTarget = writeFile("doc.qmd");

   FilePath workingDir = modules::quarto::preview::previewWorkingDir(previewTarget, quartoYml);
   EXPECT_EQ(workingDir, projectDir_);
   EXPECT_EQ(modules::quarto::preview::previewTargetPath(previewTarget, workingDir), "doc.qmd");
}

TEST_F(PreviewWorkingDir, FileOutsideProjectRunsFromParentDirectory)
{
   FilePath previewTarget = writeFile("labs/doc.qmd");

   FilePath workingDir = modules::quarto::preview::previewWorkingDir(previewTarget, FilePath());
   EXPECT_EQ(workingDir, previewTarget.getParent());
   EXPECT_EQ(modules::quarto::preview::previewTargetPath(previewTarget, workingDir), "doc.qmd");
}

TEST_F(PreviewWorkingDir, DirectoryTargetRunsFromItself)
{
   FilePath quartoYml = writeFile("_quarto.yml");

   FilePath workingDir = modules::quarto::preview::previewWorkingDir(projectDir_, quartoYml);
   EXPECT_EQ(workingDir, projectDir_);
}

TEST_F(PreviewWorkingDir, TargetOutsideWorkingDirFallsBackToAbsolutePath)
{
   FilePath previewTarget = writeFile("doc.qmd");
   FilePath unrelatedDir = projectDir_.completeChildPath("elsewhere");

   EXPECT_EQ(modules::quarto::preview::previewTargetPath(previewTarget, unrelatedDir),
             previewTarget.getAbsolutePath());
}

} // namespace tests
} // namespace quarto
} // namespace session
} // namespace rstudio
