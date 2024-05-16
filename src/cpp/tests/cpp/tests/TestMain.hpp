/*
 * TestMain.hpp
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

// Include this to build a Catch test executable.
// See TestRunner.hpp if you want to embed the test runner
// in your own executable.

#ifndef TESTS_TESTMAIN_HPP
#define TESTS_TESTMAIN_HPP

#define CATCH_CONFIG_RUNNER
#include "vendor/catch.hpp"

#include <shared_core/Logger.hpp>
#include <shared_core/StderrLogDestination.hpp>

#include <core/Log.hpp>
#include <core/LogOptions.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>
#include <core/system/System.hpp>

int main(int argc, const char* argv[])
{
   using namespace rstudio;
   using namespace rstudio::core;
   using namespace rstudio::core::log;
   
   std::string programId = "rstudio-tests-" + core::system::username();
   core::log::setProgramId(programId);
   core::system::initializeStderrLog(programId, LogLevel::WARN, false);
   
   return Catch::Session().run(argc, const_cast<char**>(argv));
}

#endif
