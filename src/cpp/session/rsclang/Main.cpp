/*
 * Main.cpp
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

#include <iostream>

#include <boost/algorithm/string/case_conv.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/ProgramStatus.hpp>
#include <core/system/System.hpp>

#include <libclang/libclang.hpp>

#include "Options.hpp"

#include "rsclang-config.h"

using namespace core;
using namespace rsclang;

namespace {



} // anonymous namespace


int main(int argc, char** argv)
{
  core::system::initializeStderrLog("rstudio-rsclang",
                                    core::system::kLogLevelWarning);

  // ignore SIGPIPE
  Error error = core::system::ignoreSignal(core::system::SigPipe);
  if (error)
     LOG_ERROR(error);

  // read program options
  Options& options = rsclang::options();
  ProgramStatus status = options.read(argc, argv);
  if ( status.exit() )
     return status.exitCode() ;

  // is this an availability check?
  if (options.checkAvailable())
  {
     std::string errMsg;
     if (libclang::isLoadable(options.libclangPath().absolutePath(), &errMsg))
     {
        return EXIT_SUCCESS;
     }
     else
     {
        std::cerr << errMsg << std::endl;
        return EXIT_FAILURE;
     }
  }

  // otherwise wait for commands
  while (true)
  {
     //std::cerr << "rsclang: waiting for input" << std::endl;

     // read next command
     std::string command;
     char ch;
     while ((ch = ::getc(stdin)) != '\n')
     {
        //std::cerr << "rsclang: got character" << std::endl;

        command.push_back(ch);
     }

     //std::cerr << "rsclang: input termianted" << std::endl;

     // execute and return output
     boost::algorithm::to_upper(command);
     std::cout << command << std::endl;
     //::puts(command.c_str());
  }


  return EXIT_SUCCESS;
}
