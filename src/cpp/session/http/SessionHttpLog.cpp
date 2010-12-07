/*
 * SessionHttpLog.cpp
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

#include "SessionHttpLog.hpp"

#include <iostream>

#include <boost/foreach.hpp>

#include <core/Thread.hpp>
#include <core/DateTime.hpp>

#include <core/http/Request.hpp>

using namespace core ;

namespace session {

std::string rstudioRequestIdFromRequest(const core::http::Request& request)
{
   return request.headerValue("X-RS-RID");
}

HttpLog& httpLog()
{
   static HttpLog instance ;
   return instance ;
}

HttpLog::HttpLog()
{
   logEntries_.set_capacity(500);
}

void HttpLog::addEntry(const EntryType& type, const std::string& requestId)
{
   LOCK_MUTEX(mutex_)
   {
      logEntries_.push_back(Entry(type, requestId));
   }
   END_LOCK_MUTEX
}

void HttpLog::asJson(core::json::Array* pEntryArray)
{
   LOCK_MUTEX(mutex_)
   {
      BOOST_FOREACH(const Entry& entry, logEntries_)
      {
         json::Object entryJson;
         entryJson["type"] = entry.type;
         entryJson["id"] = entry.requestId;
         entryJson["ts"] = date_time::millisecondsSinceEpoch(entry.timestamp);
         pEntryArray->push_back(entryJson);
      }
   }
   END_LOCK_MUTEX
}



} // namespace session
