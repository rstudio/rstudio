/*
 * SessionTests.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include "SessionTests.hpp"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#define kTestsNone           "none"
#define kTestsTestThat       "test-testthat"
#define kTestsShinyTest      "test-shinytest"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace tests {

namespace {

TestsFileType getTestType(const std::string& contents)
{
   if (contents.find("test_that(", 0) != std::string::npos)
      return TestsTestThat;

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
            return kTestsNone;
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
   return initBlock.execute();
}


} // namespace tests
} // namespace modules
} // namespace session
} // namespace rstudio

