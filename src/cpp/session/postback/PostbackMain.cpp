/*
 * PostbackMain.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#if defined(_WIN32)
// Necessary to avoid compile error on Win x64
#include <winsock2.h>
#endif

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/ProgramStatus.hpp>
#include <core/SafeConvert.hpp>

#include <core/system/System.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <session/SessionConstants.hpp>

#include <session/http/SessionRequest.hpp>

#include "PostbackOptions.hpp"

using namespace rstudio;
using namespace rstudio::core;
using namespace session::postback ;

int exitFailure(const Error& error)
{
   LOG_ERROR(error);
   return EXIT_FAILURE;
}

int main(int argc, char * const argv[]) 
{
   try
   {
      // initialize log
      initializeSystemLog("rpostback", core::system::kLogLevelWarning);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // read program options 
      Options& options = session::postback::options();
      ProgramStatus status = options.read(argc, argv); 
      if ( status.exit() )
         return status.exitCode() ;
      
      http::Response response;
      error = session::http::sendSessionRequest(
            kLocalUriLocationPrefix kPostbackUriScope + options.command(), 
            options.argument(), 
            &response);
      if (error)
         return exitFailure(error);

      std::string exitCode = response.headerValue(kPostbackExitCodeHeader);
      std::cout << response.body();
      return safe_convert::stringTo<int>(exitCode, EXIT_FAILURE);
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

