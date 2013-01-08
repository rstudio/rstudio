/*
 * Environment.hpp
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

#ifndef CORE_SYSTEM_ENVIRONMENT_HPP
#define CORE_SYSTEM_ENVIRONMENT_HPP

#include <string>

#include <core/system/Types.hpp>

namespace core {
namespace system {

// NOTE: all environment variables are UTF8-encoded


/****************************************************************
   Direct manipulation of global environment
*****************************************************************/

std::string getenv(const std::string& name);
void setenv(const std::string& name, const std::string& value);
void unsetenv(const std::string& name);


/****************************************************************
   Read current environment into memory and access its variables
*****************************************************************/

// get a copy of all current environment variables
void environment(Options* pEnvironment);

// get an environment variable within an Options structure
std::string getenv(const Options& environment, const std::string& name);


/****************************************************************
   Manipulating the memory environment variable structure. These
   functions are typically used for preparing values to be passed
   as ProcessOptions::environnent
*****************************************************************/

// set an environment variable within an Options structure (replaces
// any existing value)
void setenv(Options* pEnvironment,
            const std::string& name,
            const std::string& value);

// remove an enviroment variable from an Options structure
void unsetenv(Options* pEnvironment,
              const std::string& name);

void getModifiedEnv(const Options& extraVars, Options* pEnv);

// add to the PATH within a string
void addToPath(std::string* pPath,
               const std::string& filePath,
               bool prepend = false);

// add to the PATH within an Options struture
void addToPath(Options* pEnvironment,
               const std::string& filePath,
               bool prepend = false);


/****************************************************************
   Utility functions
*****************************************************************/

bool parseEnvVar(const std::string envVar, Option* pEnvVar);

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_ENVIRONMENT_HPP
