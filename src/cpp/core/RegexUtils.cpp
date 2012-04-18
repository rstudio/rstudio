/*
 * RegexUtils.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/RegexUtils.hpp>

#include <vector>

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/StringUtils.hpp>

namespace core {
namespace regex_utils {

boost::regex wildcardPatternToRegex(const std::string& pattern)
{
   // split into componenents
   using namespace boost::algorithm;
   std::vector<std::string> components;
   split(components, pattern, is_any_of("*"), token_compress_on);

   // build and return regex
   std::string regex;
   for (std::size_t i=0; i<components.size(); i++)
   {
      if (i > 0)
         regex.append(".*");
      regex.append("\\Q");
      regex.append(components.at(i));
      regex.append("\\E");
   }
   return boost::regex(regex);
}

bool textMatches(const std::string& text,
                 const boost::regex& regex,
                 bool prefixOnly,
                 bool caseSensitive)
{
   boost::smatch match;
   boost::match_flag_type flags = boost::match_default;
   if (prefixOnly)
      flags |= boost::match_continuous;
   return regex_search(caseSensitive ? text : string_utils::toLower(text),
                       match,
                       regex,
                       flags);
}


} // namespace regex_utils
} // namespace core 



