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

   bool changed(const FilePath& priorConfig, std::time_t priorWriteTime)
   {
      return modules::quarto::preview::projectConfigChanged(
         previewTarget_, priorConfig, priorWriteTime);
   }

   FilePath projectDir_;
   FilePath previewTarget_;
};

TEST_F(PreviewConfigChange, UnchangedConfigIsNotChanged)
{
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   EXPECT_FALSE(changed(quartoYml, quartoYml.getLastWriteTime()));
}

TEST_F(PreviewConfigChange, ConfigModifiedAfterStartIsChanged)
{
   // the file on disk is newer than the time captured when the preview started
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   EXPECT_TRUE(changed(quartoYml, quartoYml.getLastWriteTime() - 10));
}

TEST_F(PreviewConfigChange, ConfigSavedBeforeStartIsNotChanged)
{
   // the file on disk is older than the time captured when the preview started
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   EXPECT_FALSE(changed(quartoYml, quartoYml.getLastWriteTime() + 10));
}

TEST_F(PreviewConfigChange, NoConfigIsNotChanged)
{
   // no _quarto.yml exists and none did when the preview started
   EXPECT_FALSE(changed(FilePath(), 0));
}

TEST_F(PreviewConfigChange, ConfigAddedIsChanged)
{
   // no config when the preview started, but one exists now
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   EXPECT_TRUE(changed(FilePath(), 0));
}

TEST_F(PreviewConfigChange, ConfigRemovedIsChanged)
{
   // a config governed the preview at startup, but none exists now
   FilePath priorConfig = projectDir_.completeChildPath("_quarto.yml");
   EXPECT_TRUE(changed(priorConfig, std::time_t(0)));
}

TEST_F(PreviewConfigChange, DifferentConfigFileIsChanged)
{
   // a different config file governs the file than did at startup
   FilePath quartoYml = writeQuartoYml("project:\n  type: default\n");
   FilePath priorConfig = projectDir_.completeChildPath("_quarto.yaml");
   EXPECT_TRUE(changed(priorConfig, quartoYml.getLastWriteTime()));
}

} // namespace tests
} // namespace quarto
} // namespace session
} // namespace rstudio
