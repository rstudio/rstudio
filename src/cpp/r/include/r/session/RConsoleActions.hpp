/*
 * RConsoleActions.hpp
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

#ifndef R_SESSION_CONSOLE_ACTIONS_HPP
#define R_SESSION_CONSOLE_ACTIONS_HPP

#include <new>
#include <string>
#include <utility>
#include <vector>

#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <shared_core/Memory.hpp>
#include <shared_core/json/Json.hpp>

#include <core/BoostThread.hpp>

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

struct ConsoleAction
{
   int type;
   std::string data;
};

// Add an action to a buffer that allocates its storage as it fills.
//
// Returns false if the storage couldn't grow. The buffer's capacity is then
// lowered to the actions it holds, so the new action replaces the oldest one,
// as it would at the capacity asked for. Otherwise every later push would
// request the same allocation again, and fail with it: the buffer works out
// how far to grow from its size and its storage, and a failed growth changes
// neither.
template <typename Allocator>
bool pushConsoleAction(
   boost::circular_buffer_space_optimized<ConsoleAction, Allocator>& actions,
   ConsoleAction action)
{
   try
   {
      actions.push_back(std::move(action));
      return true;
   }
   catch (const std::bad_alloc&)
   {
      // push_back() grows the storage before it moves from the action, so
      // the action is still intact here
   }

   // the storage is full, or it wouldn't have had to grow; a capacity equal
   // to it is one that neither this nor the push below allocates for
   actions.rset_capacity(actions.size());
   actions.push_back(std::move(action));
   return false;
}

class ConsoleActions : boost::noncopyable
{
private:
   ConsoleActions();
   friend ConsoleActions& consoleActions();
   template <typename T, typename... Args>
   friend T& core::make_leaked(Args&&... args);
   
public:
   int capacity() const;
   void setCapacity(int capacity);

   void add(int type, const std::string& data);
   void notifyInterrupt();
   
   // reset to all but the last prompt
   void reset();
   
   // get actions in their wire-representation
   // (two identically sized arrays, one for type and one for data)
   void asJson(core::json::Object* pActions);
   
   core::Error saveToFile(const core::FilePath& filePath);
   core::Error loadFromFile(const core::FilePath& filePath);

   std::vector<std::string> pendingInput() const { return pendingInput_; }

   // Get console lines with pagination support
   // limit: number of lines to return
   // offset: number of lines to skip
   // fromBottom: if true, start from most recent; if false, start from oldest
   // maxChars: maximum total characters to return (truncates early if exceeded)
   std::vector<std::string> getConsoleLines(int limit,
                                            int offset = 0,
                                            bool fromBottom = true,
                                            int maxChars = 8000);

   // whether the retained output contains text, ignoring ANSI escapes
   // (including the markers added by condition highlighting)
   bool containsOutput(const std::string& text);

private:

   // NOTE: helper methods assume mutex is already locked
   void flush();
   void pushAction(int type, std::string data);
   void pushAction(ConsoleAction action);

   // protect data using a mutex because background threads (e.g.
   // console output capture threads) can interact with console actions
   mutable boost::mutex mutex_;
   ConsoleAction buffer_;

   // allocates as actions are added rather than its whole capacity up front:
   // the capacity is the console_max_lines pref, which has no maximum
   boost::circular_buffer_space_optimized<ConsoleAction> actions_;
   std::vector<std::string> pendingInput_;
};

   
   
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_CONSOLE_ACTIONS_HPP 

