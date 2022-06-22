/*
 * ProgramOptions.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <set>
#include <string>
#include <vector>
#include <sstream>

#include <boost/algorithm/string.hpp>
#include <boost/utility.hpp>
#include <boost/program_options.hpp>

#include <core/ProgramStatus.hpp>

namespace std
{
   // needed for boost to compile std::vector<T> default value for option
   template<typename T>
   inline std::ostream& operator<<(std::ostream &os, const std::vector<T>& vec)
   {
      for (auto item : vec)
      {
         os << item << " ";
      }

      return os;
   }

   // needed for boost to compile std::set<T> default value for option
   template<typename T>
   inline std::ostream& operator<<(std::ostream &os, const std::set<T, std::less<T>, std::allocator<T>>& set)
   {
      for (auto item : set)
      {
         os << item << ",";
      }

      return os;
   }
   
   // needed to parse comma-separated lists from options files directly into a set
   // There must be an operator>>(std::istream&, T&) in the std namespace to compile for type T
   template <typename T>
   inline std::istream& operator>>(std::istream& is, std::set<T, std::less<T>, std::allocator<T>>& set)
   {
      std::string list;
      is >> list;

      std::vector<std::string> splitList;
      boost::split(splitList, list, boost::is_any_of(","));

      for (const auto& strVal : splitList)
      {
         T value;
         std::stringstream stream(strVal);
         stream >> value;
         if (stream.fail())
         {
            is.setstate(stream.rdstate());
            return is;
         }
         set.insert(value);
      }

      return is;
   }
}

namespace rstudio {
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
   std::string programName;
   std::string defaultConfigFilePath;
   boost::program_options::options_description commandLine;
   boost::program_options::positional_options_description positionalOptions;
   boost::program_options::options_description configFile;
};


ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc,
                   const char * const argv[],
                   std::vector<std::string>* pUnrecognized,
                   bool* pHelp,
                   bool allowUnregisteredConfigOptions = false,
                   bool configFileHasPrecedence = false);

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          bool* pHelp,
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false)
{
   return read(optionsDescription, argc, argv, NULL, pHelp,
               allowUnregisteredConfigOptions, configFileHasPrecedence);
}

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          std::vector<std::string>* pUnrecognized,
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false)
{
   bool help;
   return read(optionsDescription, argc, argv, pUnrecognized, &help,
               allowUnregisteredConfigOptions, configFileHasPrecedence);
}

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false)
{
   bool help;
   return read(optionsDescription, argc, argv, &help,
               allowUnregisteredConfigOptions, configFileHasPrecedence);
}

void reportError(const Error& error,
                 const ErrorLocation& location,
                 bool forceStdErr = false);

void reportError(const std::string& errorMessage,
                 const ErrorLocation& location,
                 bool forceStdErr = false);

void reportWarnings(const std::string& warningMessages,
                    const ErrorLocation& location);

} // namespace program_options
} // namespace core
} // namespace rstudio

#endif // CORE_PROGRAM_OPTIONS_HPP

