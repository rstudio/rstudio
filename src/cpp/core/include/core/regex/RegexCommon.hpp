/*
 * RegexCommon.hpp
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

#ifndef CORE_REGEX_REGEX_COMMON_HPP
#define CORE_REGEX_REGEX_COMMON_HPP

#include <string>

#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/StringUtils.hpp>


namespace rstudio {
namespace core {
namespace regex_utils {

namespace detail {

inline std::string toString(const boost::regex& pattern)
{
   return pattern.str();
}

inline std::string toString(const boost::wregex& pattern)
{
   return string_utils::wideToUtf8(pattern.str());
}

inline std::string normalizeWhitespace(const std::string& string)
{
   boost::regex pattern("\n+");
   std::string replacement(" ");
   return boost::regex_replace(string, pattern, replacement);
}

template <typename PatternType>
void reportException(const std::string& method,
                     const PatternType& pattern,
                     std::exception& e)
{
   // catch and report exceptions to the user
   const char* fmt =
         "caught exception emitted by %1% "
         "[pattern='%2%' reason='%3%']";

   boost::format formatter(fmt);
   formatter
         % method
         % detail::toString(pattern)
         % detail::normalizeWhitespace(e.what());

   std::string message = formatter.str();

   // write to error logs
   LOG_WARNING_MESSAGE(message);

   // log to console as well
   std::cerr << "Warning: " << message << std::endl
             << "Please report this error to http://support.rstudio.com"
             << std::endl;
}

} // end namespace detail

} // end namespace regex_utils
} // end namespace core
} // end namespace rstudio

#endif /* CORE_REGEX_REGEX_COMMON_HPP */
