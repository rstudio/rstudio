/*
 * Util.hpp
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

#ifndef CORE_HTTP_UTIL_HPP
#define CORE_HTTP_UTIL_HPP

#include <string>
#include <vector>
#include <map>

#include <boost/asio/buffer.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/system/error_code.hpp>

namespace rstudio {
namespace core {
   
class Error;
class FilePath;

namespace http {
      
class Request;
class Response;

typedef std::pair<std::string,std::string> Field;
typedef std::vector<Field> Fields;
   
class FieldPredicate
{
public:
   FieldPredicate(const std::string& name) 
      : name_(name) 
   {
   }
   bool operator()(const Field& field) 
   { 
      return name_.compare(field.first) == 0;
   }
private:
   std::string name_;
};
   
struct File
{
   bool empty() const { return name.empty(); }
   std::string name;
   std::string contentType;
   std::string contents;
};

typedef std::map<std::string,File> Files;
   
namespace util {
      
Fields::const_iterator findField(const Fields& fields, const std::string& name);
std::string fieldValue(const Fields& fields, const std::string& name);
   
template <typename T>
T fieldValue(const Fields& fields, const std::string& name, const T& defaultVal)
{
   Fields::const_iterator pos = findField(fields, name);
   if (pos != fields.end())
   {
      try
      {
         return boost::lexical_cast<T>(pos->second);
      }
      catch(boost::bad_lexical_cast&)
      {
         return defaultVal;
      }
   }
   else // not found, return default
   {
      return defaultVal;
   }
}

template <typename T, typename Predicate>
bool fieldValue(const Fields& fields, 
                const std::string& name, 
                const Predicate& validator,
                T* pValue)
{
   Fields::const_iterator pos = findField(fields, name);
   if (pos != fields.end())
   {
      try
      {
         *pValue = boost::lexical_cast<T>(pos->second);
         return validator(*pValue);
      }
      catch(boost::bad_lexical_cast&)
      {
         return false;
      }
   }
   else 
   {
      return false;
   }
}
   
   

enum FieldDecodeType
{
   FieldDecodeNone,
   FieldDecodeForm,
   FieldDecodeQueryString
};
   
void parseFields(const std::string& fields, 
                 const char* fieldDelim, 
                 const char* valueDelim, 
                 Fields* pFields, 
                 FieldDecodeType fieldDecode);
   
void parseForm(const std::string& body, Fields* pFields);
   
void parseMultipartForm(const std::string& contentType,
                        const std::string& body, 
                        Fields* pFields,
                        Files* pFiles);

void buildQueryString(const Fields& fields, std::string* pQueryString);
void parseQueryString(const std::string& queryString, Fields* pFields);
   
std::string urlEncode(const std::string& in, bool queryStringSpaces = false);
std::string urlDecode(const std::string& in);
   
   
boost::posix_time::ptime parseHttpDate(const std::string& date);
   
boost::posix_time::ptime parseAtomDate(const std::string& date);
   
std::string httpDate(const boost::posix_time::ptime& datetime = 
                           boost::posix_time::second_clock::universal_time());

bool isValidDate(const std::string& httpDate);


std::string pathAfterPrefix(const Request& request,
                            const std::string& pathPrefix);

core::FilePath requestedFile(const std::string& wwwLocalPath,
                             const std::string& relativePath);

void fileRequestHandler(const std::string& wwwLocalPath,
                        const std::string& baseUri,
                        const core::http::Request& request,
                        core::http::Response* pResponse);

std::string formatMessageAsHttpChunk(const std::string& message);

// determines if the given string is a well-formed IP address
bool isIpAddress(const std::string& addr);

// determines if the given string is a network address by
// querying the DNS system
bool isNetworkAddress(const std::string& str);

// determins if the given request is request to upgrade the connection to a websocket
bool isWSUpgradeRequest(const Request& request);

// does the given error represent SSL truncation/shutdown?
bool isSslShutdownError(const boost::system::error_code& code);

std::string addQueryParam(const std::string& uri,
                          const std::string& queryParam);

} // namespace util

} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_UTIL_HPP
