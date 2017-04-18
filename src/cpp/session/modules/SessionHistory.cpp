/*
 * SessionHistory.cpp
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

#include "SessionHistory.hpp"

#include <iostream>
#include <sstream>
#include <vector>
#include <algorithm>

#include <boost/utility.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/DateTime.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RConsoleHistory.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionHistoryArchive.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace history {

namespace {   


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

Error setJsonResultFromHistory(int startIndex,
                               int endIndex,
                               json::JsonRpcResponse* pResponse)
{
   // get all entries
   const std::vector<HistoryEntry>& allEntries = historyArchive().entries();

   // validate indexes
   int historySize = allEntries.size();
   if ( (startIndex < 0)               ||
        (startIndex > historySize)     ||
        (endIndex < 0)                 ||
        (endIndex > historySize) )
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }
   
   // return the entries
   std::vector<HistoryEntry> entries;
   std::copy(allEntries.begin() + startIndex,
             allEntries.begin() + endIndex,
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


void historyRangeAsJson(int startIndex,
                        int endIndex,
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
   int maxItems;
   Error error = json::readParam(request.params, 0, &maxItems);
   if (error)
      return error;

   // alias console history
   using namespace rstudio::r::session;
   ConsoleHistory& consoleHistory = r::session::consoleHistory();

   // validate
   if (maxItems <= 0)
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // compute start and end indexes
   int startIndex = std::max(0, consoleHistory.size() - maxItems);
   int endIndex = consoleHistory.size();

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
   int startIndex; // inclusive
   int endIndex;   // exclusive
   Error error = json::readParams(request.params, &startIndex, &endIndex);
   if (error)
      return error;

   // get the range and return it
   json::Object historyJson;
   historyRangeAsJson(startIndex, endIndex, &historyJson);
   pResponse->setResult(historyJson);
   return Success();
}

void enqueConsoleResetHistoryEvent(bool preserveUIContext)
{
   json::Array historyJson;
   r::session::consoleHistory().asJson(&historyJson);
   json::Object resetJson;
   resetJson["history"] = historyJson;
   resetJson["preserve_ui_context"] = preserveUIContext;
   ClientEvent event(client_events::kConsoleResetHistory, resetJson);
   module_context::enqueClientEvent(event);
}

Error removeHistoryItems(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // get indexes
   json::Array bottomIndexesJson;
   Error error = json::readParam(request.params, 0, &bottomIndexesJson);
   if (error)
      return error;

   // convert to top indexes
   int historySize = r::session::consoleHistory().size();
   std::vector<int> indexes;
   for (std::size_t i=0; i<bottomIndexesJson.size(); i++)
   {  
      const json::Value& value = bottomIndexesJson[i];
      if (json::isType<int>(value))
      {
         int bottomIndex = value.get_int();
         int topIndex = historySize - 1 - bottomIndex;
         indexes.push_back(topIndex);
      }
   }

   // remove them
   r::session::consoleHistory().remove(indexes);

   // enque event
   enqueConsoleResetHistoryEvent(true);

   return Success();
}

Error clearHistory(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   r::session::consoleHistory().clear();

   enqueConsoleResetHistoryEvent(false);

   return Success();
}

Error getHistoryArchiveItems(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // get start and end index
   int startIndex; // inclusive
   int endIndex;   // exclusive
   Error error = json::readParams(request.params, &startIndex, &endIndex);
   if (error)
      return error;
   
   // truncate indexes if necessary
   int historySize = historyArchive().entries().size();
   startIndex = std::min(startIndex, historySize);
   endIndex = std::min(endIndex, historySize);
   
   // return json for the appropriate range
   return setJsonResultFromHistory(startIndex, endIndex, pResponse);
}
   
Error searchHistoryArchive(const json::JsonRpcRequest& request,
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
   const std::vector<HistoryEntry>& allEntries =  historyArchive().entries();
   std::vector<HistoryEntry> matchingEntries;
   for (std::vector<HistoryEntry>::const_reverse_iterator 
            it = allEntries.rbegin();
            it != allEntries.rend();
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
   int maxEntries;
   bool uniqueOnly;
   Error error = json::readParams(request.params,
                                  &prefix, &maxEntries, &uniqueOnly);
   if (error)
      return error;
   
   // trim the prefix
   boost::algorithm::trim(prefix);
   
   // examine the items in the history for matches
   const std::vector<HistoryEntry>& allEntries = historyArchive().entries();
   std::set<std::string> matchedCommands;
   std::vector<HistoryEntry> matchingEntries;
   for (std::vector<HistoryEntry>::const_reverse_iterator 
        it = allEntries.rbegin();
        it != allEntries.rend();
        ++it)
   {
      // check limit
      if (matchingEntries.size() >= static_cast<std::size_t>(maxEntries))
         break;
      
      // look for match 
      if (boost::algorithm::starts_with(it->command, prefix))
      {
         if (!uniqueOnly || (matchedCommands.count(it->command) == 0))
         {
            matchingEntries.push_back(*it);
            matchedCommands.insert(it->command);
         }
      }
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

SEXP rs_timestamp(SEXP stampSEXP) {
   std::string stamp = r::sexp::safeAsString(stampSEXP);
   r::session::consoleHistory().add(stamp);

   return R_NilValue;
}

} // anonymous namespace
   
   
Error initialize()
{
   // migrate .Rhistory if necessary
   HistoryArchive::migrateRhistoryIfNecessary();
   
   // connect to console history add event
   r::session::consoleHistory().connectOnAdd(onHistoryAdd);

   // register timestamp function
   R_CallMethodDef methodDef;
   methodDef.name = "rs_timestamp" ;
   methodDef.fun = (DL_FUNC) rs_timestamp;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);   

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
} // namespace session
} // namespace rstudio

