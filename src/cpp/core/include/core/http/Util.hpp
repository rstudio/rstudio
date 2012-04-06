/*
 * Util.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <boost/lexical_cast.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace core {
   
class Error;

namespace http {
      
class Request;

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
   std::string name_ ;
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
                 FieldDecodeType fieldDecode) ;
   
void parseForm(const std::string& body, Fields* pFields);
   
void parseMultipartForm(const std::string& contentType,
                        const std::string& body, 
                        Fields* pFields,
                        Files* pFiles);

void buildQueryString(const Fields& fields, std::string* pQueryString);
void parseQueryString(const std::string& queryString, Fields* pFields);
   
std::string urlEncode(const std::string& in, bool queryStringSpaces = false);
std::string urlDecode(const std::string& in, bool fromQueryString = false);
   
   
boost::posix_time::ptime parseHttpDate(const std::string& date);
   
boost::posix_time::ptime parseAtomDate(const std::string& date);
   
std::string httpDate(const boost::posix_time::ptime& datetime = 
                           boost::posix_time::second_clock::universal_time());


std::string pathAfterPrefix(const Request& request,
                            const std::string& pathPrefix);

} // namespace util

} // namespace http
} // namespace core 

#endif // CORE_HTTP_UTIL_HPP
