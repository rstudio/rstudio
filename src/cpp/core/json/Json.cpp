/*
 * Json.cpp
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

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <cstdlib>
#include <sstream>

#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/scoped_array.hpp>

#include <core/Log.hpp>
#include <core/Thread.hpp>

#include "spirit/json_spirit.h"

namespace rstudio {
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

json::Object toJsonObject(
      const std::vector<std::pair<std::string,std::string> >& options)
{
   json::Object optionsJson;
   typedef std::pair<std::string,std::string> Pair;
   BOOST_FOREACH(const Pair& option, options)
   {
      optionsJson[option.first] = option.second;
   }
   return optionsJson;
}

std::vector<std::pair<std::string,std::string> > optionsFromJson(
                                      const json::Object& optionsJson)
{
   std::vector<std::pair<std::string,std::string> > options;
   BOOST_FOREACH(const json::Member& member, optionsJson)
   {
      std::string name = member.first;
      json::Value value = member.second;
      if (value.type() == json::StringType)
         options.push_back(std::make_pair(name, value.get_str()));
   }
   return options;
}


bool fillVectorString(const Array& array, std::vector<std::string>* pVector)
{
   for (Array::const_iterator it = array.begin();
        it != array.end();
        ++it)
   {
      if (!isType<std::string>(*it))
         return false;
      pVector->push_back(it->get_str());
   }
   
   return true;
}

bool fillVectorInt(const Array& array, std::vector<int>* pVector)
{
   for (Array::const_iterator it = array.begin();
        it != array.end();
        ++it)
   {
      if (!isType<int>(*it))
         return false;
      pVector->push_back(it->get_int());
   }

   return true;
}

bool fillMap(const Object& object, std::map< std::string, std::vector<std::string> >* pMap)
{
   for (Object::const_iterator it = object.begin();
        it != object.end();
        ++it)
   {
      std::vector<std::string> strings;
      const json::Array& array = it->second.get_array();
      if (!fillVectorString(array, &strings))
         return false;
      
      (*pMap)[it->first] = strings;
   }
   return true;
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

std::string write(const Value& value)
{
   return json_spirit::write(value);
}

std::string writeFormatted(const Value& value)
{
   return json_spirit::write_formatted(value);
}

Error writeCsv(const Array& array, std::ostream& os)
{
   std::size_t nrow = array.size();
   if (nrow == 0)
      return Success();
   
   Value value;
   Object object;
   
   for (std::size_t i = 0; i < nrow; ++i)
   {
      // get array element as object
      value = array[i];
      if (!isType<Object>(value))
         return Error(errc::ParamInvalid, ERROR_LOCATION);
      object = value.get_obj();
      
      if (object.size() == 0)
         continue;
      
      Object::const_iterator it;

      // first object: write names
      if (i == 0)
      {
         it = object.begin();
         json::write(it->first, os);
         for (++it; it != object.end(); ++it)
         {
            os << ",";
            json::write(it->first, os);
         }
         os << "\n";
      }

      // write values
      it = object.begin();
      json::write(it->second, os);
      for (++it; it != object.end(); ++it)
      {
         os << ",";
         json::write(it->second, os);
      }
      os << "\n";
   }
   
   return Success();
   
}

} // namespace json
} // namespace core
} // namespace rstudio



