/*
 * SessionHistoryArchive.hpp
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

#ifndef SESSION_HISTORY_ARCHIVE_HPP
#define SESSION_HISTORY_ARCHIVE_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace history {
   
struct HistoryEntry
{
   HistoryEntry() : index(0), timestamp(0) {}
   HistoryEntry(int index, double timestamp, const std::string& command)
      : index(index), timestamp(timestamp), command(command)
   {
   }
   int index;
   double timestamp;
   std::string command;
};

class HistoryArchive;
HistoryArchive& historyArchive();

class HistoryArchive : boost::noncopyable
{
private:
   HistoryArchive() : entryCacheLastWriteTime_(-1) {}
   friend HistoryArchive& historyArchive();

public:
   static void migrateRhistoryIfNecessary();

public:
   core::Error add(const std::string& command);
   const std::vector<HistoryEntry>& entries() const;

private:
   mutable time_t entryCacheLastWriteTime_;
   mutable std::vector<HistoryEntry> entries_;
};
                       
} // namespace history
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_HISTORY_ARCHIVE_HPP
