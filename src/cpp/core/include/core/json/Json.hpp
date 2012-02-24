/*
 * Json.hpp
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

#ifndef CORE_JSON_HPP
#define CORE_JSON_HPP

#include <string>
#include <vector>
#include <iosfwd>

#include <boost/type_traits/is_same.hpp>

#include <core/json/spirit/json_spirit_value.h>

namespace core {
namespace json {
   
// alias json_spirit type constants 
extern json_spirit::Value_type ObjectType;
extern json_spirit::Value_type ArrayType;   
extern json_spirit::Value_type StringType;
extern json_spirit::Value_type BooleanType;
extern json_spirit::Value_type IntegerType;
extern json_spirit::Value_type RealType;
extern json_spirit::Value_type NullType;
   
// alias json_spirit value and collection types
typedef json_spirit::Value_impl<json_spirit::mConfig> Value;
typedef json_spirit::mConfig::Array_type Array;
typedef json_spirit::mConfig::Object_type Object;
typedef Object::value_type Member;
   
template <typename T>
bool isType(const Value& value) 
{ 
   if (value.is_null())
      return false;
   else if (boost::is_same<T, Object>::value)
      return value.type() == ObjectType;
   else if (boost::is_same<T, Array>::value)
      return value.type() == ArrayType;
   else if (boost::is_same<T, std::string>::value)
      return value.type() == StringType;
   else if (boost::is_same<T, bool>::value)
      return value.type() == BooleanType;
   else if (boost::is_same<T, int>::value)
      return value.type() == IntegerType; 
   else if (boost::is_same<T, double>::value)
      return value.type() == RealType || value.type() == IntegerType;
   else
      return false;
}

template<typename T>
json::Value toJsonValue(const T& val)
{
   return json::Value(val);
}

json::Value toJsonString(const std::string& val);

template<typename T>
json::Array toJsonArray(const std::vector<T>& val)
{
   json::Array results;
   std::copy(val.begin(), val.end(), std::back_inserter(results));
   return results;
}

bool parse(const std::string& input, Value* pValue);

void write(const Value& value, std::ostream& os);
void writeFormatted(const Value& value, std::ostream& os);
   
} // namespace json
} // namespace core

#endif // CORE_JSON_HPP

