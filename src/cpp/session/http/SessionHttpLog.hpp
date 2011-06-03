/*
 * SessionHttpLog.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_HTTP_LOG_HPP
#define SESSION_HTTP_LOG_HPP

#include <iosfwd>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <core/BoostThread.hpp>

#include <core/json/Json.hpp>

namespace core {
   class Error;
   namespace http {
      class Request;
   }
}

namespace session {

std::string rstudioRequestIdFromRequest(const core::http::Request& request);

// singleton
class HttpLog;
HttpLog& httpLog();

class HttpLog : boost::noncopyable
{
public:
   enum EntryType
   {
      ConnectionReceived = 1,
      ConnectionDequeued = 2,
      ConnectionResponded = 3,
      ConnectionTerminated = 4,
      ConnectionError = 5
   };

private:
   HttpLog();
   friend HttpLog& httpLog();

public:
   void addEntry(const EntryType& type, const std::string& requestId);

   void asJson(core::json::Array* pEntryArray);

private:
   struct Entry
   {
      Entry(const EntryType& type, const std::string& requestId)
         : type(type), requestId(requestId)
      {
         timestamp = boost::posix_time::microsec_clock::universal_time();
      }

      EntryType type;
      std::string requestId;
      boost::posix_time::ptime timestamp;
   };

   // make mutex heap based so we don't get destructor assertions
   // when it is closed within a forked child (from multicore)
   boost::mutex* pMutex_;
   boost::circular_buffer<Entry> logEntries_;
};

} // namespace session

#endif // SESSION_HTTP_LOG_HPP

