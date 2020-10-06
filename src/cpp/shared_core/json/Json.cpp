/*
 * Json.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <shared_core/json/Json.hpp>

#include <sstream>

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>
#include <boost/system/error_code.hpp>

#include <shared_core/Error.hpp>

#include "shared_core/json/rapidjson/document.h"
#include "shared_core/json/rapidjson/stringbuffer.h"
#include "shared_core/json/rapidjson/prettywriter.h"
#include "shared_core/json/rapidjson/writer.h"
#include "shared_core/json/rapidjson/error/en.h"
#include "shared_core/json/rapidjson/schema.h"

// JSON Boost Error ====================================================================================================
// Declare rapidjson errors as boost errors.
namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {

template <>
struct is_error_code_enum<rapidjson::ParseErrorCode>
{
   static const bool value = true;
};

template <>
struct is_error_code_enum<rapidjson::PointerParseErrorCode>
{
   static const bool value = true;
};

} // namespace system
} // namespace boost

namespace rstudio {
namespace core {
namespace json {
   const boost::system::error_category& jsonParseCategory();
   const boost::system::error_category& jsonPointerParseCategory();
}
}
}

namespace rapidjson {
inline boost::system::error_code make_error_code(ParseErrorCode e) {
   return { e, rstudio::core::json::jsonParseCategory() };
}

inline boost::system::error_condition make_error_condition(ParseErrorCode e) {
   return { e, rstudio::core::json::jsonParseCategory() };
}

inline boost::system::error_code make_error_code(PointerParseErrorCode e) {
   return { e, rstudio::core::json::jsonPointerParseCategory() };
}

inline boost::system::error_condition make_error_condition(PointerParseErrorCode e) {
   return { e, rstudio::core::json::jsonPointerParseCategory() };
}
}

namespace rstudio {
namespace core {
namespace json {

class JsonParseErrorCategory : public boost::system::error_category
{
public:
   const char* name() const BOOST_NOEXCEPT override;

   std::string message(int ev) const override;
};

class JsonPointerParseErrorCategory : public boost::system::error_category
{
public:
   const char* name() const BOOST_NOEXCEPT override;

   std::string message(int ev) const override;
};

const boost::system::error_category& jsonParseCategory()
{
   static JsonParseErrorCategory jsonParseErrorCategoryConst;
   return jsonParseErrorCategoryConst;
}

const boost::system::error_category& jsonPointerParseCategory()
{
   static JsonPointerParseErrorCategory jsonPointerParseErrorCategoryConst;
   return jsonPointerParseErrorCategoryConst;
}

const char* JsonParseErrorCategory::name() const BOOST_NOEXCEPT
{
   return "json-parse";
}

std::string JsonParseErrorCategory::message(int ev) const
{
   return rapidjson::GetParseError_En(static_cast<rapidjson::ParseErrorCode>(ev));
}

const char* JsonPointerParseErrorCategory::name() const BOOST_NOEXCEPT
{
   return "json-pointer-parse";
}

std::string JsonPointerParseErrorCategory::message(int /*ev*/) const
{
   // rapidjson provides no friendly mapping of pointer parse errors
   return "Pointer parse failure - see error code";
}

typedef rapidjson::GenericDocument<rapidjson::UTF8<>, rapidjson::CrtAllocator> JsonDocument;
typedef rapidjson::GenericValue<rapidjson::UTF8<>, rapidjson::CrtAllocator> JsonValue;
typedef rapidjson::GenericPointer<rapidjson::GenericValue<rapidjson::UTF8<>, rapidjson::CrtAllocator>,
                                  rapidjson::CrtAllocator> JsonPointer;

// Globals and Helpers =================================================================================================
namespace {

rapidjson::CrtAllocator s_allocator;

Object getSchemaDefaults(const Object& schema)
{
   Object result;
   Object::Iterator objType = schema.find("type");
   if (objType == schema.end() ||
       (*objType).getValue().getType() != Type::STRING ||
       (*objType).getValue().getString() != "object")
   {
      // Nothing to do for non-object types
      return result;
   }

   Object::Iterator objProperties = schema.find("properties");
   if (objProperties == schema.end() ||
       (*objProperties).getValue().getType() != Type::OBJECT)
   {
      // Nothing to do for types with no properties
      return result;
   }

   // Iterate over all the properties specified in the schema
   const json::Object& properties = (*objProperties).getValue().getObject();
   for (auto prop: properties)
   {
      // JSON schema specifies that properties are defined with objects
      if (prop.getValue().getType() != Type::OBJECT)
         continue;

      const json::Object& definition = prop.getValue().getObject();
      Object::Iterator def = definition.find("default");
      if (def == definition.end())
      {
         // We didn't find a default value for this property, so recurse and see if it is an
         // object with its own defaults.
         json::Object child = getSchemaDefaults(definition);
         if (!child.isEmpty())
         {
            // We found defaults inside the object; use them
            result[prop.getName()] = child;
         }
      }
      else
      {
         // Use the default
         result[prop.getName()] = (*def).getValue();
      }
   }
   return result;
}

} // anonymous namespace

