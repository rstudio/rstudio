/*
 * Environment.cpp
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

#include <core/system/Environment.hpp>

#include <algorithm>
#include <boost/regex.hpp>

#include <core/Algorithm.hpp>

#include <boost/bind.hpp>

#ifdef _WIN32
#define kPathSeparator ";"
#else
#define kPathSeparator ":"
#endif

namespace rstudio {
namespace core {
namespace system {

namespace impl {

// platform-specific name matching (Win32 is case-insensitive)
bool optionIsNamed(const Option& option, const std::string& name);

} // namespace impl

// get an environment variable within an Options structure
std::string getenv(const Options& environment, const std::string& name)
{
   Options::const_iterator it = std::find_if(
                                 environment.begin(),
                                 environment.end(),
                                 boost::bind(impl::optionIsNamed, _1, name));

   if (it != environment.end())
      return it->second;
   else
      return std::string();
}

void getModifiedEnv(const Options& extraVars, Options* pEnv)
{
   core::system::environment(pEnv);
   for (const auto& var : extraVars)
   {
      core::system::setenv(pEnv, var.first, var.second);
   }
}

// set an environment variable within an Options structure (replaces
// any existing value)
void setenv(Options* pEnvironment,
            const std::string& name,
            const std::string& value)
{
   Options::iterator it = std::find_if(
                                 pEnvironment->begin(),
                                 pEnvironment->end(),
                                 boost::bind(impl::optionIsNamed, _1, name));
   if (it != pEnvironment->end())
      *it = std::make_pair(name, value);
   else
      pEnvironment->push_back(std::make_pair(name, value));
}

// remove an enviroment variable from an Options structure
void unsetenv(Options* pEnvironment, const std::string& name)
{
   pEnvironment->erase(std::remove_if(pEnvironment->begin(),
                                      pEnvironment->end(),
                                      boost::bind(impl::optionIsNamed,
                                                  _1,
                                                  name)),
                       pEnvironment->end());
}

namespace {

std::string modifyPath(const std::string& path,
                       const std::string& entry,
                       bool prepend)
{
   // if the PATH is empty, we can just use the
   // requested entry as-is
   if (path.empty())
      return entry;

   // otherwise, prepend or append as appropriate
   return prepend
         ? entry + kPathSeparator + path
         : path + kPathSeparator + entry;
}

} // end anonymous namespace

void addToPath(const std::string& filePath,
               bool prepend)
{
   std::string oldPath = getenv("PATH");
   std::string newPath = modifyPath(oldPath, filePath, prepend);
   setenv("PATH", newPath);
}

void addToPath(std::string* pPath,
               const std::string& filePath,
               bool prepend)
{
   *pPath = modifyPath(*pPath, filePath, prepend);
}

// add to the PATH within an Options struture
void addToPath(Options* pEnvironment,
               const std::string& filePath,
               bool prepend)
{
   std::string oldPath = getenv(*pEnvironment, "PATH");
   std::string newPath = modifyPath(oldPath, filePath, prepend);
   setenv(pEnvironment, "PATH", newPath);
}

bool parseEnvVar(const std::string envVar, Option* pEnvVar)
{
   std::string::size_type pos = envVar.find("=");
   if ( pos != std::string::npos )
   {
      std::string key = envVar.substr(0, pos);
      std::string value;
      if ( (pos + 1) < envVar.size() )
         value = envVar.substr(pos + 1);
      *pEnvVar = std::make_pair(key,value);
      return true;
   }
   else
   {
      return false;
   }
}

std::string expandEnvVars(const Options& environment, const std::string& str)
{
   std::string result(str);
   for (const auto& pair: environment)
   {
      // replace bare forms (/home/$USER)
      boost::regex reVar("\\$" + pair.first + "\\>");
      result = boost::regex_replace(result, reVar, pair.second);

      // replace curly brace forms (/home/${USER})
      boost::regex reBraceVar("\\${" + pair.first + "}");
      result = boost::regex_replace(result, reBraceVar, pair.second);
   }
   return result;
}

} // namespace system
} // namespace core
} // namespace rstudio
