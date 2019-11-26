/*
 * SessionConsoleProcessInfoTests.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/ShellUtils.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

 namespace rstudio {
 namespace session {
 namespace modules {
 namespace find {
 namespace tests {

 using namespace rstudio::core;
 //using namespace modules::find;

 namespace {

   bool previewFlag = true;
   bool replaceFlag = true;
   bool asRegex = false;
   bool ignoreCase = true;
   bool replaceRegex = false;
   bool useGitIgnore = false;
   //json::Array patterns;
   //LocalProgress* pProgress;
   std::string encoding;
   std::set<std::string>* pErrorSet;
   //json::Array* pReplaceMatchOn;
   //json::Array* pReplaceMatchOff;

   const std::string directory("/test/directory");
   const std::string searchPattern("hello");
   const std::string replacePattern("goodbye");
   const std::string regexSearchPattern("sub[^ ]");
   const std::string regexReplacePattern("sub-");
   const std::string handle("unit-test01");
   const FilePath grepFile;
   const FilePath replaceFile("/some/file/tbd.txt");
   const shell_utils::ShellCommand grepCommand("grep");


   } // anonymous namespace

   TEST_CASE("SessionFind")
   {
      SECTION("Add error message")
      {
         /*
         boost::shared_ptr<GrepOperation> pGrepOp = GrepOperation::create(encoding, grepFile);
         bool successFlag = true;
         pGrepOp->addErrorMessage("Test error message",
                                  pErrorSet, pReplaceMatchOn, pReplaceMatchOff,
                                  &successFlag);
         CHECK(pReplaceMatchOn->getValueAt(pReplaceMatchOn->getSize() - 1).getInt() == -1);
         CHECK(pReplaceMatchOff->getValueAt(pReplaceMatchOff->getSize() - 1).getInt() == -1);
         CHECK(pErrorSet->find("Test error message") !=
               pErrorSet->end());
         CHECK_FALSE(successFlag);
         */
      }

      SECTION("Initialize file replace")
      {
         /*
         boost::shared_ptr<GrepOperation> pGrepOp = GrepOperation::create(encoding, grepFile);
         Error error = pGrepOp->initializeFileForReplace(replaceFile);
         CHECK(!error);
         CHECK(pGrepOp->outputStream()->good());
         CHECK(pGrepOp->inputStream()->good());
         CHECK(pGrepOp->inputLineNum() == 0);
         CHECK(pGrepOp->currentFile() == replaceFile.getAbsolutePath());
         */
      }
   }
} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio
