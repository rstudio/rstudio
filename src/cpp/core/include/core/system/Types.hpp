/*
 * Types.hpp
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

#ifndef CORE_SYSTEM_TYPES_HPP
#define CORE_SYSTEM_TYPES_HPP

#include <string>
#include <vector>

namespace core {
namespace system {

typedef std::pair<std::string,std::string> Option;
typedef std::vector<Option> Options;

inline bool optionIsNamed(const Option& option, const std::string& name)
{
   return option.first == name;
}

} // namespace system
} // namespace stypes

#endif // CORE_SYSTEM_TYPES_HPP
