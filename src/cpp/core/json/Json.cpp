/*
 * Json.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>
#include <core/json/rapidjson/error/en.h>
#include <core/json/rapidjson/schema.h>

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

Error Value::parse(const std::string& input, const ErrorLocation& location)
{
   rapidjson::ParseResult result = get_impl().Parse(input.c_str());
   if (result.IsError())
   {
      Error error(result.Code(), location);
      error.addProperty("offset", result.Offset());
      return error;
   }
   return Success();
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

json::Array toJsonArray(const std::vector<std::pair<std::string,std::string> >& options)
{
   json::Array optionsArray;
   typedef std::pair<std::string,std::string> Pair;

   for (const Pair& option : options)
   {
      // escape the equals in the keys and values
      // this is necessary because we will jam the key value pairs together
      // into an array to ensure that options stay ordered, and we want to make sure
      // to properly deliniate between the real equals delimiter and an equals value
      // in the key value pairs
      std::string escapedKey = boost::replace_all_copy(option.first, "=", "\\=");
      std::string escapedValue = boost::replace_all_copy(option.second, "=", "\\=");

      std::string argVal = escapedKey;
      if (!escapedValue.empty())
         argVal += "=" + escapedValue;

      optionsArray.push_back(argVal);
   }

   return optionsArray;
}

std::vector<std::pair<std::string,std::string> > optionsFromJson(const json::Object& optionsJson)
{
   std::vector<std::pair<std::string,std::string> > options;
   for (const Member& member : optionsJson)
   {
      if (member.value().type() == StringType)
         options.push_back(std::make_pair(member.name(), member.value().get_str()));
   }
   return options;
}

std::vector<std::pair<std::string,std::string> > optionsFromJson(const json::Array& optionsJson)
{
   std::vector<std::pair<std::string,std::string> > options;
   for (const json::Value& value : optionsJson)
   {
      if (value.type() != json::StringType)
         continue;

      const std::string& optionStr = value.get_str();

      // find the first equals that is not preceded by an escape character
      // this is the actual position in the string we will split on to get
      // the key and value separated
      boost::smatch results;
      boost::regex rx("[^\\\\]=");
      if (boost::regex_search(optionStr, results, rx))
      {
         std::string key = optionStr.substr(0, results.position() + 1);
         std::string value = optionStr.substr(results.position() + 2);
         boost::replace_all(key, "\\=", "=");
         boost::replace_all(value, "\\=", "=");
         options.push_back(std::make_pair(key, value));
      }
      else
      {
         // no value, just a key
         std::string unescapedKey = boost::replace_all_copy(optionStr, "\\=", "=");
         options.push_back(std::make_pair(unescapedKey, std::string()));
      }
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

Error parse(const std::string& input, const ErrorLocation& location, Value* pValue)
{
   return pValue->parse(input, location);
}

json::Object getSchemaDefaults(const Object& schema)
{
   json::Object result;
   Object::iterator objType = schema.find("type");
   if (objType == schema.end() ||
       (*objType).value().type() != json::StringType ||
       (*objType).value().get_str() != "object")
   {
      // Nothing to do for non-object types
      return result;
   }

   Object::iterator objProperties = schema.find("properties");
   if (objProperties == schema.end() ||
       (*objProperties).value().type() != json::ObjectType)
   {
      // Nothing to do for types with no properties
      return result;
   }

   // Iterate over all the properties specified in the schema
   const json::Object& properties = (*objProperties).value().get_obj();
   for (auto prop: properties)
   {
      // JSON schema specifies that properties are defined with objects
      if (prop.value().type() != json::ObjectType)
         continue;

      const json::Object& definition = prop.value().get_obj();
      Object::iterator def = definition.find("default");
      if (def == definition.end())
      {
         // We didn't find a default value for this property, so recurse and see if it is an
         // object with its own defaults.
         json::Object child = getSchemaDefaults(definition);
         if (!child.empty())
         {
            // We found defaults inside the object; use them
            result[prop.name()] = child;
         }
      }
      else
      {
         // Use the default
         result[prop.name()] = (*def).value().clone();
      }
   }
   return result;
}

Object merge(const Object& base, const Object& overlay)
{
   Object merged;

   // Begin by enumerating all the properties in the base object and replacing them with any
   // properties also present in the overlay object.
   for (auto prop: base)
   {
      auto it = overlay.find(prop.name());
      if (it == overlay.end())
      {
         // The property does not exist in the overlay object, so use the base copy.
         merged[prop.name()] = prop.value().clone();
      }
      else
      {
         // The property exists in the overlay object.
         if (prop.value().type() == json::ObjectType &&
             (*it).value().type() == json::ObjectType)
         {
            // If the properties exist in both objects and both are object types, then we
            // recursively merge the objects (instead of just taking the overlay).
            merged[prop.name()] = merge(prop.value().get_obj(), (*it).value().get_obj());
         }
         else
         {
            // Not objects, so just take the overlay value
            merged[prop.name()] = (*it).value().clone();
         }
      }
   }

   // Next, we need to fill in any properties in the overlay object that are not present in the
   // base.
   for (auto prop: overlay)
   {
      auto it = base.find(prop.name());
      if (it == base.end())
      {
         merged[prop.name()] = prop.value().clone();
      }
   }
   return merged; 
}

Error getSchemaDefaults(const std::string& schema, Value* pValue)
{
   json::Value value;
   Error error = parse(schema, ERROR_LOCATION, &value);
   if (error)
   {
      return error;
   }

   if (value.type() != json::ObjectType)
   {
      return Error(rapidjson::kParseErrorValueInvalid, ERROR_LOCATION);
   }

   *pValue = getSchemaDefaults(value.get_obj());
   return Success();
}

Error parseAndValidate(const std::string& input, const std::string& schema,
      const ErrorLocation& location, Value* pValue)
{
   Error error;

   // Parse the schema first.
   rapidjson::Document sd;
   rapidjson::ParseResult result = sd.Parse(schema);
   if (result.IsError())
   {
      error = Error(result.Code(), location);
      error.addProperty("offset", result.Offset());
      return error;
   }

   // Next, parse the input.
   error = pValue->parse(input, location);
   if (error)
      return error;

   // Validate the input according to the schema.
   rapidjson::SchemaDocument schemaDoc(sd) ;
   rapidjson::SchemaValidator validator(schemaDoc);
   if (!pValue->get_impl().Accept(validator))
   {
      rapidjson::StringBuffer sb;
      error = Error(rapidjson::kParseErrorUnspecificSyntaxError, location);
      validator.GetInvalidSchemaPointer().StringifyUriFragment(sb);
      error.addProperty("schema", sb.GetString());
      error.addProperty("keyword", validator.GetInvalidSchemaKeyword());
      validator.GetInvalidDocumentPointer().StringifyUriFragment(sb);
      error.addProperty("document", sb.GetString());
      return error;
   }

   return Success();
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

class JsonParseErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const BOOST_NOEXCEPT;
   virtual std::string message(int ev) const;
};

const boost::system::error_category& jsonParseCategory()
{
   static JsonParseErrorCategory jsonParseErrorCategoryConst;
   return jsonParseErrorCategoryConst;
}

const char * JsonParseErrorCategory::name() const BOOST_NOEXCEPT
{
   return "json-parse";
}

std::string JsonParseErrorCategory::message(int ev) const
{
   return rapidjson::GetParseError_En(static_cast<rapidjson::ParseErrorCode>(ev));
}

} // namespace json
} // namespace core
} // namespace rstudio



