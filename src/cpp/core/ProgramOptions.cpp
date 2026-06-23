/*
 * ProgramOptions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <shared_core/FilePath.hpp>

#include <core/Log.hpp>
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
#ifdef RSTUDIO_DEVELOPMENT_BUILD
         std::cerr << "-- Reading config file: " << configFile << std::endl;
#endif

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

// Collect all unrecognized keys from the config file by parsing with
// allow_unregistered=true. Used by --check-config to report every
// unrecognized key in a single pass rather than stopping at the first.
// On a read failure pError is set; a successful read leaves it unmodified.
std::vector<std::string> collectUnrecognizedConfigKeys(
      const std::string& configFile,
      const OptionsDescription& optionsDescription,
      Error* pError)
{
   std::vector<std::string> unrecognized;

   if (configFile.empty())
      return unrecognized;

   std::shared_ptr<std::istream> pIfs;
   Error error = FilePath(configFile).openForRead(pIfs);
   if (error)
   {
      if (pError)
         *pError = error;
      return unrecognized;
   }

   // Parse with allow_unregistered=true to collect every unrecognized key in a
   // single pass. Only an invalid-syntax error is caught here -- it is already
   // reported by the main parse, which runs before this scan -- so that any
   // other exception propagates rather than being silently reported as a clean
   // check.
   try
   {
      parsed_options parsed = parse_config_file(*pIfs,
                                                optionsDescription.configFile,
                                                true /* allow_unregistered */);

      // collect the key name of each unregistered option; we use string_key
      // rather than collect_unrecognized() so we report only the option names
      // and not their values
      for (const auto& option : parsed.options)
      {
         if (option.unregistered && !option.string_key.empty())
            unrecognized.push_back(option.string_key);
      }
   }
   catch(const boost::program_options::invalid_config_file_syntax&)
   {
      // a syntax error would already have been reported by the main parse
   }

   return unrecognized;
}


ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc,
                   const char * const argv[],
                   std::vector<std::string>* pUnrecognized,
                   bool* pHelp,
                   bool allowUnregisteredConfigOptions,
                   bool configFileHasPrecedence,
                   bool deferCheckConfig)
{
   *pHelp = false;
   std::string configFile;
   OptionsParseState state = OptionsParseState::Initial;

   // detect --check-config (and its deprecated alias --test-config) early via a
   // raw argv scan so we can relax config parsing before the full parse runs;
   // this lets us collect ALL unrecognized keys in a single pass rather than
   // stopping at the first
   bool checkConfigMode = false;
   for (int i = 1; i < argc; ++i)
   {
      std::string arg(argv[i]);
      if (arg == "--check-config" || arg == "--test-config")
      {
         checkConfigMode = true;
         break;
      }
   }
   if (checkConfigMode)
      allowUnregisteredConfigOptions = true;

   try
   {
      // general options
      options_description general("general");
      general.add_options()
         ("help", "print help message")
         ("test-config", "deprecated alias for --check-config")
         ("check-config", "validate the configuration file and report all unrecognized options in a single pass")
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

      // run the configuration check for --check-config (or its deprecated
      // alias --test-config); otherwise start normally
      if (checkConfigMode)
      {
         // --test-config is retained as a deprecated alias for --check-config
         if (vm.count("test-config"))
         {
            std::cerr << "Warning: --test-config is deprecated and will be removed "
                         "in a future release; use --check-config instead."
                      << std::endl;
         }

         // report a missing config file explicitly rather than as a clean
         // check, so a typo'd or absent path is not mistaken for success. The
         // PASS/FAIL verdict goes to stdout; the exit code is the contract.
         if (configFile.empty())
         {
            std::cout << "[FAIL] No configuration file found to validate; "
                         "specify one with --config-file" << std::endl;
            return ProgramStatus::exitFailure();
         }

         // collect every unrecognized option in a single pass
         Error scanError;
         std::vector<std::string> unrecognizedKeys =
               collectUnrecognizedConfigKeys(configFile, optionsDescription, &scanError);
         if (scanError)
         {
            std::cout << "[FAIL] Config file " << configFile << ": "
                      << scanError.getSummary() << std::endl;
            return ProgramStatus::exitFailure();
         }

         if (unrecognizedKeys.empty())
         {
            std::cout << "[PASS] Config file " << configFile
                      << ": no unrecognized options found" << std::endl;
            // When the caller has requested deferred mode it will run additional
            // extended checks (file-path existence, R installation, etc.) before
            // deciding the final exit code, so hand control back via run().
            // In non-deferred mode (rsession, postback, ...) exit immediately as
            // before so that behaviour is completely unchanged for those callers.
            if (deferCheckConfig)
               return ProgramStatus::run();
            return ProgramStatus::exitSuccess();
         }

         std::cout << "[FAIL] Config file " << configFile
                   << ": unrecognized option(s):" << std::endl;
         for (const std::string& key : unrecognizedKeys)
            std::cout << "  - " << key << std::endl;
         return ProgramStatus::exitFailure();
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
