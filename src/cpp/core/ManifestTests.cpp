/*
 * ManifestTests.cpp
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

#include <gtest/gtest.h>

#include <string>
#include <vector>

#include <shared_core/FilePath.hpp>

#include <core/FileUtils.hpp>

// These tests run on every platform even though the manifests are only consumed on
// Windows, because the files are checked in and a developer on any platform can
// break them.
//
// Nothing in the build validates these files: the linker is passed /MANIFEST:NO and
// rc.exe embeds the manifest as an opaque RT_MANIFEST resource without parsing it.
// The first thing to parse one is the Windows side-by-side loader at process launch,
// so a malformed manifest either silently drops its settings or stops the executable
// from starting -- neither of which shows up in CI. See #12806.

namespace rstudio {
namespace core {
namespace {

std::vector<std::string> manifestRelativePaths()
{
   return {
      "session/rsession.exe.manifest",
      "session/rsession-utf8.exe.manifest",
      "session/consoleio/consoleio.exe.manifest",
      "session/postback/rpostback.exe.manifest",
      "diagnostics/diagnostics.exe.manifest"
   };
}

FilePath manifestPath(const std::string& relativePath)
{
   return FilePath(RSTUDIO_CPP_SOURCE_DIR).completeChildPath(relativePath);
}

} // anonymous namespace

TEST(ManifestTest, ManifestCommentsHaveNoDoubleHyphen)
{
   // XML forbids "--" inside a comment body. This is easy to introduce, because our
   // house style uses "--" where prose wants an em dash, and lenient parsers (including
   // rapidxml, which we vendor) skip to "-->" without noticing -- so a targeted check
   // catches strictly more than parsing would.
   for (const std::string& relativePath : manifestRelativePaths())
   {
      FilePath path = manifestPath(relativePath);
      ASSERT_TRUE(path.exists()) << relativePath << " not found at " << path.getAbsolutePath();

      std::string contents = file_utils::readFile(path);

      std::size_t commentStart = contents.find("<!--");
      while (commentStart != std::string::npos)
      {
         std::size_t bodyStart = commentStart + 4;
         std::size_t commentEnd = contents.find("-->", bodyStart);
         ASSERT_NE(commentEnd, std::string::npos)
            << relativePath << ": unterminated XML comment";

         std::string body = contents.substr(bodyStart, commentEnd - bodyStart);
         EXPECT_EQ(body.find("--"), std::string::npos)
            << relativePath << ": XML comments cannot contain a double hyphen; "
            << "use a single hyphen or a comma instead. Comment body: " << body;

         commentStart = contents.find("<!--", commentEnd + 3);
      }
   }
}

TEST(ManifestTest, ManifestsDeclareLongPathAwareness)
{
   // Long path support is a per-process opt-in, so dropping this from any of our
   // executables silently reinstates the MAX_PATH limit for it. See #12806.
   for (const std::string& relativePath : manifestRelativePaths())
   {
      FilePath path = manifestPath(relativePath);
      ASSERT_TRUE(path.exists()) << relativePath << " not found at " << path.getAbsolutePath();

      std::string contents = file_utils::readFile(path);
      EXPECT_NE(contents.find("<longPathAware"), std::string::npos)
         << relativePath << ": missing a longPathAware declaration";
   }
}

} // namespace core
} // namespace rstudio
