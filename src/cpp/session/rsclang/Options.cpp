/*
 * Options.cpp
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

#include "Options.hpp"

#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>

using namespace core ;

namespace rsclang {

Options& options()
{
   static Options instance ;
   return instance ;
}
   
ProgramStatus Options::read(int argc, char * const argv[])
{
   using namespace boost::program_options ;
   
   // postback includes command and optional argument
   std::string libclangPath;
   options_description rsclang("rsclang");
   rsclang.add_options()
      ("libclang-path",
       value<std::string>(&libclangPath),
       "path to libclang shared library")
      ("check-available",
       bool_switch(&checkAvailable_),
       "check whether libclang is available on this system");

   // define program options (allow positional specification)
   program_options::OptionsDescription optionsDesc("rsclang");
   optionsDesc.commandLine.add(rsclang);
   
   // read options
   ProgramStatus status = core::program_options::read(optionsDesc, argc, argv);
   if (status.exit())
      return status;
   
   libclangPath_ = FilePath(libclangPath);

   // return status
   return ProgramStatus::run();
}

} // namespace rsclang

