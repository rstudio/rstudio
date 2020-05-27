/*
 * PosixEnvironment.cpp
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

#include <stdlib.h>

#include <boost/algorithm/string/predicate.hpp>

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
   for (char **env = environ; *env; ++env)
   {
      Option envVar;
      if (parseEnvVar(std::string(*env), &envVar))
         pEnvironment->push_back(envVar);
   }
}

std::string getenv(const std::string& name)
{
   char * value = ::getenv(name.c_str());
   if (value)
      return std::string(value);
   else
      return std::string();
}

void setenv(const std::string& name, const std::string& value)
{
   ::setenv(name.c_str(), value.c_str(), 1);
}

void unsetenv(const std::string& name)
{
   ::unsetenv(name.c_str());
}


} // namespace sytem
} // namespace core
} // namespace rstudio

