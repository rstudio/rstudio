/*
 * RegexUtils.hpp
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

#ifndef CORE_REGEX_UTILS_HPP
#define CORE_REGEX_UTILS_HPP

#include <string>

#include <boost/regex_fwd.hpp>

namespace core {

class Error;
class FilePath;

namespace regex_utils {
   
// convert a pattern which includes wildcard (i.e. '*') characters
// into a regulard expression
boost::regex wildcardPatternToRegex(const std::string& pattern);


bool textMatches(const std::string& text,
                 const boost::regex& regex,
                 bool prefixOnly,
                 bool caseSensitive);

} // namespace regex_utils
} // namespace core 


#endif // CORE_REGEX_UTILS_HPP

