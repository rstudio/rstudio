/*
 * SessionHistory.cpp
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

#include "SessionHistory.hpp"

#include <iostream>
#include <sstream>
#include <vector>
#include <algorithm>

#include <boost/utility.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/DateTime.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/session/RConsoleHistory.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace history {

namespace {   

struct HistoryEntry
{
   HistoryEntry() : index(0), timestamp(0) {}
   HistoryEntry(long index, double timestamp, const std::string& command)
      : index(index), timestamp(timestamp), command(command)
   {
   }
   long index;
   double timestamp;
   std::string command;
};

void historyEntriesAsJson(const std::vector<HistoryEntry>& entries,
                          json::Object* pEntriesJson)
{
   // clear inbound
   pEntriesJson->clear();
   
   // populate arrays
   json::Array indexArray, timestampArray, commandArray;
   for (std::size_t i=0; i<entries.size(); i++)
   {
      indexArray.push_back((double)entries[i].index);
      timestampArray.push_back(entries[i].timestamp);
      commandArray.push_back(entries[i].command);
   }
   
   // set arrays into result object
   pEntriesJson->operator[]("index") = indexArray;
   pEntriesJson->operator[]("timestamp") = timestampArray;
   pEntriesJson->operator[]("command") = commandArray;
}
   

// simple reader for parsing lines of history file
class HistoryEntryReader
{
public:
   HistoryEntryReader() : nextIndex_(0) {}
   
   ReadCollectionAction operator()(const std::string& line, 
                                   HistoryEntry* pEntry)
   {
      // if the line doesn't have a ':' then ignore it
      if (line.find(':') == std::string::npos)
         return ReadCollectionIgnoreLine;

      pEntry->index = nextIndex_++; 
      std::istringstream istr(line);
      istr >> pEntry->timestamp ;
      istr.ignore(1, ':');
      std::getline(istr, pEntry->command);
      
      // if we had a read failure log it and return ignore state
      if (!istr.fail())
      {
         return ReadCollectionAddLine;
      }
      else
      {
         LOG_ERROR_MESSAGE("unexpected io error reading history line: " + 
                           line);
         return ReadCollectionIgnoreLine;  
      }
   }
private:
   long nextIndex_;
};
   
   
class History : boost::noncopyable
{
private:
   History() : entryCacheValid_(false) {}
   friend History& historyArchive();
   
public:
   
   Error add(const std::string& command)
   {
      // invalidiate the entry cache -- obviously we could maintain the entry 
      // cache in memory however the OS will do this as well so better to free 
      // it on the assumption that an add operation means the user has resumed
      // their work and isn't going to actively query the history again 
      // for a while
      entries_.clear();
      entryCacheValid_ = false;
            
      // write the entry to the file
      std::ostringstream ostrEntry ;
      double currentTime = core::date_time::millisecondsSinceEpoch();
      writeEntry(currentTime, command, &ostrEntry);
      ostrEntry << std::endl;
      return appendToFile(historyDatabaseFilePath(), ostrEntry.str());
   }
   
   std::vector<HistoryEntry>::const_iterator begin() const
   {
      return entries().begin();
   }
   
   std::vector<HistoryEntry>::const_iterator end() const
   {
      return entries().end();
   }
   
   std::vector<HistoryEntry>::const_reverse_iterator rbegin() const
   {
      return entries().rbegin();
   }
   
   std::vector<HistoryEntry>::const_reverse_iterator rend() const
   {
      return entries().rend();
   }
   
   int size() const 
   {
      return entries().size();
   }
   
   static void migrateRhistoryIfNecessary()
   {
      // if the history database doesn't exist see if we can migrate the 
      // old .Rhistory file
      FilePath historyDBPath = historyDatabaseFilePath();
      if (!historyDBPath.exists())
         attemptRhistoryMigration() ;
   }
   

private:
   
   const std::vector<HistoryEntry>& entries() const
   {
      if (!entryCacheValid_)
      {
         // read history file if it exists
         FilePath historyDBPath = historyDatabaseFilePath();
         if (historyDBPath.exists())
         {
            Error error = readCollectionFromFile<std::vector<HistoryEntry> >(
                                                         historyDBPath,
                                                         &entries_,
                                                         HistoryEntryReader());
            if (error)
            {
               LOG_ERROR(error);
            }
            else
            {
               entryCacheValid_ = true;
            }
         }
         else
         {
            entryCacheValid_ = true; // doesn't exist so empty cache is valid
         }
      }
      
      return entries_;
   }
   
   static void writeEntry(double timestamp, 
                          const std::string& command, 
                          std::ostream* pOS) 
   {
      *pOS << std::fixed << std::setprecision(0) 
           << timestamp << ":" << command;
   }
   
   static std::string migratedHistoryEntry(const std::string& command)
   {
      std::ostringstream ostr ;
      writeEntry(0, command, &ostr);
      return ostr.str();
   }
   
   static void attemptRhistoryMigration() 
   {
      Error error = writeCollectionToFile<r::session::ConsoleHistory>(
                                                   historyDatabaseFilePath(),
                                                   r::session::consoleHistory(),
                                                   migratedHistoryEntry);  
      
      // log any error which occurs
      if (error)
         LOG_ERROR(error);
   }
   
   static FilePath historyDatabaseFilePath()
   {
      return module_context::userScratchPath().complete("history_database");
   }
   
   
private:
   mutable bool entryCacheValid_;
   mutable std::vector<HistoryEntry> entries_;
};
   
History& historyArchive()
{
   static History instance;
   return instance;
}

Error setJsonResultFromHistory(long startIndex,
                               long endIndex,
                               json::JsonRpcResponse* pResponse)
{
   // validate indexes
   long historySize = historyArchive().size();
   if ( (startIndex < 0)               ||
        (startIndex > historySize)     ||
        (endIndex < 0)                 ||
        (endIndex > historySize) )
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }
   
   // return the entries
   std::vector<HistoryEntry> entries;
   const History& hist = historyArchive();
   std::copy(hist.begin() + startIndex,
             hist.begin() + endIndex,
             std::back_inserter(entries));
   json::Object entriesJson;
   historyEntriesAsJson(entries, &entriesJson);
   pResponse->setResult(entriesJson);
   return Success();
}
   
bool matches(const HistoryEntry& entry,
             const std::vector<std::string>& searchTerms)
{   
   // look for each search term in the input
   for (std::vector<std::string>::const_iterator it = searchTerms.begin();
        it != searchTerms.end();
        ++it)
   {
      if (!boost::algorithm::contains(entry.command, *it))
         return false;
   }
   
   // had all of the search terms, return true
   return true;
}


void historyRangeAsJson(long startIndex,
                        long endIndex,
                        json::Object* pHistoryJson)
{
   // get the subset of entries
   std::vector<HistoryEntry> historyEntries;
   std::vector<std::string> entries;
   r::session::consoleHistory().subset(startIndex, endIndex, &entries);
   for (std::vector<std::string>::const_iterator it = entries.begin();
        it != entries.end();
        ++it)
   {
       historyEntries.push_back(HistoryEntry(startIndex++, 0, *it));
   }

   // convert to json
   historyEntriesAsJson(historyEntries, pHistoryJson);
}

Error getRecentHistory(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // get params
   double maxItemsDbl;
   Error error = json::readParam(request.params, 0, &maxItemsDbl);
   if (error)
      return error;
   long maxItems = (long)maxItemsDbl;

   // alias console history
   using namespace r::session;
   ConsoleHistory& consoleHistory = r::session::consoleHistory();

   // validate
   if (maxItems <= 0)
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // compute start and end indexes
   long startIndex = std::max(0L, consoleHistory.size() - maxItems);
   long endIndex = consoleHistory.size();

   // get json and set it
   json::Object historyJson;
   historyRangeAsJson(startIndex, endIndex, &historyJson);
   pResponse->setResult(historyJson);
   return Success();
}

Error getHistoryItems(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // get start and end index
   double startIndex; // inclusive
   double endIndex;   // exclusive
   Error error = json::readParams(request.params, &startIndex, &endIndex);
   if (error)
      return error;

   // get the range and return it
   json::Object historyJson;
   historyRangeAsJson((long)startIndex, (long)endIndex, &historyJson);
   pResponse->setResult(historyJson);
   return Success();
}

Error removeHistoryItems(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   return Success();
}

Error clearHistory(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   r::session::consoleHistory().clear();

   json::Array historyJson;
   ClientEvent event(client_events::kConsoleResetHistory, historyJson);
   module_context::enqueClientEvent(event);

   return Success();
}

Error getHistoryArchiveItems(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // get start and end index
   double startIndex; // inclusive
   double endIndex;   // exclusive
   Error error = json::readParams(request.params, &startIndex, &endIndex);
   if (error)
      return error;
   
   // truncate indexes if necessary
   long historySize = historyArchive().size();
   long startIndexLong = std::min((long)startIndex, historySize);
   long endIndexLong = std::min((long)endIndex, historySize);
   
   // return json for the appropriate range
   return setJsonResultFromHistory(startIndexLong, endIndexLong, pResponse);
}
   
Error searchHistoryArchive(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // get the query
   std::string query;
   double maxEntriesDbl;
   Error error = json::readParams(request.params, &query, &maxEntriesDbl);
   if (error)
      return error;
   long maxEntries = maxEntriesDbl;
   
   // convert the query into a list of search terms
   std::vector<std::string> searchTerms;
   boost::char_separator<char> sep;
   boost::tokenizer<boost::char_separator<char> > tok(query, sep);
   std::copy(tok.begin(), tok.end(), std::back_inserter(searchTerms));
   
   // examine the items in the history for matches
   std::vector<HistoryEntry> matchingEntries;
   const History& hist = historyArchive();
   for (std::vector<HistoryEntry>::const_reverse_iterator 
            it = hist.rbegin();
            it != hist.rend();
            ++it)
   {
      // check limit
      if (matchingEntries.size() >= static_cast<std::size_t>(maxEntries))
         break;

      // look for match
      if (matches(*it, searchTerms))
      {
         // add entry
         matchingEntries.push_back(*it);
      }
   }

   // return json
   json::Object entriesJson;
   historyEntriesAsJson(matchingEntries, &entriesJson);
   pResponse->setResult(entriesJson);
   return Success();
}
   
Error searchHistoryArchiveByPrefix(const json::JsonRpcRequest& request,
                                   json::JsonRpcResponse* pResponse)
{
   // get the query
   std::string prefix;
   double maxEntriesDbl;
   Error error = json::readParams(request.params, &prefix, &maxEntriesDbl);
   if (error)
      return error;
   long maxEntries = (long)maxEntriesDbl;
   
   // trim the prefix
   boost::algorithm::trim(prefix);
   
   // examine the items in the history for matches
   std::vector<HistoryEntry> matchingEntries;
   const History& hist = historyArchive();
   for (std::vector<HistoryEntry>::const_reverse_iterator 
        it = hist.rbegin();
        it != hist.rend();
        ++it)
   {
      // check limit
      if (matchingEntries.size() >= static_cast<std::size_t>(maxEntries))
         break;
      
      // look for match
      if (boost::algorithm::starts_with(it->command, prefix))
         matchingEntries.push_back(*it);
   }
   
   // return json
   json::Object entriesJson;
   historyEntriesAsJson(matchingEntries, &entriesJson);
   pResponse->setResult(entriesJson);
   return Success();
}

void onHistoryAdd(const std::string& command)
{   
   // add command to history archive
   Error error = historyArchive().add(command);
   if (error)
      LOG_ERROR(error);

   // fire event
   int entryIndex = r::session::consoleHistory().size() - 1;
   std::vector<HistoryEntry> entries;
   entries.push_back(HistoryEntry(entryIndex, 0, command));
   json::Object entriesJson;
   historyEntriesAsJson(entries, &entriesJson);
   ClientEvent event(client_events::kHistoryEntriesAdded, entriesJson);
   module_context::enqueClientEvent(event);
}

} // anonymous namespace
   
   
Error initialize()
{
   // migrate .Rhistory if necessary
   History::migrateRhistoryIfNecessary();
   
   // connect to console history add event
   r::session::consoleHistory().connectOnAdd(onHistoryAdd);   
   
   // install handlers
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_recent_history", getRecentHistory))
      (bind(registerRpcMethod, "get_history_items", getHistoryItems))
      (bind(registerRpcMethod, "remove_history_items", removeHistoryItems))
      (bind(registerRpcMethod, "clear_history", clearHistory))
      (bind(registerRpcMethod, "get_history_archive_items", getHistoryArchiveItems))
      (bind(registerRpcMethod, "search_history_archive", searchHistoryArchive))
      (bind(registerRpcMethod, "search_history_archive_by_prefix", searchHistoryArchiveByPrefix));
   return initBlock.execute();
}


} // namespace history
} // namespace modules
} // namesapce session

