/*
 * SessionLSPTests.cpp
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

#include "SessionLSP.hpp"

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>

#include <session/SessionSourceDatabase.hpp>

namespace rstudio {
namespace session {
namespace tests {

using core::FilePath;
using modules::lsp::languageIdFromDocument;
using modules::lsp::languageIdFromFilename;

namespace {

std::string languageIdForName(const std::string& fileName)
{
   return languageIdFromFilename(FilePath("/project").completeChildPath(fileName));
}

// Saved documents of the given type, as the editor has them open.
class LanguageIdFromDocumentTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      FilePath::tempFilePath(dir_);
      dir_.ensureDirectory();
   }

   void TearDown() override
   {
      dir_.removeIfExists();
   }

   std::string languageIdFor(const std::string& fileName, const std::string& type)
   {
      FilePath path = dir_.completeChildPath(fileName);
      EXPECT_FALSE(core::writeStringToFile(path, "contents\n"));
      auto pDoc = boost::make_shared<source_database::SourceDocument>(type);
      EXPECT_FALSE(pDoc->setPathAndContents(path.getAbsolutePath()));
      return languageIdFromDocument(pDoc);
   }

private:
   FilePath dir_;
};

} // anonymous namespace

TEST(SessionLSP, LanguageIdFromFilenameRecognizesDockerfileNames)
{
   EXPECT_EQ(languageIdForName("Dockerfile"), "dockerfile");
   EXPECT_EQ(languageIdForName("Containerfile"), "dockerfile");
   EXPECT_EQ(languageIdForName("Dockerfile.dev"), "dockerfile");
   EXPECT_EQ(languageIdForName("Containerfile.dev"), "dockerfile");
}

TEST(SessionLSP, LanguageIdFromFilenameRecognizesMakefileNames)
{
   EXPECT_EQ(languageIdForName("Makefile"), "makefile");
   EXPECT_EQ(languageIdForName("makefile"), "makefile");
}

TEST(SessionLSP, LanguageIdFromFilenameIgnoresOtherNames)
{
   EXPECT_EQ(languageIdForName("analysis.R"), "");
   EXPECT_EQ(languageIdForName(".Rprofile"), "");
   EXPECT_EQ(languageIdForName("Rprofile.site"), "");
   EXPECT_EQ(languageIdForName("Makefile.in"), "");
   EXPECT_EQ(languageIdForName("MyDockerfile"), "");
}

TEST_F(LanguageIdFromDocumentTest, RDocumentsAreR)
{
   EXPECT_EQ(languageIdFor("analysis.R", kSourceDocumentTypeRSource), "r");
   EXPECT_EQ(languageIdFor("report.Rmd", kSourceDocumentTypeRMarkdown), "r");
}

TEST_F(LanguageIdFromDocumentTest, FileNameTakesPrecedenceOverType)
{
   // The editor opens Containerfile.dev as plain text
   EXPECT_EQ(languageIdFor("Containerfile.dev", "text"), "dockerfile");
   EXPECT_EQ(languageIdFor("Containerfile", "dockerfile"), "dockerfile");
   EXPECT_EQ(languageIdFor("Makefile", "makefile"), "makefile");
}

TEST_F(LanguageIdFromDocumentTest, OtherDocumentsUseTheirLowercasedType)
{
   EXPECT_EQ(languageIdFor("script.py", kSourceDocumentTypePython), "python");
   EXPECT_EQ(languageIdFor("notes.txt", "Text"), "text");
}

} // namespace tests
} // namespace session
} // namespace rstudio
