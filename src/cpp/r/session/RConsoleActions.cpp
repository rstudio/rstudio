/*
 * RConsoleActions.cpp
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

#include <r/session/RConsoleActions.hpp>

#include <algorithm>

#include <boost/algorithm/string/split.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/Thread.hpp>

#include <r/ROptions.hpp>

using namespace core ;

namespace r {
namespace session {

namespace {   
const char * const kActionType = "type";
const char * const kActionData = "data";
}
   
ConsoleActions& consoleActions()
{
   static ConsoleActions instance;
   return instance;
}
   
ConsoleActions::ConsoleActions()
{
   setCapacity(1000);
}
   
int ConsoleActions::capacity() const
{
   LOCK_MUTEX(mutex_)
   {
      return actionsType_.capacity();
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return 0;
}

void ConsoleActions::setCapacity(int capacity)
{
   LOCK_MUTEX(mutex_)
   {
      actionsType_.set_capacity(capacity);
      actionsData_.set_capacity(capacity);
   }
   END_LOCK_MUTEX
}
   
void ConsoleActions::add(int type, const std::string& data)
{
   LOCK_MUTEX(mutex_)
   {
      // manage pending input buffer
      if (type == kConsoleActionPrompt &&
          data == r::options::getOption<std::string>("prompt"))
      {
         pendingInput_.clear();
      }
      else if (type == kConsoleActionInput)
      {
         std::vector<std::string> input;
         boost::algorithm::split(input,
                                 data,
                                 boost::algorithm::is_any_of("\n"));
         pendingInput_.insert(pendingInput_.end(), input.begin(), input.end());
      }


      // automatically combine consecutive output actions (up to 512 bytes)
      // we enforce a limit so that the limit defined for our circular buffer
      // (see setCapacity above) implies a content size limit as well (if we
      // didn't cap the size of combined output then the output actions could
      // grow to arbitrary size)
      if (type == kConsoleActionOutput &&
          actionsType_.size() > 0      &&
          actionsType_.back().get_value<int>() == kConsoleActionOutput &&
          actionsData_.back().get_str().size() < 512)
      {
         actionsData_.back() = actionsData_.back().get_str() + data;
      }
      else
      {
         actionsType_.push_back(type);
         actionsData_.push_back(data);
      }
   }
   END_LOCK_MUTEX
}

void ConsoleActions::notifyInterrupt()
{
   LOCK_MUTEX(mutex_)
   {
      pendingInput_.clear();
   }
   END_LOCK_MUTEX
}

void ConsoleActions::reset()
{
   LOCK_MUTEX(mutex_)
   {
      // clear the existing actions
      actionsType_.clear();
      actionsData_.clear();
   }
   END_LOCK_MUTEX
}
   
void ConsoleActions::asJson(json::Object* pActions) const
{
   LOCK_MUTEX(mutex_)
   {
      // clear inbound
      pActions->clear();

      // copy actions and insert into destination
      json::Array actionsType;
      std::copy(actionsType_.begin(),
                actionsType_.end(),
                std::back_inserter(actionsType));
      pActions->operator[](kActionType) = actionsType;

      // copy data and insert into destination
      json::Array actionsData;
      std::copy(actionsData_.begin(),
                actionsData_.end(),
                std::back_inserter(actionsData));
      pActions->operator[](kActionData) = actionsData;
   }
   END_LOCK_MUTEX
}

Error ConsoleActions::loadFromFile(const FilePath& filePath)
{
   LOCK_MUTEX(mutex_)
   {
      actionsType_.clear();
      actionsData_.clear();

      if (filePath.exists())
      {
         // read from file
         std::string actionsJson ;
         Error error = readStringFromFile(filePath, &actionsJson);
         if (error)
            return error ;

         // parse json and confirm it contains an object
         json::Value value;
         if ( json::parse(actionsJson, &value) &&
              (value.type() == json::ObjectType) )
         {
            json::Object& actions = value.get_obj();

            const json::Value& typeValue = actions[kActionType] ;
            if (typeValue.type() == json::ArrayType)
            {
               const json::Array& actionsType = typeValue.get_array();
               std::copy(actionsType.begin(),
                         actionsType.end(),
                         std::back_inserter(actionsType_));
            }
            else
            {
               LOG_WARNING_MESSAGE("unexpected json type in: " + actionsJson);
            }

            json::Value& dataValue = actions[kActionData] ;
            if ( dataValue.type() == json::ArrayType )
            {
               const json::Array& actionsData = dataValue.get_array();
               std::copy(actionsData.begin(),
                         actionsData.end(),
                         std::back_inserter(actionsData_));
            }
            else
            {
               LOG_WARNING_MESSAGE("unexpected json type in: " + actionsJson);
            }
         }
         else
         {
            LOG_WARNING_MESSAGE("unexpected json type in: " + actionsJson);
         }
      }
   }
   END_LOCK_MUTEX
   
   return Success();
}
   
Error ConsoleActions::saveToFile(const core::FilePath& filePath) const
{
   // write actions
   json::Object actionsObject;
   asJson(&actionsObject);
   std::ostringstream ostr ;
   json::writeFormatted(actionsObject, ostr);
   
   // write to file
   return writeStringToFile(filePath, ostr.str());
}
   
} // namespace session
} // namespace r



