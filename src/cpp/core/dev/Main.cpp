/*
 * Main.cpp
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

#include <iostream>
#include <fstream>

#include <boost/test/minimal.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

#include <core/r_util/RVersionsPosix.hpp>

using namespace rstudio;
using namespace rstudio::core;

int test_main(int argc, char * argv[])
{
   try
   { 
      // setup log
      log::setLogLevel(log::LogLevel::WARN);
      log::setProgramId("coredev")
      system::initializeStderrLog("coredev", log::LogLevel::WARN);

      // ignore sigpipe
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      using namespace r_util;
      std::vector<RVersion> versions;
      error = readRVersionsFromFile(FilePath("rstudio-some-r-versions"), &versions);
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);


      RVersion version = selectVersion("3.1.0", "/opt/R/3.1.0/lib/R", versions);


      std::cerr << version << std::endl;


      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE;
}

