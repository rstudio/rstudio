/*
 * Environment.hpp
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

#ifndef CORE_SYSTEM_ENVIRONMENT_HPP
#define CORE_SYSTEM_ENVIRONMENT_HPP

#include <string>

#include <core/system/Types.hpp>

namespace core {
namespace system {

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
Options environment();

// get an environment variable within an Options structure
std::string getenv(const std::string& name, const Options& environment);


/****************************************************************
   Manipulating the memory environment variable structure. These
   functions are typically used for preparing values to be passed
   as ProcessOptions::environnent
*****************************************************************/

// set an environment variable within an Options structure (replaces
// any existing value)
void setenv(const std::string& name,
            const std::string& value,
            Options* pEnvironment);

// remove an enviroment variable from an Options structure
void unsetenv(const std::string& name, Options* pEnvironment);

// add to the PATH within an Options struture
void addToPath(const std::string& filePath, Options* pEnvironment);


} // namespace system
} // namespace core

#endif // CORE_SYSTEM_ENVIRONMENT_HPP
