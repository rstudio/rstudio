/*
 * Map.hpp
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
#ifndef CORE_ALGORITHM_MAP_HPP
#define CORE_ALGORITHM_MAP_HPP

// Convenience algorithms -- not necessarily as fast as just
// using the STL + iterators, but often easier to use.

#include <vector>
#include <map>
#include <algorithm>

namespace rstudio {
namespace core {
namespace algorithm {

template <typename key_type, typename mapped_type>
std::vector<key_type> map_keys(const std::map<key_type, mapped_type>& map)
{
   std::vector<key_type> result;
   result.reserve(map.size());
   for (typename std::map<key_type, mapped_type>::const_iterator it = map.begin();
        it != map.end();
        ++it)
   {
      result.push_back(it->first);
   }
   return result;
}

template <typename key_type, typename mapped_type>
std::vector<mapped_type> map_values(const std::map<key_type, mapped_type>& map)
{
   std::vector<key_type> result;
   result.reserve(map.size());
   for (typename std::map<key_type, mapped_type>::const_iterator it = map.begin();
        it != map.end();
        ++it)
   {
      result.push_back(it->second);
   }
   return result;
}

} // namespace algorithm
} // namespace core
} // namespace rstudio

#endif