// Value ===============================================================================================================
struct Value::Impl
{
   Impl() :
      Document(new JsonDocument(&s_allocator))
   {
   }

   explicit Impl(const std::shared_ptr<JsonDocument>& in_jsonDocument) :
      Document(in_jsonDocument)
   {
   }

   void copy(const Impl& in_other)
   {
      Document->CopyFrom(*in_other.Document, s_allocator);
   }

   std::shared_ptr<JsonDocument> Document;
};

Value::Value() :
   m_impl(new Impl())
{
}

Value::Value(ValueImplPtr in_valueImpl) :
   m_impl(std::move(in_valueImpl))
{
}

Value::Value(const Value& in_other) :
   Value()
{
   m_impl->copy(*in_other.m_impl);
}

Value::Value(Value&& in_other) noexcept :
   Value()
{
   move(std::move(in_other));
}

Value::Value(bool in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(double in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(float in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(int in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(int64_t in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(const char* in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(const std::string& in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(unsigned int in_value) :
   Value()
{
   *this = in_value;
}

Value::Value(uint64_t in_value) :
   Value()
{
   *this = in_value;
}

Value& Value::operator=(const Value& in_other)
{
   // Don't bother copying if these objects are the same object.
   if (this != &in_other)
      m_impl->copy(*in_other.m_impl);
   return *this;
}

Value& Value::operator=(Value&& in_other) noexcept
{
   // Don't bother moving if these objects are the same.
   if (this != &in_other)
      move(std::move(in_other));

   return *this;
}

Value& Value::operator=(bool in_value)
{
   m_impl->Document->SetBool(in_value);
   return *this;
}

Value& Value::operator=(double in_value)
{
   m_impl->Document->SetDouble(in_value);
   return *this;
}

Value& Value::operator=(float in_value)
{
   m_impl->Document->SetFloat(in_value);
   return *this;
}

Value& Value::operator=(int in_value)
{
   m_impl->Document->SetInt(in_value);
   return *this;
}

Value& Value::operator=(int64_t in_value)
{
   m_impl->Document->SetInt64(in_value);
   return *this;
}

Value& Value::operator=(const char* in_value)
{
   m_impl->Document->SetString(in_value, s_allocator);
   return *this;
}

Value& Value::operator=(const std::string& in_value)
{
   m_impl->Document->SetString(in_value.c_str(), s_allocator);
   return *this;
}

Value& Value::operator=(unsigned int in_value)
{
   m_impl->Document->SetUint(in_value);
   return *this;
}

Value& Value::operator=(uint64_t in_value)
{
   m_impl->Document->SetUint64(in_value);
   return *this;
}

bool Value::operator==(const Value& in_other) const
{
   // Don't bother with deep comparison if shallow comparison will do.
   if (this == &in_other)
      return true;

   if (m_impl->Document == in_other.m_impl->Document)
      return true;

   // Exactly one is null (they're not equal) - return false.
   if ((m_impl->Document == nullptr) || (in_other.m_impl->Document == nullptr))
      return false;

   return *m_impl->Document == *in_other.m_impl->Document;
}

bool Value::operator!=(const Value& in_other) const
{
   return !(*this == in_other);
}

Value Value::clone() const
{
   return Value(*this);
}

Error Value::coerce(const std::string& in_schema,
                    std::vector<std::string>& out_propViolations)
{
   Error error;

   // Parse the schema first.
   rapidjson::Document sd;
   rapidjson::ParseResult result = sd.Parse(in_schema.c_str());
   if (result.IsError())
   {
      error = Error(result.Code(), ERROR_LOCATION);
      error.addProperty("offset", result.Offset());
      return error;
   }

   // Validate the input according to the schema.
   rapidjson::SchemaDocument schemaDoc(sd);
   rapidjson::SchemaValidator validator(schemaDoc);
   rapidjson::Pointer lastInvalid;
   while (!m_impl->Document->Accept(validator))
   {
      rapidjson::StringBuffer sb;

      // Find the invalid part of the document
      rapidjson::Pointer invalid = validator.GetInvalidDocumentPointer();

      if (invalid == lastInvalid)
      {
         // If this is the same as the last invalid piece we tried to remove, then removing
         // it didn't actually fix the problem.
         error = Error(rapidjson::kParseErrorUnspecificSyntaxError, ERROR_LOCATION);
         error.addProperty("keyword", validator.GetInvalidSchemaKeyword());
         invalid.StringifyUriFragment(sb);
         error.addProperty("document", sb.GetString());
         return error;
      }

      // Remember this as the last error we hit, so we can bail if mutating the document
      // doesn't resolve it
      lastInvalid = invalid;

      // Accumulate the error for the caller
      invalid.Stringify(sb);
      out_propViolations.emplace_back(sb.GetString());

      // Remove the invalid part of the document
      JsonPointer pointer(sb.GetString(), &s_allocator);
      pointer.Erase(*(m_impl->Document));

      // Reset state for re-validation
      validator.Reset();
   }

   // The value was successfully coerced, or didn't need to be.
   return Success();
}

Array Value::getArray() const
{
   assert(getType() == Type::ARRAY);
   return Array(m_impl);
}

bool Value::getBool() const
{
   assert(isBool());
   return m_impl->Document->GetBool();
}

double Value::getDouble() const
{
   assert(isDouble() || isFloat() || (getType() == Type::INTEGER));
   return m_impl->Document->GetDouble();
}

float Value::getFloat() const
{
   assert(isFloat() || (getType() == Type::INTEGER));
   return m_impl->Document->GetFloat();
}

int Value::getInt() const
{
   assert(isInt());
   return m_impl->Document->GetInt();
}

int64_t Value::getInt64() const
{
   assert(isInt64() || isInt());
   return m_impl->Document->GetInt64();
}

Object Value::getObject() const
{
   assert(isObject());
   return Object(m_impl);
}

std::string Value::getString() const
{
   assert(isString());
   return std::string(m_impl->Document->GetString(), m_impl->Document->GetStringLength());
}

Type Value::getType() const
{
   switch (m_impl->Document->GetType())
   {
      case rapidjson::kArrayType:
         return Type::ARRAY;
      case rapidjson::kTrueType:
      case rapidjson::kFalseType:
         return Type::BOOL;
      case rapidjson::kNumberType:
      {
         if (m_impl->Document->IsDouble() || m_impl->Document->IsFloat())
            return Type::REAL;

         return Type::INTEGER;
      }
      case rapidjson::kObjectType:
         return Type::OBJECT;
      case rapidjson::kStringType:
         return Type::STRING;
      case rapidjson::kNullType:
         return Type::NULL_TYPE;
      default:
         return Type::UNKNOWN;
   }
}

unsigned int Value::getUInt() const
{
   assert(isUInt());
   return m_impl->Document->GetUint();
}

uint64_t Value::getUInt64() const
{
   assert(isUInt64() || isUInt());
   return m_impl->Document->GetUint64();
}

template<>
Array Value::getValue<Array>() const
{
   // Perform a full copy.
   const Array self = getArray();
   Array copy(self);
   return copy;
}

template<>
bool Value::getValue<bool>() const
{
   return getBool();
}

template<>
double Value::getValue<double>() const
{
   return getDouble();
}

template<>
float Value::getValue<float>() const
{
   return getFloat();
}

template<>
int Value::getValue<int>() const
{
   return getInt();
}

template<>
int64_t Value::getValue<int64_t>() const
{
   return getInt64();
}

template<>
Object Value::getValue<Object>() const
{
   // Perform a full copy.
   const Object self = getObject();
   Object copy(self);
   return copy;
}

template<>
std::string Value::getValue<std::string>() const
{
   return getString();
}

template<>
unsigned int Value::getValue<unsigned int>() const
{
   return getUInt();
}

template<>
uint64_t Value::getValue<uint64_t>() const
{
   return getUInt64();
}

bool Value::isArray() const
{
   return getType() == Type::ARRAY;
}

bool Value::isBool() const
{
   return getType() == Type::BOOL;
}

bool Value::isDouble() const
{
   return m_impl->Document->IsDouble();
}

bool Value::isFloat() const
{
   return m_impl->Document->IsFloat();
}

bool Value::isInt() const
{
   return m_impl->Document->IsInt();
}

bool Value::isInt64() const
{
   return m_impl->Document->IsInt64();
}

bool Value::isObject() const
{
   return getType() == Type::OBJECT;
}

bool Value::isString() const
{
   return getType() == Type::STRING;
}

bool Value::isNull() const
{
   return m_impl->Document->IsNull();
}

bool Value::isUInt() const
{
   return m_impl->Document->IsUint();
}

bool Value::isUInt64() const
{
   return m_impl->Document->IsUint64();
}

Error Value::parse(const char* in_jsonStr)
{
   rapidjson::ParseResult result = m_impl->Document->Parse(in_jsonStr);

   if (result.IsError())
   {
      std::string message = "An error occurred while parsing json. Offset: " + std::to_string(result.Offset());
      return Error(result.Code(), message, ERROR_LOCATION);
   }

   return Success();
}

Error Value::parse(const std::string& in_jsonStr)
{
   return parse(in_jsonStr.c_str());
}

Error Value::parseAndValidate(const std::string& in_jsonStr, const std::string& in_schema)
{
   Error error;

   error = parse(in_jsonStr);
   if (error)
      return error;

   return validate(in_schema);
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, const json::Value& in_value)
{
   JsonPointer pointer(in_pointerPath.c_str());
   if (!pointer.IsValid())
   {
      Error error(pointer.GetParseErrorCode(), ERROR_LOCATION);
      error.addProperty("offset", pointer.GetParseErrorOffset());
      return error;
   }

   pointer.Set(*m_impl->Document, *in_value.clone().m_impl->Document);
   return Success();
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, bool in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, double in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, float in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, int in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, int64_t in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, const char* in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, const std::string& in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, unsigned int in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, uint64_t in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, const Array& in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::setValueAtPointerPath(const std::string& in_pointerPath, const Object& in_value)
{
   return setValueAtPointerPath(in_pointerPath, json::Value(in_value));
}

Error Value::validate(const std::string& in_schema) const
{
   Error error;

   // Parse the schema first.
   rapidjson::Document sd;
   rapidjson::ParseResult result = sd.Parse(in_schema.c_str());
   if (result.IsError())
   {
      error = Error(result.Code(), ERROR_LOCATION);
      error.addProperty("offset", result.Offset());
      return error;
   }

   // Validate the input according to the schema.
   rapidjson::SchemaDocument schemaDoc(sd);
   rapidjson::SchemaValidator validator(schemaDoc);
   if (!m_impl->Document->Accept(validator))
   {
      rapidjson::StringBuffer sb;
      error = Error(rapidjson::kParseErrorUnspecificSyntaxError, ERROR_LOCATION);
      validator.GetInvalidSchemaPointer().StringifyUriFragment(sb);
      error.addProperty("schema", sb.GetString());
      error.addProperty("keyword", validator.GetInvalidSchemaKeyword());
      validator.GetInvalidDocumentPointer().StringifyUriFragment(sb);
      error.addProperty("document", sb.GetString());
      return error;
   }

   return Success();
}

std::string Value::write() const
{
   rapidjson::StringBuffer buffer;
   rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);

   m_impl->Document->Accept(writer);
   return std::string(buffer.GetString(), buffer.GetLength());
}

void Value::write(std::ostream& os) const
{
   os << write();
}

std::string Value::writeFormatted() const
{
   rapidjson::StringBuffer buffer;
   rapidjson::PrettyWriter<rapidjson::StringBuffer> writer(buffer);

   m_impl->Document->Accept(writer);
   return std::string(buffer.GetString(), buffer.GetLength());
}

void Value::writeFormatted(std::ostream& os) const
{
   os << writeFormatted();
}

void Value::move(Value&& in_other)
{
   // rapidjson copy is a move operation
   // only move the underlying value (and none of the document members)
   // because we do not want to move the allocators (as they are the same and rapidjson cannot
   // handle this)
   static_cast<JsonValue&>(*m_impl->Document) = static_cast<JsonValue&>(*in_other.m_impl->Document);
}

// Object Member =======================================================================================================
struct Object::Member::Impl
{
   Impl(const std::string& in_name, const std::shared_ptr<JsonDocument>& in_document) :
      Document(in_document),
      Name(in_name)
   {
   }

   std::shared_ptr<JsonDocument> Document;
   std::string Name;
};

Object::Member::Member(const std::shared_ptr<Object::Member::Impl>& in_impl) :
   m_impl(in_impl)
{
}

const std::string& Object::Member::getName() const
{
   return m_impl->Name;
}

Value Object::Member::getValue() const
{
   return Value(ValueImplPtr(new Value::Impl(m_impl->Document)));
}

// Object Iterator =====================================================================================================
Object::Iterator::Iterator(const Object* in_parent, std::ptrdiff_t in_startPos) :
   m_parent(in_parent),
   m_pos(in_startPos)
{
}

Object::Iterator& Object::Iterator::operator=(const Object::Iterator& in_other)
{
   m_parent = in_other.m_parent;
   m_pos = in_other.m_pos;
   return *this;
}

Object::Iterator& Object::Iterator::operator++()
{
   if (static_cast<rapidjson::SizeType>(m_pos) < m_parent->m_impl->Document->MemberCount())
      ++m_pos;
   return *this;
}

Object::Iterator Object::Iterator::operator++(int) // NOLINT
{
   Iterator copied(*this);
   ++(*this);
   return copied;
}

Object::Iterator& Object::Iterator::operator--()
{
   if (m_pos > 0)
      --m_pos;
   return *this;
}

Object::Iterator Object::Iterator::operator--(int) // NOLINT
{
   Iterator copied(*this);
   --(*this);
   return copied;
}

bool Object::Iterator::operator==(const Object::Iterator& in_other) const
{
   return (m_parent == in_other.m_parent) && (m_pos == in_other.m_pos);
}

bool Object::Iterator::operator!=(const Object::Iterator& in_other) const
{
   return !(*this == in_other);
}

Object::Iterator::reference Object::Iterator::operator*() const
{
   if (m_pos > m_parent->m_impl->Document->MemberCount())
      return Object::Member();

   auto itr = m_parent->m_impl->Document->MemberBegin() + m_pos;

   JsonDocument& docRef = static_cast<JsonDocument&>(itr->value);
   std::shared_ptr<JsonDocument> docPtr(m_parent->m_impl->Document, &docRef);

   return Object::Member(
      std::make_shared<Member::Impl>(
      std::string(itr->name.GetString(), itr->name.GetStringLength()),
      docPtr));
}

// Object ==============================================================================================================
Object::Object() :
   Value()
{
   m_impl->Document->SetObject();
}

Object::Object(const StringPairList& in_strPairs) :
   Object()
{
   for (const auto& pair : in_strPairs)
   {
      (*this)[pair.first] = pair.second;
   }
}

Object::Object(const Object& in_other) :
   Value(in_other)
{
}

Object::Object(Object&& in_other) noexcept :
   Value(in_other)
{
}

Error Object::getSchemaDefaults(const std::string& in_schema, Object& out_schemaDefaults)
{
   json::Value schema;
   Error error = schema.parse(in_schema);
   if (error)
      return error;

   if (!schema.isObject())
      return Error(rapidjson::kParseErrorValueInvalid, ERROR_LOCATION);

   out_schemaDefaults = ::rstudio::core::json::getSchemaDefaults(schema.getObject());
   return Success();
}

Object Object::mergeObjects(const Object& in_base, const Object& in_overlay)
{
   Object merged;

   // Begin by enumerating all the properties in the base object and replacing them with any
   // properties also present in the overlay object.
   for (Object::Member prop: in_base)
   {
      auto it = in_overlay.find(prop.getName());
      if (it == in_overlay.end())
      {
         // The property does not exist in the overlay object, so use the base copy.
         merged[prop.getName()] = prop.getValue().clone();
      }
      else
      {
         // The property exists in the overlay object.
         if (prop.getValue().isObject() && (*it).getValue().isObject())
         {
            // If the properties exist in both objects and both are object types, then we
            // recursively merge the objects (instead of just taking the overlay).
            merged[prop.getName()] = mergeObjects(prop.getValue().getObject(), (*it).getValue().getObject());
         }
         else
         {
            // Not objects, so just take the overlay value
            merged[prop.getName()] = (*it).getValue().clone();
         }
      }
   }

   // Next, we need to fill in any properties in the overlay object that are not present in the
   // base.
   for (auto prop: in_overlay)
   {
      auto it = in_base.find(prop.getName());
      if (it == in_base.end())
      {
         merged[prop.getName()] = prop.getValue().clone();
      }
   }
   return merged;
}

Object& Object::operator=(const Object& in_other)
{
   Value::operator=(in_other);
   return *this;
}

Object& Object::operator=(Object&& in_other) noexcept
{
   Value::operator=(std::move(in_other));
   return *this;
}

Value Object::operator[](const char* in_name)
{
   JsonDocument& doc = *m_impl->Document;
   if (!doc.HasMember(in_name))
   {
      doc.AddMember(JsonValue(in_name, s_allocator), JsonDocument(), s_allocator);
   }

   JsonDocument& docRef = static_cast<JsonDocument&>(doc.FindMember(in_name)->value);
   std::shared_ptr<JsonDocument> docPtr(m_impl->Document, &docRef);
   return Value(ValueImplPtr(new Impl(docPtr)));
}

Value Object::operator[](const std::string& in_name)
{
   return (*this)[in_name.c_str()];
}

Object::Iterator Object::find(const char* in_name) const
{
   auto itr = m_impl->Document->FindMember(in_name);
   if (itr == m_impl->Document->MemberEnd())
      return end();

   return Object::Iterator(this, itr - m_impl->Document->MemberBegin());
}

Object::Iterator Object::find(const std::string& in_name) const
{
   return find(in_name.c_str());
}

Object::Iterator Object::begin() const
{
   return Object::Iterator(this);
}

Object::Iterator Object::end() const
{
   return Object::Iterator(this, getSize());
}

Object::ReverseIterator Object::rbegin() const
{
   return Object::ReverseIterator(end());
}

Object::ReverseIterator Object::rend() const
{
   return Object::ReverseIterator(begin());
}

void Object::clear()
{
   m_impl->Document->SetObject();
}

bool Object::erase(const char* in_name)
{
   return m_impl->Document->EraseMember(in_name);
}

bool Object::erase(const std::string& in_name)
{
   return erase(in_name.c_str());
}

Object::Iterator Object::erase(const Object::Iterator& in_itr)
{
   auto internalItr = m_impl->Document->MemberBegin() + in_itr.m_pos;
   std::ptrdiff_t newPos = m_impl->Document->EraseMember(internalItr) - m_impl->Document->MemberBegin();
   return Object::Iterator(this, newPos);
}

size_t Object::getSize() const
{
   return m_impl->Document->MemberCount();
}

bool Object::hasMember(const char* in_name) const
{
   return m_impl->Document->HasMember(in_name);
}

bool Object::hasMember(const std::string& in_name) const
{
   return hasMember(in_name.c_str());
}

void Object::insert(const std::string& in_name, const Value& in_value)
{
   (*this)[in_name] = in_value;
}

void Object::insert(const std::string& in_name, bool in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, double in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, float in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, int in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, int64_t in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, const char* in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, const std::string& in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, unsigned int in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, uint64_t in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, const Array& in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const std::string& in_name, const Object& in_value)
{
   insert(in_name, json::Value(in_value));
}

void Object::insert(const Member& in_member)
{
   insert(in_member.getName(), in_member.getValue());
}


bool Object::isEmpty() const
{
   return m_impl->Document->ObjectEmpty();
}

Error Object::parse(const char* in_jsonStr)
{
   static const std::string kObjectSchema = "{ \"type\": \"object\" }";
   Error error = Value::parse(in_jsonStr);
   if (error)
      return error;

   return validate(kObjectSchema);
}

Error Object::parse(const std::string& in_jsonStr)
{
   return parse(in_jsonStr.c_str());
}

bool Object::toStringMap(StringListMap& out_map) const
{
   for (const Member member: *this)
   {
      std::vector<std::string> strs;
      const Array& array = member.getValue().getArray();
      if (!array.toVectorString(strs))
         return false;

      out_map[member.getName()] = strs;
   }

   return true;
}

StringPairList Object::toStringPairList() const
{
   StringPairList stringPairs;
   for (Member member : *this)
   {
      if (member.getValue().getType() == Type::STRING)
         stringPairs.emplace_back(member.getName(), member.getValue().getString());
      else
         log::logDebugMessage(
            "Skipping member " +
            member.getName() +
            " when converting object to a list of string pairs because its value does not have a string type.",
            ERROR_LOCATION);
   }

   return stringPairs;
}

Object::Object(ValueImplPtr in_value)
{
   m_impl = in_value;
   assert(m_impl->Document->IsObject());
}

// Array Iterator ======================================================================================================
Array::Iterator::Iterator(const Array* in_parent, std::ptrdiff_t in_startPos) :
   m_parent(in_parent),
   m_pos(in_startPos)
{
}

Array::Iterator& Array::Iterator::operator=(const Array::Iterator& in_other)
{
   m_parent = in_other.m_parent;
   m_pos = in_other.m_pos;
   return *this;
}

Array::Iterator& Array::Iterator::operator++()
{
   if (m_pos < m_parent->m_impl->Document->Size())
      ++m_pos;

   return *this;
}

Array::Iterator& Array::Iterator::operator--()
{
   if (m_pos > 0)
      --m_pos;

   return *this;
}

Array::Iterator Array::Iterator::operator++(int) // NOLINT
{
   Array::Iterator copied(*this);
   ++(*this);
   return copied;
}

Array::Iterator Array::Iterator::operator--(int) // NOLINT
{
   Array::Iterator copied(*this);
   --(*this);
   return copied;
}

bool Array::Iterator::operator==(const Array::Iterator& in_other) const
{
   return (m_parent == in_other.m_parent) && (m_pos == in_other.m_pos);
}

bool Array::Iterator::operator!=(const Array::Iterator& in_other) const
{
   return !(*this == in_other);
}

Array::Iterator::reference Array::Iterator::operator*() const
{
   if (m_pos >= m_parent->m_impl->Document->Size())
      return Value();

   auto internalItr = m_parent->m_impl->Document->Begin() + m_pos;

   JsonDocument& docRef = static_cast<JsonDocument&>(*internalItr);
   std::shared_ptr<JsonDocument> docPtr(m_parent->m_impl->Document, &docRef);

   return Value(ValueImplPtr(new Impl(docPtr)));
}

// Array ===============================================================================================================
Array::Array() :
   Value()
{
   m_impl->Document->SetArray();
}

Array::Array(const StringPairList& in_strPairs) :
   Array()
{
   for (const auto& pair : in_strPairs)
   {
      // escape the equals in the keys and values
      // this is necessary because we will jam the key value pairs together
      // into an array to ensure that options stay ordered, and we want to make sure
      // to properly deliniate between the real equals delimiter and an equals value
      // in the key value pairs
      std::string escapedKey = boost::replace_all_copy(pair.first, "=", "\\=");
      std::string escapedValue = boost::replace_all_copy(pair.second, "=", "\\=");

      std::string argVal = escapedKey;
      if (!escapedValue.empty())
         argVal += "=" + escapedValue;

      push_back(Value(argVal));
   }
}

Array::Array(const Array& in_other) :
   Value(in_other)
{
}

Array::Array(Array&& in_other) noexcept :
   Value(in_other)
{
}

Array& Array::operator=(const Array& in_other)
{
   Value::operator=(in_other);
   return *this;
}

Array& Array::operator=(Array&& in_other) noexcept
{
   Value::operator=(std::move(in_other));
   return *this;
}

Value Array::operator[](size_t in_index) const
{
   JsonDocument& docRef = static_cast<JsonDocument&>((*m_impl->Document)[in_index]);
   std::shared_ptr<JsonDocument> docPtr(m_impl->Document, &docRef);

   return Value(ValueImplPtr(new Impl(docPtr)));
}

Array::Iterator Array::begin() const
{
   return Array::Iterator(this);
}

Array::Iterator Array::end() const
{
   return Array::Iterator(this, m_impl->Document->Size());
}

Array::ReverseIterator Array::rbegin() const
{
   return Array::ReverseIterator(end());
}

Array::ReverseIterator Array::rend() const
{
   return Array::ReverseIterator(begin());
}

void Array::clear()
{
   m_impl->Document->Clear();
}

Array::Iterator Array::erase(const Array::Iterator& in_itr)
{
   if (getSize() == 0)
      return Array::Iterator(this);

   auto internalItr = m_impl->Document->Begin() + in_itr.m_pos;
   std::ptrdiff_t newPos = m_impl->Document->Erase(internalItr) - m_impl->Document->Begin();
   return Array::Iterator(this, newPos);
}

Array::Iterator Array::erase(const Array::Iterator& in_first, const Array::Iterator& in_last)
{
   if (getSize() == 0)
      return Array::Iterator(this);

   auto internalFirst = m_impl->Document->Begin() + in_first.m_pos;
   auto internalLast = m_impl->Document->Begin() + in_last.m_pos;

   std::ptrdiff_t newPos = m_impl->Document->Erase(internalFirst, internalLast) - m_impl->Document->Begin();
   return Array::Iterator(this, newPos);
}

Value Array::getBack() const
{
   return (*this)[getSize() - 1];
}

Value Array::getFront() const
{
   return (*this)[0];
}

Value Array::getValueAt(size_t in_index) const
{
   return (*this)[in_index];
}

size_t Array::getSize() const
{
   return m_impl->Document->Size();
}

bool Array::isEmpty() const
{
   return m_impl->Document->Empty();
}

Error Array::parse(const char* in_jsonStr)
{
   static const std::string kObjectSchema = "{ \"type\": \"array\" }";
   Error error = Value::parse(in_jsonStr);
   if (error)
      return error;

   return validate(kObjectSchema);
}

Error Array::parse(const std::string& in_jsonStr)
{
   return parse(in_jsonStr.c_str());
}

void Array::push_back(const Value& in_value)
{
   JsonDocument doc;
   doc.CopyFrom(*in_value.m_impl->Document, s_allocator);
   m_impl->Document->PushBack(doc, s_allocator);
}

void Array::push_back(bool in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(double in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(float in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(int in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(int64_t in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(const char* in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(const std::string& in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(unsigned int in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(uint64_t in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(const json::Array& in_value)
{
   push_back(json::Value(in_value));
}

void Array::push_back(const json::Object& in_value)
{
   push_back(json::Value(in_value));
}

bool Array::toSetString(std::set<std::string>& out_set) const
{
   for (const Value value: *this)
   {
      if (!isType<std::string>(value))
         return false;
      out_set.insert(value.getString());
   }

   return true;
}

StringPairList Array::toStringPairList() const
{
   StringPairList strPairs;
   auto iter = begin();
   const auto endIter = end();
   for (; iter != endIter; ++iter)
   {
      if (!(*iter).isString())
      {
         log::logDebugMessage(
            "Skipping value " +
            (*iter).write() +
            " when converting array to a list of string pairs because its value does not have a string type.",
            ERROR_LOCATION);
         continue;
      }

      const std::string& pairStr = (*iter).getString();

      // find the first equals that is not preceded by an escape character
      // this is the actual position in the string we will split on to get
      // the key and value separated
      boost::smatch results;
      boost::regex rx("[^\\\\]=");
      if (boost::regex_search(pairStr, results, rx))
      {
         std::string key = pairStr.substr(0, results.position() + 1);
         std::string valueStr = pairStr.substr(results.position() + 2);
         boost::replace_all(key, "\\=", "=");
         boost::replace_all(valueStr, "\\=", "=");
         strPairs.emplace_back(key, valueStr);
      }
      else
      {
         // no value, just a key
         std::string unescapedKey = boost::replace_all_copy(pairStr, "\\=", "=");
         strPairs.emplace_back(unescapedKey, std::string());
      }
   }

   return strPairs;
}

bool Array::toVectorInt(std::vector<int>& out_vector) const
{
   for (const Value value: *this)
   {
      if (!isType<int>(value))
         return false;
      out_vector.push_back(value.getInt());
   }

   return true;
}

bool Array::toVectorString(std::vector<std::string>& out_vector) const
{
   for (const Value value: *this)
   {
      if (!isType<std::string>(value))
         return false;
      out_vector.push_back(value.getString());
   }

   return true;
}

Array::Array(ValueImplPtr in_value)
{
   m_impl = in_value;
   assert(m_impl->Document->IsArray());
}

// Free functions ======================================================================================================
std::string typeAsString(Type in_type)
{
   std::ostringstream os;
   os << in_type;
   return os.str();
}

std::ostream& operator<<(std::ostream& io_ostream, Type in_type)
{
   switch (in_type)
   {
      case Type::ARRAY:
      {
         io_ostream << "<Array>";
         break;
      }
      case Type::BOOL:
      {
         io_ostream << "<Boolean>";
         break;
      }
      case Type::INTEGER:
      {
         io_ostream << "<Integer>";
         break;
      }
      case Type::OBJECT:
      {
         io_ostream << "<Object>";
         break;
      }
      case Type::STRING:
      {
         io_ostream << "<String>";
         break;
      }
      case Type::REAL:
      {
         io_ostream << "<Real>";
         break;
      }
      case Type::NULL_TYPE:
      {
         io_ostream << "<Null>";
         break;
      }
      case Type::UNKNOWN:
      default:
      {
         io_ostream << "<Unknown>";
         break;
      }
   }

   return io_ostream;
}



template<>
Array toJsonArray<Value>(const std::vector<Value>& vector)
{
   Array results;
   for (const Value& val : vector)
   {
      results.push_back(val);
   }
   return results;
}

template<>
Array toJsonArray<Object>(const std::vector<Object>& vector)
{
   Array results;
   for (const Object& val : vector)
   {
      results.push_back(val);
   }
   return results;
}

template<>
Array toJsonArray<Array>(const std::vector<Array>& vector)
{
   Array results;
   for (const Array& val : vector)
   {
      results.push_back(val);
   }
   return results;
}

template<>
Array toJsonArray<Value>(const std::set<Value>& set)
{
   Array results;
   for (const Value& val : set)
   {
      results.push_back(val);
   }
   return results;
}

template<>
Array toJsonArray<Object>(const std::set<Object>& set)
{
   Array results;
   for (const Object& val : set)
   {
      results.push_back(val);
   }
   return results;
}

template<>
Array toJsonArray<Array>(const std::set<Array>& set)
{
   Array results;
   for (const Array& val : set)
   {
      results.push_back(val);
   }
   return results;
}

Error jsonReadError(JsonReadError in_errorCode, const std::string& in_message, const ErrorLocation& in_errorLocation)
{
   if (in_errorCode == JsonReadError::SUCCESS)
      return Success();

   return Error("JsonReadError", static_cast<int>(in_errorCode), in_message, in_errorLocation);
}

bool isMissingMemberError(const Error& in_error)
{
   return ((in_error.getName() == "JsonReadError") &&
           (static_cast<JsonReadError>(in_error.getCode()) == JsonReadError::MISSING_MEMBER));
}

} // namespace json
} // namespace core
} // namespace rstudio
