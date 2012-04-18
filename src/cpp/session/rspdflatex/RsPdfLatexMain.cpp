/*
 * RsPdfLatexMain.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <iostream>

#include <boost/format.hpp>
#include <boost/bind.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/system/ShellUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>

using namespace core;


void onExit(int exitStatus, int *pStatus)
{
   *pStatus = exitStatus;
}

void onStdError(const std::string& output)
{
   std::cerr << output;
}

void onStdOutput(const std::string& output)
{
   std::cout << output;
}

int main(int argc, char** argv)
{
   try
   {
      // initialize log
      initializeSystemLog("rspdflatex", core::system::kLogLevelWarning);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // get all of the variables used to drive pdf compilation
      std::string rsPdfLatex = core::system::getenv("RS_PDFLATEX");
      core::shell_utils::ShellArgs args;
      std::size_t optNumber = 1;
      boost::format fmt("RS_PDFLATEX_OPTION_%1%");
      while (true)
      {
         std::string optionName = boost::str(fmt % optNumber);
         std::string optionVal = core::system::getenv(optionName);
         if (optionVal.empty())
            break;

         args << optionVal;
         optNumber++;
      }

      // get the other args passed to us
      for (int i = 1; i<argc; i++)
         args << argv[i];

      // run the requested process
      int exitStatus = EXIT_FAILURE;
      core::system::ProcessOptions options;
      core::system::ProcessCallbacks cb;
      cb.onStderr = boost::bind(onStdError, _2);
      cb.onStdout = boost::bind(onStdOutput, _2);
      cb.onExit = boost::bind(onExit, _1, &exitStatus);
      core::system::ProcessSupervisor supervisor;
      error = supervisor.runProgram(rsPdfLatex, args, options, cb);
      if (error)
      {
         LOG_ERROR(error);
         return EXIT_FAILURE;
      }

      // wait for it to terminate
      while(supervisor.hasRunningChildren())
         supervisor.wait(boost::posix_time::milliseconds(50));

      // return the exit status
      return exitStatus;
   }
   CATCH_UNEXPECTED_EXCEPTION

   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}
