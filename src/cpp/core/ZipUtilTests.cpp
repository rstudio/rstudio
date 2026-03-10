/*
 * ZipUtilTests.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include <core/ZipUtil.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/Process.hpp>
#include <boost/system/error_code.hpp>

#include <fstream>

namespace rstudio {
namespace core {
namespace zip {

#ifdef __linux__

TEST(ZipUtilTest, CompressFolderZipsASingleFile)
{
   using namespace rstudio::core;

   // Create a single file with some content
   FilePath testFile;
   Error error = FilePath::tempFilePath(".txt", testFile);
   ASSERT_FALSE(error);

   std::ofstream outFile(testFile.getAbsolutePath());
   ASSERT_TRUE(outFile.is_open());
   outFile << "This is a test file.";
   outFile.close();
   ASSERT_TRUE(testFile.exists());

   // Define the output zip file path
   FilePath zipFile = testFile.getParent().completeChildPath("test.zip");

   // Compress the single file
   error = zip::createZip(testFile, zipFile.getAbsolutePath(), false);
   ASSERT_FALSE(error);
   ASSERT_TRUE(zipFile.exists());

   // Check if the zip file contains the expected file
   system::ProcessResult result1, result2;
   error = system::runCommand("unzip -Z1 " + zipFile.getAbsolutePath(), system::ProcessOptions(), &result1);
   ASSERT_FALSE(error);
   EXPECT_EQ(0, result1.exitStatus);
   EXPECT_EQ(testFile.getFilename() + "\n", result1.stdOut);

   // Unzip the archive and check that the output file has the intended contents
   error = system::runCommand("unzip -p " + zipFile.getAbsolutePath(), system::ProcessOptions(), &result2);
   ASSERT_FALSE(error);
   EXPECT_EQ(0, result2.exitStatus);
   EXPECT_EQ("This is a test file.", result2.stdOut);

   // Clean up
   testFile.remove();
   zipFile.remove();
}

TEST(ZipUtilTest, CompressFolderZipsAFolderWithNestedFoldersAndFiles)
{
   using namespace rstudio::core;

   // Create a unique temp directory as base for all test artifacts
   FilePath tmpBase;
   Error error = FilePath::tempFilePath(tmpBase);
   ASSERT_FALSE(error);
   error = tmpBase.ensureDirectory();
   ASSERT_FALSE(error);

   // Create a temporary folder structure
   FilePath baseFolder = tmpBase.completeChildPath("test_folder");
   error = baseFolder.ensureDirectory();
   ASSERT_FALSE(error);

   FilePath nestedFolder = baseFolder.completeChildPath("nested");
   error = nestedFolder.ensureDirectory();
   ASSERT_FALSE(error);

   FilePath file1 = baseFolder.completeChildPath("file1.txt");
   FilePath file2 = nestedFolder.completeChildPath("file2.txt");

   std::ofstream outFile1(file1.getAbsolutePath());
   ASSERT_TRUE(outFile1.is_open());
   outFile1 << "This is file1.";
   outFile1.close();
   ASSERT_TRUE(file1.exists());

   std::ofstream outFile2(file2.getAbsolutePath());
   ASSERT_TRUE(outFile2.is_open());
   outFile2 << "This is file2.";
   outFile2.close();
   ASSERT_TRUE(file2.exists());

   // Define the output zip file path
   FilePath zipFile = tmpBase.completeChildPath("test_nested.zip");

   // Compress the folder
   error = zip::createZip(baseFolder, zipFile.getAbsolutePath(), true);
   ASSERT_FALSE(error);
   ASSERT_TRUE(zipFile.exists());

   // Check if the zip file contains the expected files
   system::ProcessResult result1, result2;
   error = system::runCommand("unzip -Z1 " + zipFile.getAbsolutePath(), system::ProcessOptions(), &result1);
   ASSERT_FALSE(error);
   EXPECT_EQ(0, result1.exitStatus);
   EXPECT_NE(std::string::npos, result1.stdOut.find("file1.txt"));
   EXPECT_NE(std::string::npos, result1.stdOut.find("nested/file2.txt"));

   // Unzip the archive and check the contents of the files
   FilePath unzipFolder = tmpBase.completeChildPath("unzip_test");
   error = unzipFolder.ensureDirectory();
   ASSERT_FALSE(error);

   error = system::runCommand("unzip " + zipFile.getAbsolutePath() + " -d " + unzipFolder.getAbsolutePath(), system::ProcessOptions(), &result2);
   ASSERT_FALSE(error);
   EXPECT_EQ(0, result2.exitStatus);

   FilePath unzippedFile1 = unzipFolder.completeChildPath("file1.txt");
   FilePath unzippedFile2 = unzipFolder.completeChildPath("nested/file2.txt");

   ASSERT_TRUE(unzippedFile1.exists());
   ASSERT_TRUE(unzippedFile2.exists());

   std::ifstream inFile1(unzippedFile1.getAbsolutePath());
   std::string content1((std::istreambuf_iterator<char>(inFile1)), std::istreambuf_iterator<char>());
   EXPECT_EQ("This is file1.", content1);

   std::ifstream inFile2(unzippedFile2.getAbsolutePath());
   std::string content2((std::istreambuf_iterator<char>(inFile2)), std::istreambuf_iterator<char>());
   EXPECT_EQ("This is file2.", content2);

   // Clean up
   tmpBase.remove();
}

TEST(ZipUtilTest, CompressFolderHandlesMissingInputFile)
{
   using namespace rstudio::core;

   // Define a non-existent file path
   FilePath missingFile = FilePath("/nonexistent/file.txt");

   // Define the output zip file path
   FilePath zipFile = missingFile.getParent().completeChildPath("missing_test.zip");

   // Attempt to compress the non-existent file
   Error error = zip::createZip(missingFile, zipFile.getAbsolutePath(), false);

   // Verify that an appropriate error is returned
   ASSERT_TRUE(error);
   EXPECT_NE(std::string::npos, error.getMessage().find("Failed to create zip file for folder '/nonexistent/file.txt'"));

   // Ensure the zip file was not created
   EXPECT_FALSE(zipFile.exists());
}

#endif // __linux__

} // namespace zip
} // namespace core
} // namespace rstudio
