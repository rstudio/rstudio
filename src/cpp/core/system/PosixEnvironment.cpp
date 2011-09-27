/*
 * PosixEnvironment.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/Environment.hpp>

#include <stdlib.h>

extern char **environ;

namespace core {
namespace system {

Options environment()
{
   Options options;
   for (char **env = environ; *env; ++env)
   {
      std::string envVar(*env);
      std::string::size_type pos = envVar.find("=") ;
      if ( pos != std::string::npos )
      {
         std::string key = envVar.substr(0, pos) ;
         std::string value;
         if ( (pos + 1) < envVar.size() )
            value = envVar.substr(pos + 1) ;
         options.push_back(std::make_pair(key,value));
      }
   }

   return options;
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

