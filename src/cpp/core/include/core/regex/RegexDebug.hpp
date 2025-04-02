/*
 * RegexDebug.hpp
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

#ifndef CORE_REGEX_DEBUG_HPP
#define CORE_REGEX_DEBUG_HPP

#include <iostream>

namespace rstudio {
namespace core {
namespace regex_utils {

// log details about a match
template <typename MatchType>
void debugLog(const MatchType& match)
{
   std::cerr << "-- m.size() == " << match.size() << std::endl;
   std::cerr << "-- m.empty() == " << match.empty() << std::endl;
   for (std::size_t i = 0; i < match.size(); i++)
   {
      std::cerr << "-- m[" << i << "] == ";
      if (match[i].matched)
      {
         std::cerr << "\"" << match[i] << "\"" << std::endl;
      }
      else
      {
         std::cerr << "(no match)" << std::endl;
      }
   }
}

} // namespace regex_utils
} // namespace core
} // namespace rstudio

#endif // CORE_REGEX_DEBUG_HPP

