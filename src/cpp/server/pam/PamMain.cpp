/*
 * PamMain.cpp
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
#include <stdio.h>

#include <boost/format.hpp>

#include <core/CrashHandler.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>
#include <server_core/system/Pam.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace {

int inappropriateUsage(const ErrorLocation& location)
{
   // log warning
   boost::format fmt("Inappropriate use of pam helper binary (user=%1%)");
   std::string msg = boost::str(
               fmt % core::system::user::currentUserIdentity().userId);
   core::log::logWarningMessage(msg, location);

   // additional notification to the user
   std::cerr << "\nThis binary is not designed for running this way\n"
                "-- the system administrator has been informed\n\n";

   // cause further annoyance
   ::sleep(10);

   return EXIT_FAILURE;
}

} // anonymous namespace


int main(int argc, char * const argv[]) 
{
   try
   { 
      // initialize log
      core::log::setProgramId("rserver-pam");
      core::system::initializeSystemLog("rserver-pam", core::log::LogLevel::WARN);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // catch unhandled exceptions
      error = core::crash_handler::initialize();
      if (error)
         LOG_ERROR(error);

      // ensure that we aren't being called inappropriately
      if (::isatty(STDIN_FILENO))
         return inappropriateUsage(ERROR_LOCATION);
      else if (::isatty(STDOUT_FILENO))
         return inappropriateUsage(ERROR_LOCATION);
      else if (argc < 2 || argc > 4)
         return inappropriateUsage(ERROR_LOCATION);

      // read username from command line
      std::string username(argv[1]);

      std::string service("rstudio");
      if (argc >= 3) {
        service = argv[2];
      }

      bool requirePasswordPrompt = true;
      if (argc >= 4) {
        requirePasswordPrompt = std::string(argv[3]) == "1";
      }

      // read password (up to 200 chars in length)
      std::string password;
      const int MAXPASS = 200;
      int ch = 0;
      int count = 0;
      while((ch = ::fgetc(stdin)) != EOF)
      {
         if (++count <= MAXPASS)
         {
            password.push_back(static_cast<char>(ch));
         }
         else
         {
            LOG_WARNING_MESSAGE("Password exceeded maximum length for "
                                "user " + username);
            return EXIT_FAILURE;
         }
      }

      // verify password
      core::system::PAM pam(service, false, true, requirePasswordPrompt);
      if (pam.login(username, password) == PAM_SUCCESS)
         return EXIT_SUCCESS;
      else
         return EXIT_FAILURE;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE;
}

