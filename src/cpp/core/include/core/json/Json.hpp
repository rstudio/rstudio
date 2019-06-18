/*
 * Json.hpp
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

#ifndef CORE_JSON_HPP
#define CORE_JSON_HPP

#define RAPIDJSON_HAS_STDSTRING 1

#include <map>
#include <set>
#include <string>
#include <vector>
#include <iosfwd>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/type_traits/TypeTraits.hpp>

#include <boost/optional.hpp>
#include <boost/thread.hpp>
#include <boost/type_traits/is_same.hpp>
#include <boost/weak_ptr.hpp>

#include <core/json/rapidjson/document.h>
#include <core/json/rapidjson/rapidjson.h>

namespace RSTUDIO_BOOST_NAMESPACE {
   namespace system {
      template <>
      struct is_error_code_enum<rapidjson::ParseErrorCode>
      {
         static const bool value = true;
      };

   } // namespace system
} // namespace boost

namespace rstudio {
namespace core {
namespace json {

enum Type
{
   ObjectType,
   ArrayType,
   StringType,
   BooleanType,
   IntegerType,
   RealType,
   NullType,
   UnknownType
};

typedef rapidjson::GenericDocument<rapidjson::UTF8<>, rapidjson::CrtAllocator> DocumentType;
typedef rapidjson::GenericValue<rapidjson::UTF8<>, rapidjson::CrtAllocator> ValueType;

class Object;
class Array;

class Value
{
public:
   Value() :
      pValue_(new DocumentType(&s_allocator)),
      needDelete_(true)
   {
   }

   virtual ~Value()
   {
      freeValue();
   }

   Value(const Value& other) :
      Value()
   {
      copy(other);
   }

   Value(Value&& other) :
      Value()
   {
      move(std::move(other));
   }

   Value(DocumentType& value) :
      pValue_(&value),
      needDelete_(false)
   {
   }

   Value& operator=(const Value& other)
   {
      // self assignment check
      if (static_cast<void*>(this) == static_cast<void const*>(&other))
          return *this;

      copy(other);
      return *this;
   }

   Value& operator=(Value&& other)
   {
      // self assignment check
      if (static_cast<void*>(this) == static_cast<void const*>(&other))
          return *this;

      move(std::move(other));
      return *this;
   }

   Value(const char* value) :
      Value()
   {
      // copy string into document-allocated buffer
      get_impl().SetString(value, s_allocator);
   }

   Value& operator=(const char* value)
   {
      get_impl().SetString(value, s_allocator);
      return *this;
   }

   Value(const std::string& value) :
      Value()
   {
      get_impl().SetString(value, s_allocator);
   }

   Value& operator=(const std::string& value)
   {
      get_impl().SetString(value, s_allocator);
      return *this;
   }

   Value(bool value) :
      Value()
   {
      get_impl().SetBool(value);
   }

   Value& operator=(bool value)
   {
      get_impl().SetBool(value);
      return *this;
   }

   Value(int value) :
      Value()
   {
      get_impl().SetInt(value);
   }

   Value& operator=(int value)
   {
      get_impl().SetInt(value);
      return *this;
   }

   Value(int64_t value) :
      Value()
   {
      get_impl().SetInt64(value);
   }

   Value& operator=(int64_t value)
   {
      get_impl().SetInt64(value);
      return *this;
   }

   Value(uint64_t value) :
      Value()
   {
      get_impl().SetUint64(value);
   }

   Value& operator=(uint64_t value)
   {
      get_impl().SetUint64(value);
      return *this;
   }

   Value(double value) :
      Value()
   {
      get_impl().SetDouble(value);
   }

   Value& operator=(double value)
   {
      get_impl().SetDouble(value);
      return *this;
   }

   bool operator==(const Value& other) const
   {
      return get_impl() == other.get_impl();
   }

   int type() const
   {
      switch (get_impl().GetType())
      {
         case rapidjson::kNullType:
            return NullType;
         case rapidjson::kStringType:
            return StringType;
         case rapidjson::kTrueType:
         case rapidjson::kFalseType:
            return BooleanType;
         case rapidjson::kArrayType:
            return ArrayType;
         case rapidjson::kObjectType:
            return ObjectType;
         case rapidjson::kNumberType:
            return get_impl().IsDouble() ? RealType : IntegerType;
         default:
            return UnknownType;
      }
   }

   bool is_uint64() const
   {
      return get_impl().IsUint64();
   }

   bool is_null() const
   {
      return get_impl().IsNull();
   }

   std::string get_str() const
   {
      return std::string(get_impl().GetString(), get_impl().GetStringLength());
   }

   Object get_obj() const;
   Array get_array() const;

   bool get_bool() const
   {
      return get_impl().GetBool();
   }

   int get_int() const
   {
      if (get_impl().IsInt())
         return get_impl().GetInt();
      else if (get_impl().IsInt64())
         return static_cast<int>(get_impl().GetInt64());
      else if (get_impl().IsUint64())
         return static_cast<int>(get_impl().GetUint64());
      else
         return static_cast<int>(get_impl().GetUint());
   }

   int64_t get_int64() const
   {
      return get_impl().GetInt64();
   }

   uint64_t get_uint64() const
   {
      return get_impl().GetUint64();
   }

   double get_real() const
   {
      return get_impl().GetDouble();
   }

   Value clone()
   {
      return Value(*this);
   }

   template< typename T > T get_value() const;

   // static allocator that will be shared by all rapidjson classes
   // this allocator is thread-safe and allows us to eliminate the need
   // for passing around a special allocator between related json objects ("documents")
   static rapidjson::CrtAllocator s_allocator;

   bool parse(const std::string& input)
   {
      return !get_impl().Parse(input.c_str()).HasParseError();
   }

   Error parse(const std::string& input, const ErrorLocation& location);

   DocumentType& get_impl() const { return *pValue_; }

protected:
   rapidjson::GenericDocument<rapidjson::UTF8<>, rapidjson::CrtAllocator>* pValue_;

private:
   void copy(const Value& other)
   {
      // full copy of the other generic value object
      get_impl().CopyFrom(other.get_impl(), s_allocator);
   }

   void move(Value&& other)
   {
      // rapidjson copy is a move operation
      // only move the underlying value (and none of the document members)
      // because we do not want to move the allocators (as they are the same and rapidjson cannot
      // handle this)
      static_cast<ValueType&>(get_impl()) = static_cast<ValueType&>(other.get_impl());
   }

   void freeValue()
   {
      if (needDelete_)
      {
         delete pValue_;
         needDelete_ = false;
      }
   }

   bool needDelete_;
};

class Object : public Value
{
public:
   Object() :
      Value()
   {
      get_impl().SetObject();
   }

   Object(const Object& other) :
      Value(other)
   {
   }

   Object(DocumentType& doc) :
      Value(doc)
   {
   }

   Object(Object&& other) :
      Value(std::move(other))
   {
   }

   Object& operator=(const Object& other)
   {
      Value::operator=(other);
      return *this;
   }

   Object& operator=(Object&& other)
   {
      Value::operator=(std::move(other));
      return *this;
   }

   Value operator[](const std::string& name)
   {
      DocumentType& impl = get_impl();

      auto memberIter = impl.FindMember(name);
      if (memberIter == impl.MemberEnd())
      {
         // member does not yet exist - create it
         ValueType nameValue(name.c_str(), s_allocator);
         impl.AddMember(nameValue, DocumentType(), s_allocator);

         memberIter = impl.FindMember(name);
      }

      return Value(static_cast<DocumentType&>(memberIter->value));
   }

   class Member
   {
   public:
      friend class Object;

      Member(const std::string& name,
             const Value& value) :
         name_(name),
         pDoc_(nullptr),
         value_(value)
      {
      }

      const std::string& name() const { return name_; }

      Value value() const
      {
         if (pDoc_ != nullptr)
            return Value(*pDoc_);
         else
            return value_;
      }

   private:
      Member() :
         pDoc_(nullptr)
      {
      }

      Member(const std::string& name,
             DocumentType* pDoc) :
         name_(name),
         pDoc_(pDoc)
      {
      }

      std::string name_;
      DocumentType* pDoc_;
      Value value_;
   };

   typedef Member value_type;

   friend class iterator;
   class iterator: public std::iterator<std::bidirectional_iterator_tag,            // iterator_category
                                        Member,                                     // value_type
                                        std::ptrdiff_t,                             // difference_type
                                        const Member*,                              // pointer
                                        Member>                                     // reference
   {
   public:
      friend class Object;

      explicit iterator(const Object* parent, std::ptrdiff_t num = 0) :
         parent_(parent), num_(num) {}

      iterator(const iterator& other) :
         parent_(other.parent_), num_(other.num_) {}

      iterator& operator=(const iterator& other)
      {
         parent_ = other.parent_;
         num_ = other.num_;
         return *this;
      }

      iterator& operator++()
      {
         if (static_cast<rapidjson::SizeType>(num_) < parent_->get_impl().MemberCount())
            ++num_;

         return *this;
      }

      iterator& operator--()
      {
         if (num_ > 0)
            --num_;

         return *this;
      }

      iterator operator++(int)
      {
         iterator retval = *this;
         ++(*this);
         return retval;
      }

      iterator operator--(int)
      {
         iterator retval = *this;
         --(*this);
         return retval;
      }

      bool operator==(iterator other) const
      {
         return num_ == other.num_;
      }

      bool operator!=(iterator other) const
      {
         return !(*this == other);
      }

      reference operator*() const
      {
         if (static_cast<rapidjson::SizeType>(num_) >= parent_->get_impl().MemberCount())
            return Member();

         auto iter = parent_->get_impl().MemberBegin() + num_;
         return Member(iter->name.GetString(), &(static_cast<DocumentType&>(iter->value)));
      }

   private:
      const Object* parent_;
      std::ptrdiff_t num_;
   };

   typedef std::reverse_iterator<iterator> reverse_iterator;

   iterator find(const std::string& name) const
   {
      auto iter = get_impl().FindMember(name);
      if (iter == get_impl().MemberEnd())
         return end();

      return iterator(this, iter - get_impl().MemberBegin());
   }

   iterator begin() const { return iterator(this, 0); }
   iterator end() const { return iterator(this, static_cast<std::ptrdiff_t>(size())); }
   reverse_iterator rbegin() const { return reverse_iterator(end()); }
   reverse_iterator rend() const { return reverse_iterator(begin()); }

   bool erase(const std::string& name)
   {
      return get_impl().EraseMember(name);
   }

   iterator erase(const iterator& iter)
   {
      rapidjson::GenericMemberIterator<false, rapidjson::UTF8<>, rapidjson::CrtAllocator> citer =
            get_impl().MemberBegin() + iter.num_;
      return iterator(this, get_impl().EraseMember(citer) - get_impl().MemberBegin());
   }

   void clear()
   {
      get_impl().SetObject();
   }

   size_t size() const
   {
      return get_impl().MemberCount();
   }

   void insert(const Member& member)
   {
      // clone the member's value to ensure we don't
      // inadvertently move a value pointer
      (*this)[member.name()] = member.value().clone();
   }

   bool empty() const
   {
      return get_impl().ObjectEmpty();
   }

   bool contains(const std::string& name) const
   {
      return get_impl().HasMember(name);
   }
};

class Array : public Value
{
public:
   typedef Value value_type;

   Array() :
      Value()
   {
      get_impl().SetArray();
   }

   Array(DocumentType& doc) :
      Value(doc)
   {
   }

   Array(const Array& other) :
      Value(other)
   {
   }

   Array(Array&& other) :
      Value(std::move(other))
   {
   }

   Array& operator=(const Array& other)
   {
      Value::operator=(other);
      return *this;
   }

   Array& operator=(Array&& other)
   {
      Value::operator=(std::move(other));
      return *this;
   }

   Value operator[](size_t index) const
   {
      ValueType& value = get_impl()[static_cast<rapidjson::SizeType>(index)];
      return Value(static_cast<DocumentType&>(value));
   }

   class iterator: public std::iterator<std::bidirectional_iterator_tag,   // iterator_category
                                        Value,                             // value_type
                                        std::ptrdiff_t,                    // difference_type
                                        const Value*,                      // pointer
                                        Value>                             // reference
   {
   public:
      friend class Array;

      explicit iterator(const Array* parent, std::ptrdiff_t num = 0) :
         parent_(parent), num_(num) {}

      iterator& operator++()
      {
         if (static_cast<rapidjson::SizeType>(num_) < parent_->get_impl().Size())
            ++num_;

         return *this;
      }

      iterator& operator--()
      {
         if (num_ > 0)
            --num_;

         return *this;
      }

      iterator operator++(int)
      {
         iterator retval = *this;
         ++(*this);
         return retval;
      }

      iterator operator--(int)
      {
         iterator retval = *this;
         --(*this);
         return retval;
      }

      bool operator==(iterator other) const
      {
         return num_ == other.num_;
      }

      bool operator!=(iterator other) const
      {
         return !(*this == other);
      }

      reference operator*() const
      {
         if (static_cast<rapidjson::SizeType>(num_) >= parent_->get_impl().Size())
            return Value();

         auto iter = parent_->get_impl().Begin() + num_;
         return Value(static_cast<DocumentType&>(*iter));
      }

   private:
      const Array* parent_;
      std::ptrdiff_t num_;
   };

   typedef std::reverse_iterator<iterator> reverse_iterator;

   iterator begin() const { return iterator(this, 0); }
   iterator end() const { return iterator(this, get_impl().Size()); }
   reverse_iterator rbegin() const { return reverse_iterator(end()); }
   reverse_iterator rend() const { return reverse_iterator(begin()); }

   void push_back(const Value& value)
   {
      // note: pass a copy as array push back is a move operation in rapidjson
      Value copy(value);
      get_impl().PushBack(copy.get_impl(), s_allocator);
   }

   void clear()
   {
      get_impl().Clear();
   }

   size_t size() const
   {
      return get_impl().Size();
   }

   bool empty() const
   {
      return get_impl().Empty();
   }

   Value at(size_t pos) const
   {
      return (*this)[pos];
   }

   Value front() const
   {
      return (*this)[0];
   }

   Value back() const
   {
      return (*this)[size() - 1];
   }

   iterator erase(iterator pos)
   {
      if (size() == 0)
         return iterator(this, 0);

      auto iter = get_impl().Begin() + pos.num_;
      iter = get_impl().Erase(iter);
      return iterator(this, iter - get_impl().Begin());
   }

   iterator erase(iterator first, iterator last)
   {
      if (size() == 0)
         return iterator(this, 0);

      auto iter = get_impl().Begin() + first.num_;
      auto lastIter = get_impl().Begin() + last.num_;

      iter = get_impl().Erase(iter, lastIter);
      return iterator(this, iter - get_impl().Begin());
   }

   bool operator==(const Array& other) const
   {
      return get_impl() == other.get_impl();
   }
};


typedef Object::Member Member;

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
   else if (boost::is_same<T, unsigned int>::value)
      return value.type() == IntegerType;
   else if (boost::is_same<T, int64_t>::value)
      return value.type() == IntegerType;
   else if (boost::is_same<T, uint64_t>::value)
      return value.type() == IntegerType;
   else if (boost::is_same<T, unsigned long>::value)
      return value.type() == IntegerType;
   else if (boost::is_same<T, double>::value)
      return value.type() == IntegerType || value.type() == RealType;
   else
      return false;
}

inline std::string typeAsString(int type)
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
int asJsonType(const T& object,
               boost::true_type)
{
   return object.type();
}

template <typename T>
int asJsonType(const T& object,
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
struct is_json_type : public boost::is_base_of<Value, T>
{
};

} // namespace detail

template <typename T>
int asJsonType(const T& object)
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
                                 const int expectedType,
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
inline Value toJsonValue(const T& val)
{
   return Value(val);
}

template <typename T>
inline Value toJsonValue(const boost::optional<T>& val)
{
   return val ? Value(*val) : Value();
}

} // namespace detail

template <typename T>
inline Value toJsonValue(const T& val)
{
   return detail::toJsonValue(val);
}

inline Value toJsonValue(const boost::optional<std::string>& val)
{
   return val ? Value(*val) : Value();
}

inline Value toJsonString(const std::string& val)
{
   return Value(val);
}

template<typename T>
Array toJsonArray(const std::vector<T>& vector)
{
   Array results;
   for (const T& val : vector)
   {
      results.push_back(val);
   }
   return results;
}

template<typename T>
Array toJsonArray(const std::set<T>& set)
{
   Array results;
   for (const T& val : set)
   {
      results.push_back(val);
   }
   return results;
}

Object toJsonObject(const std::vector<std::pair<std::string,std::string> >& options);

json::Array toJsonArray(
      const std::vector<std::pair<std::string,std::string> >& options);

std::vector<std::pair<std::string,std::string> > optionsFromJson(
                                      const json::Object& optionsJson);
std::vector<std::pair<std::string,std::string> > optionsFromJson(
                                      const json::Array& optionsJson);

bool fillSetString(const Array& array, std::set<std::string>* pSet);
bool fillVectorString(const Array& array, std::vector<std::string>* pVector);
bool fillVectorInt(const Array& array, std::vector<int>* pVector);
bool fillMap(const Object& array, std::map< std::string, std::vector<std::string> >* pMap);

// Parses, returning true (parsed successfully) or false (did not parse successfully).
bool parse(const std::string& input, Value* pValue);

// Parses, returning the parse error that occurred.
Error parse(const std::string& input, const ErrorLocation& location, Value* pValue);

// Parse input according to the given JSON schema document. Returns an error if either the input or
// schema does not parse, or if the input is not valid according to the schema. Does not apply
// default values from the schema.
Error parseAndValidate(const std::string& input, const std::string& schema, 
      const ErrorLocation& location, Value* pValue);

// Given a JSON schema document, return an object representing the default values named in the
// schema.
Error getSchemaDefaults(const std::string& schema, Value* pValue);

// Given two JSON objects, return their union, with properties in "overlay" preferred when both
// objects contain a property of the same name. Merges sub-objects.
Object merge(const Object& base, const Object& overlay);

void write(const Value& value, std::ostream& os);
void writeFormatted(const Value& value, std::ostream& os);

std::string write(const Value& value);
std::string writeFormatted(const Value& value);

const boost::system::error_category& jsonParseCategory();

} // namespace json
} // namespace core
} // namespace rstudio

namespace rapidjson {
   inline boost::system::error_code make_error_code(ParseErrorCode e) {
      return boost::system::error_code(e, rstudio::core::json::jsonParseCategory());
   }

   inline boost::system::error_condition make_error_condition(ParseErrorCode e) {
      return boost::system::error_condition(e, rstudio::core::json::jsonParseCategory());
   }
}

#endif // CORE_JSON_HPP

