/*
 * Debug.hpp
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

#ifndef CORE_DEBUG_HPP
#define CORE_DEBUG_HPP

#include <iostream>
#include <shared_core/json/Json.hpp>
#include <vector>
#include <map>

namespace rstudio {
namespace core {
namespace debug {

template <typename K, typename V>
void print(const std::map<K, V>& map, std::ostream& os = std::cerr)
{
   json::Object object;
   for (typename std::map<K, V>::const_iterator it = map.begin();
        it != map.end();
        ++it)
   {
      object[it->first] = json::toJsonValue(it->second);
   }
   object.writeFormatted(os);
   os << std::endl;
}

template <typename ConvertibleToArray>
void print(const ConvertibleToArray& object, std::ostream& os = std::cerr)
{
   typedef typename ConvertibleToArray::value_type value_type;
   std::vector<value_type> asVector(object.begin(), object.end());
   json::toJsonArray(asVector).writeFormatted(os);
   os << std::endl;
}

#ifdef _WIN32
// log messages to an open Notepad window
// (useful when you just need to dump logs somewhere easily visible)
void logToNotepad(const char* fmt, ...);
#endif

} // namespace debug
} // namespace core
} // namespace rstudio

#endif /* CORE_DEBUG_HPP */

