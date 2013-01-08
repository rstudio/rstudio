/*
 * ProgramOptions.cpp
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

#include <core/ProgramOptions.hpp>

#include <string>
#include <iostream>

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/ProgramStatus.hpp>
#include <core/system/System.hpp>

using namespace boost::program_options ;

namespace core {

namespace program_options {
 
namespace {

bool validateOptionsProvided(const variables_map& vm,
                             const options_description& optionsDescription,
                             const std::string& configFile = std::string())
{
   BOOST_FOREACH( const boost::shared_ptr<option_description>& pOptionsDesc, 
                  optionsDescription.options() )
   {
      std::string optionName = pOptionsDesc->long_name();
      if ( !(vm.count(optionName)) )
      {
         std::string msg = "Required option " + optionName + " not specified";
         if (!configFile.empty())
            msg += " in config file " + configFile;
         reportError(msg, ERROR_LOCATION);
         return false ;
      }
   }
   
   // all options validated
   return true;
}

}
  
void reportError(const std::string& errorMessage, const ErrorLocation& location)
{
   if (core::system::stderrIsTerminal())
      std::cerr << errorMessage << std::endl;
   else
      core::log::logErrorMessage(errorMessage, location);
}


ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc, 
                   char * const argv[])
{
   std::string configFile;
   try
   {        
      // general options
      options_description general("general") ;
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
      
      // parse the command line
      variables_map vm ;
      command_line_parser parser(argc, const_cast<char**>(argv));
      store(parser.options(commandLineOptions).
            positional(optionsDescription.positionalOptions).run(), vm);
      notify(vm) ;

      // "none" is a special sentinel value for the config-file which
      // explicitly prevents us from reading the defautl config file above
      // now that we are past that we can reset it to empty
      if (configFile == "none")
         configFile = "";
      
      // open the config file
      if (!configFile.empty())
      {
         boost::shared_ptr<std::istream> pIfs;
         Error error = FilePath(configFile).open_r(&pIfs);
         if (error)
         {
            reportError("Unable to open config file: " + configFile,
                        ERROR_LOCATION);
            return ProgramStatus::exitFailure() ;
         }
         
         try
         {
            // parse config file
            store(parse_config_file(*pIfs, optionsDescription.configFile), vm) ;
            notify(vm) ;
         }
         catch(const std::exception& e)
         {
            reportError(
              "IO error reading " + configFile + ": " + std::string(e.what()),
              ERROR_LOCATION);

            return ProgramStatus::exitFailure();
         }
      }
      
      // show help if requested
      if (vm.count("help"))
      {
         std::cout << commandLineOptions ;
         return ProgramStatus::exitSuccess() ;
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
         return ProgramStatus::run() ;
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
