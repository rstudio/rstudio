/*
 * RegexUtils.hpp
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

#ifndef CORE_REGEX_UTILS_HPP
#define CORE_REGEX_UTILS_HPP

#include <string>
#include <vector>

#include <boost/regex.hpp>
#include <boost/iostreams/filter/regex.hpp>

#include <core/regex/RegexMatch.hpp>
#include <core/regex/RegexSearch.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace regex_utils {
   
// convert a pattern which includes wildcard (i.e. '*') characters
// into a regulard expression
boost::regex wildcardPatternToRegex(const std::string& pattern);


// returns a regex if the passed term contains a wildcard (otherwise
// returns an empty regex)
boost::regex regexIfWildcardPattern(const std::string& term);

bool textMatches(const std::string& text,
                 const boost::regex& regex,
                 bool prefixOnly,
                 bool caseSensitive);

core::Error filterString(
                const std::string& input,
                const std::vector<boost::iostreams::regex_filter>& filters,
                std::string* pOutput);

core::Error filterString(
                const std::string& input,
                const boost::iostreams::regex_filter& filter,
                std::string* pOutput);

// helper functions with slightly more intuitive ordering
// of arguments
template <typename StringType, typename MatchType>
bool match(const boost::regex& rePattern,
           const StringType& string,
           MatchType* pMatch)
{
   return regex_utils::match(string, *pMatch, rePattern);
}

template <typename StringType, typename MatchType>
bool search(const boost::regex& rePattern,
            const StringType& string,
            MatchType* pMatch)
{
   return regex_utils::search(string, *pMatch, rePattern);
}

} // namespace regex_utils
} // namespace core 
} // namespace rstudio


#endif // CORE_REGEX_UTILS_HPP

