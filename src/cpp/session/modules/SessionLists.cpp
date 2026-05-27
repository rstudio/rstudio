/*
 * SessionLists.cpp
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


#include "SessionLists.hpp"

#include <map>
#include <set>

#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>
#include <boost/bind/bind.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/collection/MruList.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules { 
namespace lists {
   
namespace {

using namespace collection;

// list names
const char * const kFileMru = "file_mru";
const char * const kProjectMru = kProjectMruList;
const char * const kProjectNameMru = kProjectNameMruList;
const char * const kHelpHistory = "help_history_links";
const char * const kUserDictionary = "user_dictionary";
const char * const kPlotPublishMru = "plot_publish_mru";
const char * const kCommandPaletteMru = "command_palette_mru";

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
   return s_listsPath.completePath(name);
}

Error readList(const std::string& name,
               boost::shared_ptr<MruList>* pList)
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
   if (name == kProjectNameMru)
      // project name list stores the optional project display name after separator character
      pList->reset(new MruList(listPath(name), size, kProjectNameSepChar));
   else
      pList->reset(new MruList(listPath(name), size));
   return (*pList)->initialize();
}

json::Array listToJson(const std::list<std::string>& list)
{
   json::Array jsonArray;
   for (const std::string& val : list)
   {
      jsonArray.push_back(val);
   }
   return jsonArray;
}

Error migrateLegacyProjectMru()
{
   // copy the legacy project mru list to the new list to prevent loss on RStudio upgrade
   FilePath legacyProjectMru = listPath(kProjectMru);
   FilePath newProjectMru = listPath(kProjectNameMru);
   if (legacyProjectMru.exists() && !newProjectMru.exists())
   {
      std::list<std::string> legacyList;
      Error error = readCollectionFromFile<std::list<std::string>>(legacyProjectMru, &legacyList, parseString);
      if (error)
      {
         // can't read the legacy list, just delete it and move on
         LOG_ERROR(error);
         legacyProjectMru.remove();
      }
      else
      {
         LOG_INFO_MESSAGE("Migrating legacy project MRU list to new project MRU list");
         error = writeCollectionToFile<std::list<std::string>>(newProjectMru, legacyList, stringifyString);
         if (error)
         {
            return error;
         }
      }
   }
   return Success();
}

std::string removeCustomProjectName(const std::string& str)
{
   std::size_t pos = str.find(kProjectNameSepChar);
   if (pos != std::string::npos)
      return str.substr(0, pos);
   else
      return str;
}

// Split a project_name_mru entry into (path, name). Returns an empty name
// when the entry has no separator.
std::pair<std::string, std::string> splitProjectMruEntry(const std::string& entry)
{
   std::size_t pos = entry.find(kProjectNameSepChar);
   if (pos == std::string::npos)
      return std::make_pair(entry, std::string());
   return std::make_pair(entry.substr(0, pos), entry.substr(pos + 1));
}

// Join a (path, name) pair back into the on-disk MRU entry form.
std::string joinProjectMruEntry(const std::string& path, const std::string& name)
{
   if (name.empty())
      return path;
   return path + kProjectNameSepChar + name;
}

// Canonicalize the path portion of a project_name_mru entry. Aliased paths
// (~/...) are resolved to their absolute form so the on-disk representation
// is invariant under changes in how the home directory is resolved.
std::string canonicalizeProjectMruEntry(const std::string& entry)
{
   auto parts = splitProjectMruEntry(entry);
   if (parts.first.empty() || !FilePath::isAliasedPath(parts.first))
      return entry;
   std::string absolutePath =
      module_context::resolveAliasedPath(parts.first).getAbsolutePath();
   return joinProjectMruEntry(absolutePath, parts.second);
}

// Alias the path portion of a project_name_mru entry for delivery to the
// client. Paths within the user's home directory are rewritten to ~ form;
// paths already aliased or outside the home directory pass through.
std::string aliasProjectMruEntry(const std::string& entry)
{
   auto parts = splitProjectMruEntry(entry);
   if (parts.first.empty() || FilePath::isAliasedPath(parts.first))
      return entry;
   std::string aliasedPath =
      module_context::createAliasedPath(FilePath(parts.first));
   return joinProjectMruEntry(aliasedPath, parts.second);
}

// Serialize a named list's contents to JSON, aliasing project_name_mru paths
// so the client sees ~ form when paths are within the user's home directory.
// Other lists pass through unchanged.
json::Array listContentsToJson(const std::string& name,
                               const std::list<std::string>& contents)
{
   if (name != kProjectNameMru)
      return listToJson(contents);

   json::Array jsonArray;
   for (const std::string& val : contents)
      jsonArray.push_back(aliasProjectMruEntry(val));
   return jsonArray;
}

Error syncLegacyProjectMru()
{
   // read the current project mru list (project_name_mru)
   boost::shared_ptr<MruList> list;
   Error error = readList(kProjectNameMru, &list);
   if (error)
      return error;

   // write out the legacy list (project_mru) without the custom names
   error = writeCollectionToFile<std::list<std::string>>(listPath(kProjectMru),
                                                         list->contents(),
                                                         removeCustomProjectName);
   if (error)
      return error;

   return Success();
}

// One-time migration: convert any aliased paths in the on-disk
// project_name_mru file to absolute form and drop the duplicates that
// resulted from the same project being recorded under both aliased and
// absolute representations (see rstudio/rstudio#17225). After this runs
// the file is canonical: every entry is an absolute path, optionally
// followed by a name suffix. New additions are kept canonical by
// canonicalizeProjectMruEntry on the RPC path.
Error normalizeProjectMru()
{
   FilePath mruPath = listPath(kProjectNameMru);
   if (!mruPath.exists())
      return Success();

   std::list<std::string> entries;
   Error error = readCollectionFromFile<std::list<std::string>>(
      mruPath, &entries, parseString);
   if (error)
      return error;

   std::list<std::string> normalized;
   std::set<std::string> seenPaths;
   bool changed = false;
   for (const std::string& entry : entries)
   {
      std::string canonical = canonicalizeProjectMruEntry(entry);
      if (canonical != entry)
         changed = true;

      // dedupe by path, preserving the first occurrence's ordering and name
      std::string path = splitProjectMruEntry(canonical).first;
      if (!seenPaths.insert(path).second)
      {
         changed = true;
         continue;
      }
      normalized.push_back(canonical);
   }

   if (!changed)
      return Success();

   LOG_INFO_MESSAGE("Normalizing project MRU list to canonical (absolute) paths");
   return writeCollectionToFile<std::list<std::string>>(
      mruPath, normalized, stringifyString);
}

void onListsFileChanged(const core::system::FileChangeEvent& fileChange)
{
   // ignore if deleted
   if (fileChange.type() == core::system::FileChangeEvent::FileRemoved)
      return;

   // ignore if it is the lists directory
   if (fileChange.fileInfo().absolutePath() == s_listsPath.getAbsolutePath())
      return;

   // get the name of the list
   FilePath filePath(fileChange.fileInfo().absolutePath());
   std::string name = filePath.getFilename();

   // ignore changes to the legacy project_mru file; we write to it whenever project_name_mru is 
   // written, but never read it; it's kept updated so user doesn't lose their Projects MRU if
   // they downgrade to an older version of RStudio
   if (name == kProjectMru)
      return;

   // when the project_name_mru file is changed, we also update the legacy project_mru file
   if (name == kProjectNameMru)
   {
      Error error = syncLegacyProjectMru();
      if (error)
         LOG_ERROR(error);
   }

   // read it
   boost::shared_ptr<MruList> list;
   Error error = readList(name, &list);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Object eventJson;
   eventJson["name"] = name;
   eventJson["list"] = listContentsToJson(name, list->contents());

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
                             boost::shared_ptr<MruList>* pList)
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
   boost::shared_ptr<MruList> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;

   pResponse->setResult(listContentsToJson(name, list->contents()));

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

   bool isProjectMru = (name == kProjectNameMru);

   std::list<std::string> list;
   for (const json::Value& val : jsonList)
   {
      if (!json::isType<std::string>(val))
      {
         BOOST_ASSERT(false);
         continue;
      }

      std::string entry = val.getString();
      if (isProjectMru)
         entry = canonicalizeProjectMruEntry(entry);
      list.push_back(entry);
   }

   return writeCollectionToFile<std::list<std::string> >(listPath(name), list, stringifyString);
}

Error listInsertItem(bool prepend,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // get params and other context
   std::string name, value;
   boost::shared_ptr<MruList> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;
   error = json::readParam(request.params, 1, &value);
   if (error)
      return error;

   // project paths are stored canonically (absolute) so that aliased and
   // non-aliased forms of the same path don't produce duplicate entries
   if (name == kProjectNameMru)
      value = canonicalizeProjectMruEntry(value);

   // do the insert
   if (prepend)
      list->prepend(value);
   else
      list->append(value);

   return Success();
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

/**
 * Update the extra data on a list entry without changing its list position.
 */
