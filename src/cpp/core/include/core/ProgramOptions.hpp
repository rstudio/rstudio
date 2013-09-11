/*
 * ProgramOptions.hpp
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

#ifndef CORE_PROGRAM_OPTIONS_HPP
#define CORE_PROGRAM_OPTIONS_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/program_options.hpp>

#include <core/ProgramStatus.hpp>

namespace core {
   
class Error;
class ErrorLocation;
class ProgramStatus;

namespace program_options {
      
struct OptionsDescription
{
   OptionsDescription(const std::string& programName,
                      const std::string& defaultConfigFilePath = std::string())
      :  programName(programName),
         defaultConfigFilePath(defaultConfigFilePath),
         commandLine("command-line options"),
         configFile("config-file options")
   {
   }
   std::string programName ;
   std::string defaultConfigFilePath;
   boost::program_options::options_description commandLine;
   boost::program_options::positional_options_description positionalOptions;
   boost::program_options::options_description configFile;
};


ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc,
                   char * const argv[],
                   bool* pHelp);

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          char * const argv[])
{
   bool help;
   return read(optionsDescription, argc, argv, &help);
}

void reportError(const std::string& errorMessage,
                 const ErrorLocation& location);

void reportWarnings(const std::string& warningMessages,
                    const ErrorLocation& location);
   
} // namespace program_options
} // namespace core 

#endif // CORE_PROGRAM_OPTIONS_HPP

