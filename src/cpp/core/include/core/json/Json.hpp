/*
 * Json.hpp
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

#ifndef CORE_JSON_HPP
#define CORE_JSON_HPP

#include <string>
#include <vector>
#include <iosfwd>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <boost/type_traits/is_same.hpp>
#include <boost/optional.hpp>

#include <core/json/spirit/json_spirit_value.h>

namespace rstudio {
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

inline std::string typeAsString(json_spirit::Value_type type)
{
   if (type == ObjectType)
      return "<Object>";
   else if (type == ArrayType)
      return "<Array>";
   else if (type == StringType)
      return "<String>";
   else if (type == BooleanType)
      return "<Boolean>";
   else if (type == IntegerType)
      return "<Integer>";
   else if (type == RealType)
      return "<Real>";
   else
      return "<unknown>";
}

namespace detail {

template <typename T>
json_spirit::Value_type asJsonType(const T& object,
                                   boost::true_type)
{
   return object.type();
}

template <typename T>
json_spirit::Value_type asJsonType(const T& object,
                                   boost::false_type)
{
   if (boost::is_same<T, bool>::value)
      return BooleanType;
   else if (boost::is_same<T, int>::value)
      return IntegerType;
   else if (boost::is_same<T, double>::value)
      return RealType;
   else if (boost::is_same<T, std::string>::value)
      return StringType;
   
   LOG_ERROR_MESSAGE("Unexpected type");
   return NullType;
}

template <typename T>
struct is_json_type : public boost::is_same<T, json_spirit::Value_type>
{
};

} // namespace detail

template <typename T>
json_spirit::Value_type asJsonType(const T& object)
{
   return detail::asJsonType(
            object,
            detail::is_json_type<T>());
}

inline std::string typeAsString(const Value& value)
{
   if (value.is_null())
      return "<null>";
   return typeAsString(value.type());
}

inline void logIncompatibleTypes(const Value& value,
                                 const json_spirit::Value_type expectedType,
                                 const ErrorLocation& location)
{
   if (value.type() != expectedType)
   {
      log::logErrorMessage("Invalid JSON type: expected '" +
                           typeAsString(expectedType) + "', got '" +
                           typeAsString(value.type()) + "'",
                           location);
   }
}

namespace detail {

template <typename T>
inline json::Value toJsonValue(const T& val)
{
   return json::Value(val);
}

template <typename T>
inline json::Value toJsonValue(const boost::optional<T>& val)
{
   return val ? json::Value(*val) : json::Value();
}

} // namespace detail

template <typename T>
inline json::Value toJsonValue(const T& val)
{
   return detail::toJsonValue(val);
}

inline json::Value toJsonValue(const boost::optional<std::string>& val)
{
   return val ? json::Value(*val) : json::Value();
}

json::Value toJsonString(const std::string& val);

// NOTE: we can't use the templatized version for bool because
// some compilers specialize std::vector<bool> such that the
// concept check for std::copy fails
inline json::Array toJsonArray(const std::vector<bool>& val)
{
   json::Array results;
   for (size_t i=0; i<val.size(); i++)
      results.push_back(val[i] ? true : false);
   return results;
}

template<typename T>
json::Array toJsonArray(const std::vector<T>& val)
{
   json::Array results;
   std::copy(val.begin(), val.end(), std::back_inserter(results));
   return results;
}

bool fillVectorString(const Array& array, std::vector<std::string>* pVector);
bool fillVectorInt(const Array& array, std::vector<int>* pVector);
bool fillMap(const Object& array, std::map< std::string, std::vector<std::string> >* pMap);

bool parse(const std::string& input, Value* pValue);

void write(const Value& value, std::ostream& os);
void writeFormatted(const Value& value, std::ostream& os);
   
} // namespace json
} // namespace core
} // namespace rstudio

#endif // CORE_JSON_HPP