Error listUpdateItemExtraData(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   std::string name, value;
   boost::shared_ptr<MruList> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;
   error = json::readParam(request.params, 1, &value);
   if (error)
      return error;

   if (name == kProjectNameMru)
      value = canonicalizeProjectMruEntry(value);

   list->updateExtraData(value);

   return Success();
}


Error listRemoveItem(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // get list name and contents
   std::string name;
   boost::shared_ptr<MruList> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;

   // get value to remove
   std::string value;
   error = json::readParam(request.params, 1, &value);

   if (name == kProjectNameMru)
      value = canonicalizeProjectMruEntry(value);

   // remove it
   list->remove(value);

   return Success();
}

Error clearListByName(const std::string& listName)
{
   if (!isListNameValid(listName))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   boost::shared_ptr<MruList> list;
   Error error = readList(listName, &list);
   if (error)
      return error;
   list->clear();

   return Success();
}

Error listClear(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // which list
   std::string name;
   boost::shared_ptr<MruList> list;
   Error error = getListNameAndContents(request, &name, &list);
   if (error)
      return error;

   // clear list
   list->clear();

   // when clearing the project_name_mru, also clear the legacy project_mru
   if (name == kProjectNameMru)
   {
      error = clearListByName(kProjectMru);
      if (error)
         return error;
   }
   return Success();
}

} // anonymous namespace


