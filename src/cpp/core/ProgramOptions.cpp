/*
 * ProgramOptions.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/ProgramOptions.hpp>

#include <iostream>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/System.hpp>

using namespace boost::program_options;

namespace rstudio {
namespace core {

namespace program_options {
 
namespace {

enum class OptionsParseState
{
   Initial,
   ConfigFile,
   CommandLine
};

bool validateOptionsProvided(const variables_map& vm,
                             const options_description& optionsDescription,
                             const std::string& configFile = std::string())
{
   for (const auto& pOptionsDesc : optionsDescription.options())
   {
      std::string optionName = pOptionsDesc->long_name();
      if ( !(vm.count(optionName)) )
      {
         std::string msg = "Required option " + optionName + " not specified";
         if (!configFile.empty())
            msg += " in config file " + configFile;
         reportError(msg, ERROR_LOCATION, true);
         return false;
      }
   }
   
   // all options validated
   return true;
}

}

void reportError(const Error& error, const ErrorLocation& location, bool forceStderr)
{
   std::string description = error.getProperty("description");

   // in some cases, we may need to force stderr to be written
   // for example, during installation on RedHat systems, stderr
   // is not properly hooked up to a terminal when checking configuration during post install scripts
   // which would cause error output to go only to syslog and be hidden from view during install
   if ((forceStderr || core::system::stderrIsTerminal()) && !description.empty())
   {
      std::cerr << description << std::endl;
   }

   core::log::logError(error, location);
}

void reportError(const std::string& errorMessage, const ErrorLocation& location, bool forceStderr)
{
   if (core::system::stderrIsTerminal())
   {
      std::cerr << errorMessage << std::endl;
   }
   else
   {
      // see above for rationale behind forceStderr
      if (forceStderr)
         std::cerr << errorMessage << std::endl;

      core::log::logErrorMessage(errorMessage, location);
   }
}

void reportWarnings(const std::string& warningMessages,
                    const ErrorLocation& location)
{
   if (core::system::stderrIsTerminal())
      std::cerr << "WARNINGS: " << warningMessages << std::endl;
   else
      core::log::logWarningMessage(warningMessages, location);
}

void parseCommandLine(variables_map& vm,
                      const OptionsDescription& optionsDescription,
                      const options_description& commandLineOptions,
                      int argc,
                      const char * const argv[],
                      std::vector<std::string>* pUnrecognized)
{
   // parse the command line
   command_line_parser parser(argc, const_cast<char**>(argv));
   parser.options(commandLineOptions);
   parser.positional(optionsDescription.positionalOptions);
   if (pUnrecognized != nullptr)
      parser.allow_unregistered();
   parsed_options parsed = parser.run();
   store(parsed, vm);
   notify(vm);

   // collect unrecognized if necessary
   if (pUnrecognized != nullptr)
   {
      *pUnrecognized = collect_unrecognized(parsed.options,
                                            include_positional);
   }
}

bool parseConfigFile(variables_map& vm,
                     const std::string& configFile,
                     const OptionsDescription& optionsDescription,
                     bool allowUnregisteredConfigOptions)
{
   // open the config file
   if (!configFile.empty())
   {
      std::shared_ptr<std::istream> pIfs;
      Error error = FilePath(configFile).openForRead(pIfs);
      if (error)
      {
         error.addProperty("description", "Unable to open config file: " + configFile);
         reportError(error, ERROR_LOCATION, true);

         return false;
      }

      try
      {
         // parse config file
         store(parse_config_file(*pIfs, optionsDescription.configFile, allowUnregisteredConfigOptions), vm);
         notify(vm);
      }
      catch(const std::exception& e)
      {
         reportError(
           "Error reading " + configFile + ": " + std::string(e.what()),
           ERROR_LOCATION,
           true);

         return false;
      }
   }

   return true;
}


ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc,
                   const char * const argv[],
                   std::vector<std::string>* pUnrecognized,
                   bool* pHelp,
                   bool allowUnregisteredConfigOptions,
                   bool configFileHasPrecedence)
{
   *pHelp = false;
   std::string configFile;
   OptionsParseState state = OptionsParseState::Initial;
   try
   {        
      // general options
      options_description general("general");
      general.add_options()
         ("help", "print help message")
         ("test-config", "test to ensure the config file is valid")
         ("config-file",
           value<std::string>(&configFile)->default_value(
                                    optionsDescription.defaultConfigFilePath),
           std::string("configuration file").c_str());

      
      // make copy of command line options so we can add general to them
      options_description commandLineOptions(optionsDescription.commandLine);
      commandLineOptions.add(general);
      
      variables_map vm;

      // the order of parsing is determined based on whether or not the config file has precedence
      // if it does, parse it first, otherwise parse the command line first
      if (configFileHasPrecedence)
      {
         // if we are parsing the config file first, do not attempt to parse
         // the config file path from the command line arguments - just use
         // the default value that was passed in
         configFile = optionsDescription.defaultConfigFilePath;

         state = OptionsParseState::ConfigFile;
         if (!parseConfigFile(vm, configFile, optionsDescription, allowUnregisteredConfigOptions))
            return ProgramStatus::exitFailure();

         state = OptionsParseState::CommandLine;
         parseCommandLine(vm, optionsDescription, commandLineOptions, argc, argv, pUnrecognized);
      }
      else
      {
         state = OptionsParseState::CommandLine;
         parseCommandLine(vm, optionsDescription, commandLineOptions, argc, argv, pUnrecognized);

         // "none" is a special sentinel value for the config-file which
         // explicitly prevents us from reading the default config file above
         // now that we are past that we can reset it to empty
         if (configFile == "none")
            configFile = "";

         state = OptionsParseState::ConfigFile;
         if (!parseConfigFile(vm, configFile, optionsDescription, allowUnregisteredConfigOptions))
            return ProgramStatus::exitFailure();
      }

      // show help if requested
      if (vm.count("help"))
      {
         *pHelp = true;
         std::cout << commandLineOptions;
         return ProgramStatus::exitSuccess();
      }
      
      // validate all options are provided
      else 
      {
         if (!validateOptionsProvided(vm, optionsDescription.commandLine))
            return ProgramStatus::exitFailure();
         
         if (!configFile.empty())
         {
            if (!validateOptionsProvided(vm,
                                         optionsDescription.configFile,
                                         configFile))
               return ProgramStatus::exitFailure();
         }
      }

      // if this was a config-test then return exitSuccess, otherwise run
      if (vm.count("test-config"))
      {
         return ProgramStatus::exitSuccess();
      }
      else
      {
         return ProgramStatus::run();
      }
   }
   catch(const boost::program_options::error& e)
   {
      std::string msg(e.what());
      if (state == OptionsParseState::CommandLine)
         msg += " on command line";
      else if (!configFile.empty())
         msg += " in config file " + configFile;
      reportError(msg, ERROR_LOCATION, true);
      return ProgramStatus::exitFailure();
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // keep compiler happy
   return ProgramStatus::exitFailure();
}

   
} // namespace program_options
} // namespace core
} // namespace rstudio
