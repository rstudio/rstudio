/*
 * RConsoleHistory.hpp
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

#ifndef R_SESSION_CONSOLE_HISTORY_HPP
#define R_SESSION_CONSOLE_HISTORY_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <core/BoostSignals.hpp>
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
class ConsoleHistory;
ConsoleHistory& consoleHistory();
   
class ConsoleHistory : boost::noncopyable
{
public:
   typedef boost::circular_buffer<std::string>::value_type value_type;
   typedef boost::circular_buffer<std::string>::const_iterator const_iterator;
   typedef RSTUDIO_BOOST_SIGNAL<void (const std::string&)> AddSignal;

private:
   ConsoleHistory();
   friend ConsoleHistory& consoleHistory();
   // COPYING: boost::noncopyable
      
public:   
   void setCapacity(int capacity);

   void setCapacityFromRHistsize();

   int capacity() const
   {
      return static_cast<int>(historyBuffer_.capacity());
   }

   void setRemoveDuplicates(bool removeDuplicates);
   
   void add(const std::string& command);
   
   const_iterator begin() const { return historyBuffer_.begin(); }
   const_iterator end() const { return historyBuffer_.end(); }
   
   int size() const
   {
      return static_cast<int>(historyBuffer_.size());
   }

   void clear();

   void remove(const std::vector<int>& indexes);

   void subset(int beginIndex, // inclusive
               int endIndex,   // exclusive,
               std::vector<std::string>* pEntries) const;

   void asJson(core::json::Array* pHistoryArray) const;
   
   core::Error loadFromFile(const core::FilePath& filePath, bool verifyFile);
   core::Error saveToFile(const core::FilePath& filePath) const;
   
   RSTUDIO_BOOST_CONNECTION connectOnAdd(const AddSignal::slot_function_type& slot)
   {
      return onAdd_.connect(slot);
   }

private:
   void safeRemove(int index);
   
private:   
   bool removeDuplicates_;
   boost::circular_buffer<std::string> historyBuffer_;
   AddSignal onAdd_;
};
   
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_CONSOLE_HISTORY_HPP 

