/*
 * Json.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <core/json/Json.hpp>

#include <core/json/rapidjson/stringbuffer.h>
#include <core/json/rapidjson/prettywriter.h>
#include <core/json/rapidjson/writer.h>

namespace rstudio {
namespace core {
namespace json {

Object Value::get_obj() const
{
   return Object(get_impl());
}

Array Value::get_array() const
{
   return Array(get_impl());
}

rapidjson::CrtAllocator Value::s_allocator;

template <>
Object Value::get_value<Object>() const
{
   Object self = get_obj();
   Object copy(self);
   return copy;
}

template <>
Array Value::get_value<Array>() const
{
   Array self = get_array();
   Array copy(self);
   return copy;
}

template <>
int Value::get_value<int>() const
{
   return get_impl().GetInt();
}

template <>
double Value::get_value<double>() const
{
   return get_impl().GetDouble();
}

template <>
unsigned int Value::get_value<unsigned int>() const
{
   return get_impl().GetUint();
}

template <>
int64_t Value::get_value<int64_t>() const
{
   return get_impl().GetInt64();
}

template <>
uint64_t Value::get_value<uint64_t>() const
{
   return get_impl().GetUint64();
}

template <>
bool Value::get_value<bool>() const
{
   return get_impl().GetBool();
}

template <>
const char* Value::get_value<const char*>() const
{
   return get_impl().GetString();
}

template <>
std::string Value::get_value<std::string>() const
{
   return std::string(get_impl().GetString(), get_impl().GetStringLength());
}

Object toJsonObject(const std::vector<std::pair<std::string,std::string> >& options)
{
   Object optionsJson;
   typedef std::pair<std::string,std::string> Pair;
   for (const Pair& option : options)
   {
      optionsJson[option.first] = option.second;
   }
   return optionsJson;
}

std::vector<std::pair<std::string,std::string> > optionsFromJson(const Object& optionsJson)
{
   std::vector<std::pair<std::string,std::string> > options;
   for (const Member& member : optionsJson)
   {
      if (member.value().type() == StringType)
         options.push_back(std::make_pair(member.name(), member.value().get_str()));
   }
   return options;
}

bool fillSetString(const Array& array, std::set<std::string>* pSet)
{
   for (Array::iterator it = array.begin();
        it != array.end();
        ++it)
   {
      if (!isType<std::string>(*it))
         return false;
      pSet->insert((*it).get_str());
   }

   return true;
}

bool fillVectorString(const Array& array, std::vector<std::string>* pVector)
{
   for (Array::iterator it = array.begin();
        it != array.end();
        ++it)
   {
      if (!isType<std::string>(*it))
         return false;
      pVector->push_back((*it).get_str());
   }

   return true;
}

bool fillVectorInt(const Array& array, std::vector<int>* pVector)
{
   for (Array::iterator it = array.begin();
        it != array.end();
        ++it)
   {
      if (!isType<int>(*it))
         return false;
      pVector->push_back((*it).get_int());
   }

   return true;
}

bool fillMap(const Object& object, std::map< std::string, std::vector<std::string> >* pMap)
{
   for (Object::iterator it = object.begin();
        it != object.end();
        ++it)
   {
      std::vector<std::string> strings;
      const Array& array = (*it).value().get_array();
      if (!fillVectorString(array, &strings))
         return false;

      (*pMap)[(*it).name()] = strings;
   }
   return true;
}

bool parse(const std::string& input, Value* pValue)
{
   return pValue->parse(input);
}

void write(const Value& value, std::ostream& os)
{
   os << write(value);
}

void writeFormatted(const Value& value, std::ostream& os)
{
   os << writeFormatted(value);
}

std::string write(const Value& value)
{
   rapidjson::StringBuffer buffer;
   rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);

   value.get_impl().Accept(writer);
   return std::string(buffer.GetString(), buffer.GetLength());
}

std::string writeFormatted(const Value& value)
{
   rapidjson::StringBuffer buffer;
   rapidjson::PrettyWriter<rapidjson::StringBuffer> writer(buffer);

   value.get_impl().Accept(writer);
   return std::string(buffer.GetString(), buffer.GetLength());
}

} // namespace json
} // namespace core
} // namespace rstudio



