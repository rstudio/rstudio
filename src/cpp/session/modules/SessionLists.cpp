/*
 * SessionLists.cpp
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


#include "SessionLists.hpp"

#include <map>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace lists {
   
namespace {

// list names
const char * const kFileMru = "file_mru";
const char * const kProjectMru = kProjectMruList;
const char * const kHelpHistory = "help_history_links";
const char * const kUserDictioanry = "user_dictionary";
const char * const kPlotPublishMru = "plot_publish_mru";
const char * const kAddinsMru = "addins_mru";

// path to lists dir
FilePath s_listsPath;

// registered lists
typedef std::map<std::string, std::size_t> Lists;
Lists s_lists;

// lookup list size
std::size_t listSize(const char* const name)
{
   Lists::const_iterator pos = s_lists.find(name);
   if (pos != s_lists.end())
      return pos->second;
   else
      return -1;
}


FilePath listPath(const std::string& name)
{
   return s_listsPath.complete(name);
}

template <typename T>
Error readList(const std::string& name, T* pList)
{
   // lookup list size (also serves as a validation of list name)
   std::size_t size = listSize(name.c_str());
   if (size <= 0)
   {
      Error error = systemError(boost::system::errc::invalid_argument,
                                ERROR_LOCATION);
      error.addProperty("name", name);
      return error;
   }

   // read the list from disk
   pList->clear();
   FilePath listFilePath = listPath(name);
   if (listFilePath.exists())
   {
      Error error = readCollectionFromFile<T>(listFilePath,
                                              pList,
                                              parseString);
      if (error)
         return error;

      if (pList->size() > size)
         pList->resize(size);
   }

   // return success
   return Success();
}


template <typename T>
Error writeList(const std::string& name, const T& list)
{
   return writeCollectionToFile<T>(listPath(name), list, stringifyString);
}


json::Array listToJson(const std::list<std::string>& list)
{
   json::Array jsonArray;
   BOOST_FOREACH(const std::string& val, list)
   {
      jsonArray.push_back(val);
   }
   return jsonArray;
}

void onListsFileChanged(const core::system::FileChangeEvent& fileChange)
{
   // ignore if deleted
   if (fileChange.type() == core::system::FileChangeEvent::FileRemoved)
      return;

   // ignore if it is the lists directory
   if (fileChange.fileInfo().absolutePath() == s_listsPath.absolutePath())
      return;

   // get the name of the list
   FilePath filePath(fileChange.fileInfo().absolutePath());
   std::string name = filePath.filename();

   // read it
   std::list<std::string> list;
   Error error = readList(name, &list);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Object eventJson;
   eventJson["name"] = name;
   eventJson["list"] = listToJson(list);

   ClientEvent event(client_events::kListChanged, eventJson);
   module_context::enqueClientEvent(event);
}

bool isListNameValid(const std::string& name)
{
   return listSize(name.c_str()) > 0;
}

Error getListName(const json::JsonRpcRequest& request, std::string* pName)
{
   Error error = json::readParam(request.params, 0, pName);
   if (error)
      return error;

   if (!isListNameValid(*pName))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   else
      return Success();
}


Error getListNameAndContents(const json::JsonRpcRequest& request,
                             std::string* pName,
                             std::list<std::string>* pList)
{
   Error error = getListName(request, pName);
   if (error)
      return error;

   return readList(*pName, pList);
}


Error listGet(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   std::string name;
   std::list<std::string> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;

   pResponse->setResult(listToJson(list));

   return Success();
}

Error listSetContents(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string name;
   json::Array jsonList;
   Error error = json::readParams(request.params, &name, &jsonList);
   if (error)
      return error;

   std::list<std::string> list;
   BOOST_FOREACH(const json::Value& val, jsonList)
   {
      if (!json::isType<std::string>(val))
      {
         BOOST_ASSERT(false);
         continue;
      }

      list.push_back(val.get_str());
   }

   return writeList(name, list);
}

Error listInsertItem(bool prepend,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // get params and other context
   std::string name, value;
   std::list<std::string> list;
   std::size_t maxSize;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;
   error = json::readParam(request.params, 1, &value);
   if (error)
      return error;
   maxSize = listSize(name.c_str());

   // remove any existing item with this value
   list.remove(value);

   // enforce size constraints
   while (list.size() >= maxSize)
   {
      if (prepend)
         list.pop_back();
      else
         list.pop_front();
   }

   // do the insert
   if (prepend)
      list.push_front(value);
   else
      list.push_back(value);

   // update the list
   return writeList(name, list);
}


Error listPrependItem(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   return listInsertItem(true, request, pResponse);
}


Error listAppendItem(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   return listInsertItem(false, request, pResponse);
}


Error listRemoveItem(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // get list name and contents
   std::string name;
   std::list<std::string> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;

   // get value to remove
   std::string value;
   error = json::readParam(request.params, 1, &value);

   // remove it
   list.remove(value);

   // update the list
   return writeList(name, list);
}

Error listClear(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // which list
   std::string name;
   Error error = getListName(request, &name);
   if (error)
      return error;

   // write empty list
   return writeList(name, std::list<std::string>());
}

} // anonymous namespace


json::Object allListsAsJson()
{
   json::Object allListsJson;
   for (Lists::const_iterator it = s_lists.begin(); it != s_lists.end(); ++it)
   {
      std::list<std::string> list;
      Error error = readList(it->first, &list);
      if (error)
         LOG_ERROR(error);

      allListsJson[it->first] = listToJson(list);
   }

   return allListsJson;
}

Error initialize()
{  
   // register lists / max sizes
   s_lists[kFileMru] = 15;
   s_lists[kProjectMru] = 15;
   s_lists[kHelpHistory] = 15;
   s_lists[kPlotPublishMru] = 15;
   s_lists[kUserDictioanry] = 10000;
   s_lists[kAddinsMru] = 15;

   // monitor the lists directory
   s_listsPath = module_context::registerMonitoredUserScratchDir(
                                                      kListsPath,
                                                      onListsFileChanged);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "list_get", listGet))
      (bind(registerRpcMethod, "list_set_contents", listSetContents))
      (bind(registerRpcMethod, "list_prepend_item", listPrependItem))
      (bind(registerRpcMethod, "list_append_item", listAppendItem))
      (bind(registerRpcMethod, "list_remove_item", listRemoveItem))
      (bind(registerRpcMethod, "list_clear", listClear));
   return initBlock.execute();
}
   


} // namespace lists
} // namespace modules
} // namespace session
} // namespace rstudio

