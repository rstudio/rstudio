/*
 * Header.hpp
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

#ifndef CORE_HTTP_HEADER_HPP
#define CORE_HTTP_HEADER_HPP

#include <string>
#include <vector>
#include <iosfwd>

namespace rstudio {
namespace core {
namespace http {

struct Header
{
   Header() {}
   Header(const std::string& name, const std::string& value)
      : name(name), value(value)
   {
   }
   std::string name;
   std::string value;
   bool empty() const { return name.empty(); }
   
   static Header connectionClose() { return Header("Connection", "close"); }
};
   
typedef std::vector<Header> Headers;
   
class HeaderNamePredicate
{
public:
   HeaderNamePredicate(const std::string& name) 
   : name_(name) 
   {
   }
   bool operator()(const Header& header) const;
private:
   std::string name_;
};
   
bool containsHeader(const Headers& headers, const std::string& name);
   
Headers::const_iterator findHeader(const Headers& headers, 
                                   const std::string& name);
   
std::string headerValue(const Headers& headers, const std::string& name);
   
bool parseHeader(const std::string& line, Header* pHeader);
   
void parseHeaders(std::istream& is, Headers* pHeaders);
   


} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_HEADER_HPP
