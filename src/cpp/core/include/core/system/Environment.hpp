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

namespace core {
namespace system {

// basic get and set
std::string getenv(const std::string& name);
void setenv(const std::string& name, const std::string& value);
void unsetenv(const std::string& name);


} // namespace system
} // namespace core

#endif // CORE_SYSTEM_ENVIRONMENT_HPP
