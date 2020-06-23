/*
 * PostbackOptions.cpp
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

#include "PostbackOptions.hpp"

#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace postback {

Options& options()
{
   static Options instance;
   return instance;
}
   
ProgramStatus Options::read(int argc, char * const argv[])
{
   using namespace boost::program_options;
   
   // postback includes command and optional argument
   options_description postback("postback");
   postback.add_options()
      ("command", 
       value<std::string>(&command_), 
       "command to postback")
      ("argument",
       value<std::string>(&argument_)->default_value(""), 
       "argument to postback");
   
   // define program options (allow positional specification)
   program_options::OptionsDescription optionsDesc(programName_);
   optionsDesc.commandLine.add(postback);
   optionsDesc.positionalOptions.add("command", 1);
   optionsDesc.positionalOptions.add("argument", 1);
   
   // read options
   ProgramStatus status = core::program_options::read(optionsDesc, argc, argv);
   if (status.exit())
      return status;
   
   // return status
   return ProgramStatus::run();
}

} // namespace postback
} // namespace session
} // namespace rstudio
