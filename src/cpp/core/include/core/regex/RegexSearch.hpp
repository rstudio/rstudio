/*
 * RegexSearch.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef CORE_REGEX_REGEX_SEARCH_HPP
#define CORE_REGEX_REGEX_SEARCH_HPP

#include <string>
#include <iostream>

#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

#include <core/regex/RegexCommon.hpp>

namespace rstudio {
namespace core {
namespace regex_utils {

template <class IteratorType, class MatchType, class PatternType>
bool search(IteratorType begin,
            IteratorType end,
            MatchType& m,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   bool result = false;
   
   try
   {
      result = boost::regex_search(begin, end, m, pattern, flags);
   }
   catch (std::exception& e)
   {
      detail::reportException(
               "boost::regex_search()",
               pattern,
               e);
   }
   
   return result;
}

template <class IteratorType, class PatternType>
bool search(IteratorType begin,
            IteratorType end,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   boost::match_results<IteratorType> m;
   return search(begin, end, m, pattern, flags | boost::regex_constants::match_any);
}


template <class CharType, class MatchType, class PatternType>
bool search(const CharType* string,
            MatchType& m,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   return search(string, string + ::strlen(string), m, pattern, flags);
}

template <class CharType, class PatternType>
bool search(const CharType* string,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   boost::match_results<const CharType*> m;
   return search(string, string + ::strlen(string), m, pattern, flags | boost::regex_constants::match_any);
}

template <class MatchType, class PatternType>
bool search(const std::string& string,
            MatchType& m,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   return search(string.begin(), string.end(), m, pattern, flags);
}

template <class MatchType, class PatternType>
bool search(const std::wstring& string,
            MatchType& m,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   return search(string.begin(), string.end(), m, pattern, flags);
}

template <class StringType, class PatternType>
bool search(const StringType& string,
            const PatternType& pattern,
            boost::match_flag_type flags = boost::match_default)
{
   boost::match_results<typename StringType::const_iterator> m;
   return search(string.begin(), string.end(), m, pattern, flags | boost::regex_constants::match_any);
}

} // end namespace regex_utils
} // end namespace core
} // end namespace rstudio

#endif /* CORE_REGEX_REGEX_SEARCH_HPP */
