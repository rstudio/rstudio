/*
 * RConsoleActions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <r/session/RConsoleActions.hpp>

#include <algorithm>

#include <gsl/gsl>

#include <boost/algorithm/string/split.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/Thread.hpp>

#include <r/ROptions.hpp>

using namespace rstudio::core;

#define kChunkSize 512

namespace rstudio {
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
      return gsl::narrow_cast<int>(actions_.capacity());
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return 0;
}

void ConsoleActions::setCapacity(int capacity)
{
   LOCK_MUTEX(mutex_)
   {
      actions_.set_capacity(capacity);
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
         boost::algorithm::split(input, data, boost::algorithm::is_any_of("\n"));
         pendingInput_.insert(pendingInput_.end(), input.begin(), input.end());
      }
      else if (data.empty())
      {
         return;
      }

      // check for type change, and push into buffer on type change
      bool isOutputType = type == kConsoleActionOutput || kConsoleActionOutputError;
      if (isOutputType && type == pendingAction_.type)
      {
         pendingAction_.data.append(data);
      }
      else
      {
         // the output type has changed; push the pending action
         if (!pendingAction_.data.empty())
            actions_.push_back(pendingAction_);
         
         // update the action with the newly-provided data
         pendingAction_.type = type;
         pendingAction_.data = data;
      }
      
      // consume chunks of data if available
      std::size_t offset = 0;
      while (true)
      {
         if (pendingAction_.data.length() < offset + kChunkSize)
         {
            pendingAction_.data = pendingAction_.data.substr(offset);
            break;
         }
         
         // consume chunk of data
         ConsoleAction action;
         action.type = pendingAction_.type;
         action.data = pendingAction_.data.substr(offset, kChunkSize);
         actions_.push_back(action);
         
         // update offset
         offset += kChunkSize;
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
      actions_.clear();
      pendingAction_.data.clear();
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
      json::Array actionsData;
      
      for (auto&& action : actions_)
      {
         actionsType.push_back(json::Value(action.type));
         actionsData.push_back(json::Value(action.data));
      }
      
      if (!pendingAction_.data.empty())
      {
         actionsType.push_back(json::Value(pendingAction_.type));
         actionsData.push_back(json::Value(pendingAction_.data));
      }
      
      pActions->operator[](kActionType) = actionsType;
      pActions->operator[](kActionData) = actionsData;
   }
   END_LOCK_MUTEX
}

Error ConsoleActions::loadFromFile(const FilePath& filePath)
{
   LOCK_MUTEX(mutex_)
   {
      Error error;
      
      // read from file
      std::string contents;
      error = readStringFromFile(filePath, &contents);
      if (error && !isFileNotFoundError(error))
         return error;

      // parse JSON
      json::Object value;
      error = value.parse(contents);
      if (error)
         return error;
      
      // read type + data fields
      json::Value typeJson = value.getObject()[kActionType];
      if (!typeJson.isArray())
         return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
      
      json::Value dataJson = value.getObject()[kActionData];
      if (!dataJson.isArray())
         return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
      
      json::Array typeArray = typeJson.getArray();
      json::Array dataArray = dataJson.getArray();
      if (typeArray.getSize() != dataArray.getSize())
         return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
      
      for (int i = 0, n = typeArray.getSize(); i < n; i++)
      {
         ConsoleAction action;
         action.type = typeArray[i].getInt();
         action.data = dataArray[i].getString();
         actions_.push_back(action);
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
   std::ostringstream ostr;
   actionsObject.writeFormatted(ostr);
   
   // write to file
   return writeStringToFile(filePath, ostr.str());
}
   
} // namespace session
} // namespace r
} // namespace rstudio



