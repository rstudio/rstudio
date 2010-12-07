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
   HistoryEntry(int index, double timestamp, const std::string& command)
      : index(index), timestamp(timestamp), command(command)
   {
   }
   int index;
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
      indexArray.push_back(entries[i].index);
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
   int nextIndex_;
};
   
   
class History : boost::noncopyable
{
private:
   History() : entryCacheValid_(false), nextIndex_(-1) {}
   friend History& history();
   
public:
   
   void add(const std::string& command)
   {
      std::vector<std::string> commands ;
      commands.push_back(command);
      add(commands);
   }
   
   void add(const std::vector<std::string>& commands)
   {
      // determine the next index 
      int nextEntryIndex = nextIndex();
      
      // invalidiate the entry cache -- obviously we could maintain the entry 
      // cache in memory however the OS will do this as well so better to free 
      // it on the assumption that an add operation means the user has resumed
      // their work and isn't going to actively query the history again 
      // for a while
      entries_.clear();
      entryCacheValid_ = false;
            
      // note current time for stamping entries
      double currentTime = core::date_time::millisecondsSinceEpoch();
      
      // buffer containing history entries to write to file; vector of 
      // history entries to be used in firing event to client
      std::ostringstream ostrEntries ;
      std::vector<HistoryEntry> entries;
      
        // iterate over commands
      for (std::vector<std::string>::const_iterator it = commands.begin();
           it != commands.end();
           ++it)
      {
         // add entry to buffer
         writeEntry(currentTime, *it, &ostrEntries);
         ostrEntries << std::endl;
         
         // add entry to vector
         entries.push_back(HistoryEntry(nextEntryIndex++, currentTime, *it));
      }
      
      // write history entries to file -- if we fail then don't proceed
      // to firing events (because the entries will not be retreivable
      // on subsequent get_history, search_history, etc. calls)
      Error error = appendToFile(historyDatabaseFilePath(), ostrEntries.str());
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
         
      // update the nextIndex. we keep this in memory so that on subsequent
      // calls to add we don't need to re-read the history file to determine
      // the next index
      nextIndex_ = nextEntryIndex;
      
      // fire event
      json::Object entriesJson;
      historyEntriesAsJson(entries, &entriesJson);
      ClientEvent event(client_events::kHistoryEntriesAdded, entriesJson);
      module_context::enqueClientEvent(event);
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
         
         // always update nextIndex to be consistent with our in-memory
         // list of entries. if for some reason we fail to read the history
         // file then to the end user it will appear as if it is empty and
         // our nextIndex_ must reflect this
         nextIndex_ = entries_.size();
      }
      
      return entries_;
   }
   
   int nextIndex() const
   {
      // initially we have no idea how many items are in the file so
      // we need to force a read of file to determine the nextIndex. 
      // subsequently we will just keep the counter in memory (so we can
      // continue to add entries w/ the correct index without having to
      // keep the entire list of history entries in memory)
      if (nextIndex_ == -1)
         nextIndex_ = size();
      return nextIndex_;
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
   mutable int nextIndex_;
};
   
History& history()
{
   static History instance;
   return instance;
}

Error setJsonResultFromHistory(int startIndex, 
                               int endIndex,  
                               json::JsonRpcResponse* pResponse)
{
   // validate indexes
   int historySize = history().size();
   if ( (startIndex < 0)               ||
        (startIndex > historySize)     ||
        (endIndex < 0)                 ||
        (endIndex > historySize) )
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }
   
   // return the entries
   std::vector<HistoryEntry> entries;
   const History& hist = history();
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
   
Error getHistory(const json::JsonRpcRequest& request, 
                 json::JsonRpcResponse* pResponse)
{
   // get start and end index
   int startIndex; // inclusive
   int endIndex;   // exclusive
   Error error = json::readParams(request.params, &startIndex, &endIndex);
   if (error)
      return error;
   
   // truncate indexes if necessary
   int historySize = history().size();
   startIndex = std::min(startIndex, historySize);
   endIndex = std::min(endIndex, historySize);
   
   // return json for the appropriate range
   return setJsonResultFromHistory(startIndex, endIndex, pResponse);   
}
   
Error getRecentHistory(const json::JsonRpcRequest& request, 
                       json::JsonRpcResponse* pResponse)
{
   // get max entries
   int maxEntries;
   Error error = json::readParam(request.params, 0, &maxEntries);
   if (error)
      return error;
   
   // compute indexes
   int endIndex = history().size();
   int startIndex = std::max(endIndex - maxEntries, 0);
   
   // return json for the appropriate range
   return setJsonResultFromHistory(startIndex, endIndex, pResponse);
}

Error searchHistory(const json::JsonRpcRequest& request, 
                    json::JsonRpcResponse* pResponse)
{
   // get the query
   std::string query;
   int maxEntries;
   Error error = json::readParams(request.params, &query, &maxEntries);
   if (error)
      return error;
   
   // convert the query into a list of search terms
   std::vector<std::string> searchTerms;
   boost::char_separator<char> sep;
   boost::tokenizer<boost::char_separator<char> > tok(query, sep);
   std::copy(tok.begin(), tok.end(), std::back_inserter(searchTerms));
   
   // examine the items in the history for matches
   std::vector<HistoryEntry> matchingEntries;
   const History& hist = history();
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
   
Error searchHistoryByPrefix(const json::JsonRpcRequest& request, 
                            json::JsonRpcResponse* pResponse)
{
   // get the query
   std::string prefix;
   int maxEntries;
   Error error = json::readParams(request.params, &prefix, &maxEntries);
   if (error)
      return error;
   
   // trim the prefix
   boost::algorithm::trim(prefix);
   
   // examine the items in the history for matches
   std::vector<HistoryEntry> matchingEntries;
   const History& hist = history();
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
   // add command
   history().add(command);
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
      (bind(registerRpcMethod, "get_history", getHistory))
      (bind(registerRpcMethod, "get_recent_history", getRecentHistory))
      (bind(registerRpcMethod, "search_history", searchHistory))
      (bind(registerRpcMethod, "search_history_by_prefix", searchHistoryByPrefix));
   return initBlock.execute();
}


} // namespace history
} // namespace modules
} // namesapce session

