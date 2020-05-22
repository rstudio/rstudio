/*
 * SessionTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/Algorithm.hpp>
#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionConsoleProcess.hpp>
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
   size_t pos = contents.find("test_that(", 0);
   if (pos != std::string::npos && (pos == 0 || isspace(contents.at(pos - 1))))
      return TestsTestThat;

   pos = contents.find("context(\"", 0);
   if (pos != std::string::npos && (pos == 0 || isOnlySpaceBefore(contents, pos)))
   {
      pos = contents.find("test_", 0);
      if (pos != std::string::npos && (pos == 0 || isspace(contents.at(pos - 1))))
      {
         return TestsTestThat;
      }
   }

   pos = contents.find("app <- ShinyDriver$new(", 0);
   if (pos == 0)
      return TestsShinyTest;

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

Error installShinyTestDependencies(const json::JsonRpcRequest& request,
                                   json::JsonRpcResponse* pResponse)
{
   // Prepare the command
   std::string cmd;
   cmd.append("shinytest::installDependencies()");

   // R binary
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");

   // for windows we need to forward setInternet2
#ifdef _WIN32
   if (!r::session::utils::isR3_3() && prefs::userPrefs().useInternet2())
      args.push_back("--internet2");
#endif

   args.push_back("-e");
   args.push_back(cmd);

   boost::shared_ptr<console_process::ConsoleProcessInfo> pCPI =
         boost::make_shared<console_process::ConsoleProcessInfo>(
            "Installing shinytest dependencies", console_process::InteractionNever);

   // create and execute console process
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   pCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.getAbsolutePath()),
            args,
            options,
            pCPI);

   // return console process
   pResponse->setResult(pCP->toJson(console_process::ClientSerialization));
   return Success();
}

Error initialize()
{
   using namespace module_context;
   using boost::bind;

   events().onDetectSourceExtendedType.connect(onDetectTestsSourceType);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionTests.R"))
      (bind(registerRpcMethod, "install_shinytest_dependencies", installShinyTestDependencies));

   return initBlock.execute();
}


} // namespace tests
} // namespace modules
} // namespace session
} // namespace rstudio

