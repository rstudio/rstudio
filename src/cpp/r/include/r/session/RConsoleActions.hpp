/*
 * RConsoleActions.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef R_SESSION_CONSOLE_ACTIONS_HPP
#define R_SESSION_CONSOLE_ACTIONS_HPP

#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <core/BoostThread.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
   
// singleton
class ConsoleActions;
ConsoleActions& consoleActions();
   
#define kConsoleActionPrompt        0
#define kConsoleActionInput         1
#define kConsoleActionOutput        2
#define kConsoleActionOutputError   3

class ConsoleActions : boost::noncopyable
{
private:
   ConsoleActions();
   friend ConsoleActions& consoleActions();
   
public:
   int capacity() const;
   void setCapacity(int capacity);

   void add(int type, const std::string& data);
   void notifyInterrupt();
   
   std::vector<std::string> pendingInput() const { return pendingInput_; }

   // reset to all but the last prompt
   void reset();
   
   // get actions in their wire-representation (two identically sized arrays, 
   // one for type and one for data)
   void asJson(core::json::Object* pActions) const;
   
   core::Error loadFromFile(const core::FilePath& filePath);
   core::Error saveToFile(const core::FilePath& filePath) const;

private:
   // protect data using a mutex because background threads (e.g.
   // console output capture threads) can interact with console actions
   mutable boost::mutex mutex_;
   boost::circular_buffer<core::json::Value> actionsType_;
   boost::circular_buffer<core::json::Value> actionsData_;
   std::vector<std::string> pendingInput_;
};

   
   
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_CONSOLE_ACTIONS_HPP 

