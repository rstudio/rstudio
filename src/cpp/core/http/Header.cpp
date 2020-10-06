/*
 * Header.cpp
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

#include <core/http/Header.hpp>

#include <iostream>
#include <algorithm>

#include <boost/algorithm/string.hpp>

namespace rstudio {
namespace core {
namespace http {
   
bool HeaderNamePredicate::operator()(const Header& header) const
{ 
   return boost::iequals(name_, header.name);
}
   
bool containsHeader(const Headers& headers, const std::string& name)
{
   return findHeader(headers, name) != headers.end();
}

Headers::const_iterator findHeader(const Headers& headers, 
                                   const std::string& name)
{
   return std::find_if(headers.begin(), 
                       headers.end(), 
                       HeaderNamePredicate(name));
}

std::string headerValue(const Headers& headers, const std::string& name)
{
   Headers::const_iterator it = std::find_if(headers.begin(), 
                                             headers.end(), 
                                             HeaderNamePredicate(name));
   
   if ( it != headers.end() )
      return (*it).value;
   else
      return std::string();
}

   
bool parseHeader(const std::string& line, Header* pHeader)
{
   // parse the name and value out of the header
   std::string::size_type pos = line.find(": ");
   if ( pos != std::string::npos )
   {
      pHeader->name = line.substr(0, pos);
      pHeader->value = line.substr(pos + 2);
      boost::algorithm::trim(pHeader->name);
      boost::algorithm::trim(pHeader->value);
      return true;
   }
   else
   {
      return false;
   }
}
   
void parseHeaders(std::istream& is, Headers* pHeaders)
{
   bool parsing = false;
   std::string line;
   while (std::getline(is, line))
   {
      // this is either leading whitespace or a termination condition
      if (line == "\r")
      {
         if (parsing)
            break;
         else
            continue;
      }
      
      Header header;
      if (parseHeader(line, &header))
         pHeaders->push_back(header);
      parsing = true;
   }
}   
   
} // namespace http
} // namespace core
} // namespace rstudio
