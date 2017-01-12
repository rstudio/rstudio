/*
 * CsvParser.hpp
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

#ifndef CSV_PARSER_HPP
#define CSV_PARSER_HPP

#include <string>
#include <vector>
#include <boost/algorithm/string/replace.hpp>

namespace rstudio {
namespace core {
namespace text {

/*
Parses up to one line of CSV data. Empty lines will be skipped.

The value returned is a pair consisting of the line that was
successfully parsed and an iterator value that indicates where
parsing should begin next time.

If less than one line of CSV data is available, an empty vector
is returned.

This implementation is RFC4180 compliant.

Note that if parseCsvLine is called in a loop, the termination
condition should be that the returned vector is empty, NOT that
the returned iterator == end. (In the case of malformed or
incomplete CSV data that do not end with a line break, the
returned iterator will never move past the beginning of the
last line.)
*/

template <typename InputIterator>
std::pair<std::vector<std::string>, InputIterator> parseCsvLine(
      InputIterator begin,
      InputIterator end,
      bool allowMissingEOL = false)
{
   std::vector<std::string> line;

   bool inQuote = false;

   std::string element;

   InputIterator pos = begin;
   while (pos != end)
   {
      bool noIncrement = false;

      if (inQuote)
      {
         if (*pos == '"')
         {
            if (++pos != end)
            {
               if (*pos == '"')
               {
                  element.push_back('"');
                  ++pos;
                  continue;
               }
            }
            noIncrement = true;
            inQuote = false;
         }
         else
         {
            element.push_back(*pos);
         }
      }
      else // not in quote
      {
         if (*pos == '"')
         {
            // starting a quote
            element.clear();
            inQuote = true;
         }
         else if (*pos == ',')
         {
            line.push_back(element);
            element.clear();
         }
         else if (*pos == '\r')
         {
            // ignore--expect a \n next
         }
         else if (*pos == '\n')
         {
            if (!element.empty() || !line.empty())
            {
               line.push_back(element);
               element.clear();
            }

            begin = ++pos;
            noIncrement = true;

            // don't return blank lines
            if (!line.empty())
            {
               return std::pair<std::vector<std::string>, InputIterator>(
                     line, begin);
            }
         }
         else
         {
            element.push_back(*pos);
         }
      }

      if (!noIncrement)
         ++pos;
   }
   
   // if we got here, we failed to find a (terminating) newline
   if (allowMissingEOL)
   {
      line.push_back(element);
      return std::pair<std::vector<std::string>, InputIterator>(line, end);
   }
   
   return std::pair<std::vector<std::string>, InputIterator>(
         std::vector<std::string>(), begin);
}

template <typename InputIterator>
InputIterator parseCsvLine(InputIterator begin,
                           InputIterator end,
                           bool allowMissingEOL,
                           std::vector<std::string>* pParsedCsv)
{
   std::pair<std::vector<std::string>, InputIterator> parsed =
          parseCsvLine(begin, end, allowMissingEOL);
   *pParsedCsv = parsed.first;
   return parsed.second;
}


// Encodes a vector of string values to a line of RFC4180 CSV.
//
// Note that it's the caller's responsibility to add a terminating newline.
inline std::string encodeCsvLine(const std::vector<std::string>& values) 
{
   std::string line;
   for (unsigned i = 0; i < values.size(); i++)
   {
      // escape quotes if needed
      std::string val(values[i]);
      boost::algorithm::replace_all(val, "\"", "\"\"");

      // add to the line, with a comma if there are additional values
      line.append("\"" + val + "\"");
      if (i < values.size() - 1)
         line.append(",");
   }
   return line;
}

} // namespace text
} // namespace core
} // namespace rstudio

#endif // CSV_PARSER_HPP
