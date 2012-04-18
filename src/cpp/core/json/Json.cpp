/*
 * Json.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/json/Json.hpp>

#include <cstdlib>
#include <sstream>

#include <boost/format.hpp>
#include <boost/scoped_array.hpp>

#include <core/Log.hpp>
#include <core/Thread.hpp>

#include "spirit/json_spirit.h"

namespace core {
namespace json {

json_spirit::Value_type ObjectType = json_spirit::obj_type;
json_spirit::Value_type ArrayType = json_spirit::array_type; 
json_spirit::Value_type StringType = json_spirit::str_type;
json_spirit::Value_type BooleanType = json_spirit::bool_type;
json_spirit::Value_type IntegerType = json_spirit::int_type;
json_spirit::Value_type RealType = json_spirit::real_type;
json_spirit::Value_type NullType = json_spirit::null_type;

json::Value toJsonString(const std::string& val)
{
   return json::Value(val);
}

bool parse(const std::string& input, Value* pValue)
{
   // two threads simultaneously using the json parser has been observed
   // to crash the process. protect it globally with a mutex. note this was
   // probably a result of not defining BOOST_SPIRIT_THREADSAFE (which we
   // have subsequently defined) however since there isn't much documentation 
   // on the behavior of boost spirit w/ threads and the specific behavior
   // of this constant we leave in mutex just to be sure   
      
   static boost::mutex s_spiritMutex ;
   LOCK_MUTEX(s_spiritMutex)
   {
      return json_spirit::read(input, *pValue);
   }
   END_LOCK_MUTEX
   
   // mutex related error
   return false;
}

void write(const Value& value, std::ostream& os)
{
   json_spirit::write(value, os);
}

void writeFormatted(const Value& value, std::ostream& os)
{
   json_spirit::write_formatted(value, os);
}   
   
} // namespace json
} // namespace core



