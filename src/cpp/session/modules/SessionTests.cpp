/*
 * SessionTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include "SessionTests.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Algorithm.hpp>
#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/prefs/UserPrefs.hpp>

#define kTestsNone           "none"
#define kTestsTestThat       "test-testthat"
#define kTestsShinyTest      "test-shinytest"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace tests {

namespace {

bool isOnlySpaceBefore(const std::string& contents, size_t pos)
{
    while (pos > 0)
    {
        pos--;
        if (!isspace(contents.at(pos)))
        {
            return false;
        }
    }

    return true;
}

TestsFileType getTestType(const std::string& contents)
{
   // shinytest2 tests are testthat tests that drive a Shiny app via
   // AppDriver$new(). Classify a file as shinytest only when *both* tokens
   // appear (with test_that( on a token boundary) — that way an offhand
   // AppDriver$new( in a comment of a plain testthat file doesn't trip
   // the shinytest UI commands.
   size_t testThatPos = contents.find("test_that(", 0);
   bool hasTestThat =
         testThatPos != std::string::npos &&
         (testThatPos == 0 || isspace(contents.at(testThatPos - 1)));

   if (hasTestThat &&
       contents.find("AppDriver$new(", 0) != std::string::npos)
   {
      return TestsShinyTest;
   }

   if (hasTestThat)
      return TestsTestThat;

   size_t pos = contents.find("context(\"", 0);
   if (pos != std::string::npos && (pos == 0 || isOnlySpaceBefore(contents, pos)))
   {
      pos = contents.find("test_", 0);
      if (pos != std::string::npos && (pos == 0 || isspace(contents.at(pos - 1))))
      {
         return TestsTestThat;
      }
   }

   return TestsNone;
}

std::string onDetectTestsSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty() && pDoc->canContainRCode())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      TestsFileType type = getTestType(pDoc->contents());
      switch(type)
      {
         case TestsNone:
            return std::string();
         case TestsTestThat:
            return kTestsTestThat;
         case TestsShinyTest:
            return kTestsShinyTest;
      }
   }

   return std::string();
}

} // anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;

   events().onDetectSourceExtendedType.connect(onDetectTestsSourceType);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionTests.R"));

   return initBlock.execute();
}


} // namespace tests
} // namespace modules
} // namespace session
} // namespace rstudio