json::Object allListsAsJson()
{
   json::Object allListsJson;
   for (Lists::const_iterator it = s_lists.begin(); it != s_lists.end(); ++it)
   {
      boost::shared_ptr<MruList> list;
      Error error = readList(it->first, &list);
      if (error)
         LOG_ERROR(error);

      allListsJson[it->first] = listContentsToJson(it->first, list->contents());
   }

   return allListsJson;
}

Error initialize()
{  
   // register lists / max sizes
   s_lists[kFileMru] = 15;
   s_lists[kProjectMru] = 15; // legacy, kept in sync with kProjectNameMru
   s_lists[kProjectNameMru] = 15;
   s_lists[kHelpHistory] = 15;
   s_lists[kPlotPublishMru] = 15;
   s_lists[kCommandPaletteMru] = 10;
   s_lists[kUserDictionary] = 10000;

   // monitor the lists directory
   s_listsPath = module_context::registerMonitoredUserScratchDir(
                                                      kListsPath,
                                                      onListsFileChanged);

   Error error = migrateLegacyProjectMru();
   if (error)
      return error;

   error = normalizeProjectMru();
   if (error)
      LOG_ERROR(error);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "list_get", listGet))
      (bind(registerRpcMethod, "list_set_contents", listSetContents))
      (bind(registerRpcMethod, "list_prepend_item", listPrependItem))
      (bind(registerRpcMethod, "list_append_item", listAppendItem))
      (bind(registerRpcMethod, "list_remove_item", listRemoveItem))
      (bind(registerRpcMethod, "list_update_extra", listUpdateItemExtraData))
      (bind(registerRpcMethod, "list_clear", listClear));
   return initBlock.execute();
}
   


} // namespace lists
} // namespace modules
} // namespace session
} // namespace rstudio

