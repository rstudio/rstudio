/*
 * RegexUtils.cpp
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

#include <core/RegexUtils.hpp>

#include <vector>
#include <sstream>

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filtering_stream.hpp>

#include <core/StringUtils.hpp>

namespace rstudio {
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

boost::regex regexIfWildcardPattern(const std::string& term)
{
   // create wildcard pattern if the search has a '*'
   bool hasWildcard = term.find('*') != std::string::npos;
   boost::regex pattern;
   if (hasWildcard)
      pattern = regex_utils::wildcardPatternToRegex(term);
   return pattern;
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

Error filterString(const std::string& input,
                   const std::vector<boost::iostreams::regex_filter>& filters,
                   std::string* pOutput)
{
   try
   {
      // create input stream
      std::istringstream inputStream(input);
      inputStream.exceptions(std::istream::failbit | std::istream::badbit);

      // create filtered output stream
      std::ostringstream outputStream;
      outputStream.exceptions(std::istream::failbit | std::istream::badbit);
      boost::iostreams::filtering_ostream filteredStream;
      for (std::size_t i=0; i<filters.size(); i++)
         filteredStream.push(filters[i]);
      filteredStream.push(outputStream);

      boost::iostreams::copy(inputStream, filteredStream, 128);

      *pOutput = outputStream.str();

      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("input", input.substr(0, 50));
      return error;
   }

   // keep compiler happy
   return Success();
}

Error filterString(const std::string& input,
                   const boost::iostreams::regex_filter& filter,
                   std::string* pOutput)
{
   std::vector<boost::iostreams::regex_filter> filters;
   filters.push_back(filter);
   return filterString(input, filters, pOutput);
}


} // namespace regex_utils
} // namespace core 
} // namespace rstudio



