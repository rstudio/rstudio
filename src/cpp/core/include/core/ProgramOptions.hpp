/*
 * ProgramOptions.hpp
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


// Returns true if --check-config (or its deprecated alias --test-config) is
// present in argv.  Used to avoid duplicating the raw argv scan in multiple
// callers.
bool detectCheckConfigMode(int argc, const char* const argv[]);

// Returns true if --setup-db is present in argv.
bool detectSetupDbMode(int argc, const char* const argv[]);

// Returns true if --setup-db-show-password is present in argv. Used by --setup-db.
bool detectShowPassword(int argc, const char* const argv[]);

// Returns true if --setup-db-print-only is present in argv. Used by --setup-db.
bool detectPrintOnly(int argc, const char* const argv[]);

// Returns the value of --setup-db-master-password-file <path> (or --setup-db-master-password-file=<path>)
// if present in argv, or an empty string otherwise. Used by --setup-db.
std::string extractMasterPasswordFile(int argc, const char* const argv[]);

// Returns the value of --setup-db-host <host> (or --setup-db-host=<host>) if present in argv,
// or an empty string otherwise. Used by --setup-db to skip the interactive
// PostgreSQL host prompt when supplied.
std::string extractHost(int argc, const char* const argv[]);

// Returns the value of --setup-db-port <port> (or --setup-db-port=<port>) if present in argv,
// or an empty string otherwise. Used by --setup-db to skip the interactive
// PostgreSQL port prompt when supplied.
std::string extractPort(int argc, const char* const argv[]);

// Returns the value of --setup-db-master-username <user> (or --setup-db-master-username=<user>)
// if present in argv, or an empty string otherwise. Used by --setup-db to
// skip the interactive master username prompt when supplied.
std::string extractMasterUsername(int argc, const char* const argv[]);

// Returns the value of --setup-db-database-name <name> (or --setup-db-database-name=<name>) if
// present in argv, or an empty string otherwise. Used by --setup-db to skip
// the interactive database name prompt when supplied.
std::string extractDatabaseName(int argc, const char* const argv[]);

// Returns the value of --setup-db-database-user <user> (or --setup-db-database-user=<user>) if
// present in argv, or an empty string otherwise. Used by --setup-db to skip
// the interactive database user prompt when supplied.
std::string extractDatabaseUser(int argc, const char* const argv[]);

// Primary overload.  When deferCheckConfig is true and --check-config (or the
// deprecated --test-config alias) is present, the function performs the syntax
// parse and prints the per-file [PASS]/[FAIL] line but, on a clean result,
// returns ProgramStatus::run() instead of exitSuccess so that the caller can
// run additional extended checks before exiting.  When deferCheckConfig is
// false (the default) the original behaviour is preserved: a clean check
// returns exitSuccess immediately.
ProgramStatus read(const OptionsDescription& optionsDescription,
                   int argc,
                   const char * const argv[],
                   std::vector<std::string>* pUnrecognized,
                   bool* pHelp,
                   bool allowUnregisteredConfigOptions = false,
                   bool configFileHasPrecedence = false,
                   bool deferCheckConfig = false);

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          bool* pHelp,
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false,
                          bool deferCheckConfig = false)
{
   return read(optionsDescription, argc, argv, NULL, pHelp,
               allowUnregisteredConfigOptions, configFileHasPrecedence,
               deferCheckConfig);
}

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          std::vector<std::string>* pUnrecognized,
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false,
                          bool deferCheckConfig = false)
{
   bool help;
   return read(optionsDescription, argc, argv, pUnrecognized, &help,
               allowUnregisteredConfigOptions, configFileHasPrecedence,
               deferCheckConfig);
}

inline ProgramStatus read(const OptionsDescription& optionsDescription,
                          int argc,
                          const char * const argv[],
                          bool allowUnregisteredConfigOptions = false,
                          bool configFileHasPrecedence = false,
                          bool deferCheckConfig = false)
{
   bool help;
   return read(optionsDescription, argc, argv, &help,
               allowUnregisteredConfigOptions, configFileHasPrecedence,
               deferCheckConfig);
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

