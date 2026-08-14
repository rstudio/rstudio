/*
 * PosixEnvironment.cpp
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

#include <core/system/Environment.hpp>

#include <stdlib.h>

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/system/EnvironmentLock.hpp>

extern char **environ;

namespace rstudio {
namespace core {
namespace system {

namespace impl {

bool optionIsNamed(const Option& option, const std::string& name)
{
   return boost::algorithm::equals(option.first, name);
}

} // namespace impl

void environment(Options* pEnvironment)
{
   EnvironmentLock lock;

   for (char **env = environ; *env; ++env)
   {
      Option envVar;
      if (parseEnvVar(std::string(*env), &envVar))
         pEnvironment->push_back(envVar);
   }
}

std::string getenv(const std::string& name)
{
   EnvironmentLock lock;

   char * value = ::getenv(name.c_str());
   if (value)
      return std::string(value);
   else
      return std::string();
}

bool getenv(const std::string& name, std::string* pValue)
{
   EnvironmentLock lock;

   char* value = ::getenv(name.c_str());
   if (value == nullptr)
      return false;

   *pValue = value;
   return true;
}

void setenv(const std::string& name, const std::string& value)
{
   EnvironmentLock lock;

   ::setenv(name.c_str(), value.c_str(), 1);
}

void unsetenv(const std::string& name)
{
   EnvironmentLock lock;

   ::unsetenv(name.c_str());
}


} // namespace system
} // namespace core
} // namespace rstudio
