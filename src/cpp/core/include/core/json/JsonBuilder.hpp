/*
 * JsonBuilder.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_JSON_BUILDER_HPP
#define CORE_JSON_BUILDER_HPP

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
namespace json {

class JSON
{
public:
   JSON() : value_() {}

   JSON(const core::json::Value& value) : value_(value) {}

   JSON(const char* value) : value_(value) {}
   JSON(const std::string& value) : value_(value) {}

 
   template<
      typename T,
      std::enable_if_t<std::is_arithmetic_v<T>, bool> = true
   >
   JSON(const T& value)
      : value_(value)
   {
   }

   JSON(std::initializer_list<std::pair<const char*, JSON>> values)
   {
      json::Object object;
      for (auto&& value : values)
      {
         object[value.first] = value.second.value_;
      }
      value_ = object;
   }

   static JSON Array(std::initializer_list<JSON> values)
   {
      JSON json;

      json::Array array;
      for (auto&& value : values)
         array.push_back(value.value_);
      json.value_ = array;

      return json;
   }

   const json::Value get() const
   {
      return value_;
   }

   inline operator core::json::Value() const
   {
      return value_;
   }

   inline operator core::json::Object() const
   {
      return value_.getObject();
   }


private:
   json::Value value_;
};

} // end namespace json
} // end namespace core
} // end namespace rstudio

namespace rstudio {
using JSON = core::json::JSON;
} // end namespace rstudio

#endif /* CORE_JSON_BUILDER_HPP */