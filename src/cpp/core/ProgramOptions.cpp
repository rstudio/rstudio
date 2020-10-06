/*
 * ProgramOptions.cpp
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
         reportError(msg, ERROR_LOCATION);
         return false;
      }
   }
   
   // all options validated
   return true;
}

}

void reportError(const Error& error, const ErrorLocation& location)
{
   std::string description = error.getProperty("description");
   if (core::system::stderrIsTerminal() && !description.empty())
      std::cerr << description << std::endl;

   core::log::logError(error, location);
}

void reportError(const std::string& errorMessage, const ErrorLocation& location)
{
   if (core::system::stderrIsTerminal())
      std::cerr << errorMessage << std::endl;
   else
      core::log::logErrorMessage(errorMessage, location);
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
         reportError(error, ERROR_LOCATION);

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
           ERROR_LOCATION);

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

         if (!parseConfigFile(vm, configFile, optionsDescription, allowUnregisteredConfigOptions))
            return ProgramStatus::exitFailure();

         parseCommandLine(vm, optionsDescription, commandLineOptions, argc, argv, pUnrecognized);
      }
      else
      {
         parseCommandLine(vm, optionsDescription, commandLineOptions, argc, argv, pUnrecognized);

         // "none" is a special sentinel value for the config-file which
         // explicitly prevents us from reading the default config file above
         // now that we are past that we can reset it to empty
         if (configFile == "none")
            configFile = "";

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
      if (!configFile.empty())
         msg += " in config file " + configFile;
      reportError(msg, ERROR_LOCATION);
      return ProgramStatus::exitFailure();
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // keep compiler happy
   return ProgramStatus::exitFailure();
}

   
} // namespace program_options
} // namespace core
} // namespace rstudio
