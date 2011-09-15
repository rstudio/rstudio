/*
 * SessionCodeSearch.cpp
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

#include "SessionCodeSearch.hpp"

#include <iostream>
#include <vector>
#include <set>

#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <core/system/FileChangeEvent.hpp>
#include <core/system/FileMonitor.hpp>

#include <R_ext/rlocale.h>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionSource.hpp"


using namespace core ;

namespace session {  
namespace modules {
namespace code_search {

namespace {

class SourceFileIndex : boost::noncopyable
{
public:
   SourceFileIndex()
      : indexing_(false)
   {
   }

   virtual ~SourceFileIndex()
   {
   }

   // COPYING: prohibited

   template <typename ForwardIterator>
   void enqueFiles(ForwardIterator begin, ForwardIterator end)
   {
      // add all R source files to the indexing queue
      using namespace core::system;
      for ( ; begin != end; ++begin)
      {
         if (isRSourceFile(*begin))
         {
            FileChangeEvent addEvent(FileChangeEvent::FileAdded, *begin);
            indexingQueue_.push(addEvent);
         }
      }

      // schedule indexing if necessary. perform up to 200ms of work
      // immediately and then continue in periodic 20ms chunks until
      // we are completed.
      if (!indexing_)
      {
         indexing_ = true;

         module_context::scheduleIncrementalWork(
                           boost::posix_time::milliseconds(200),
                           boost::posix_time::milliseconds(20),
                           boost::bind(&SourceFileIndex::dequeAndIndex, this),
                           false /* allow indexing even when non-idle */);
      }
   }

   void enqueFileChange(const core::system::FileChangeEvent& event)
   {
      // screen out files which aren't R source files
      if (!isRSourceFile(event.fileInfo()))
         return;

      // add to the queue
      indexingQueue_.push(event);

      // schedule indexing if necessary. don't index anything immediately
      // (this is to defend against large numbers of files being enqued
      // at once and typing up the main thread). rather, schedule indexing
      // to occur during idle time in 20ms chunks
      if (!indexing_)
      {
         indexing_ = true;

         module_context::scheduleIncrementalWork(
                           boost::posix_time::milliseconds(20),
                           boost::bind(&SourceFileIndex::dequeAndIndex, this),
                           false /* allow indexing even when non-idle */);
      }
   }

   void searchSource(const std::string& term,
                     std::size_t maxResults,
                     bool prefixOnly,
                     const std::set<std::string>& excludeContexts,
                     std::vector<r_util::RSourceItem>* pItems)
   {
      BOOST_FOREACH(const Entry& entry, entries_)
      {
         // bail if this is an exluded context
         if (excludeContexts.find(entry.pIndex->context()) != excludeContexts.end())
            continue;

         // scan the next index
         entry.pIndex->search(term,
                              prefixOnly,
                              false,
                              std::back_inserter(*pItems));

         // return if we are past maxResults
         if (pItems->size() >= maxResults)
         {
            pItems->resize(maxResults);
            return;
         }
      }
   }

   void searchFiles(const std::string& term,
                    std::size_t maxResults,
                    bool prefixOnly,
                    json::Array* pNames,
                    json::Array* pPaths,
                    bool* pMoreAvailable)
   {
      // default to no more available
      *pMoreAvailable = false;

      // create wildcard pattern if the search has a '*'
      bool hasWildcard = term.find('*') != std::string::npos;
      boost::regex pattern;
      if (hasWildcard)
         pattern = regex_utils::wildcardPatternToRegex(term);

      // iterate over the files
      FilePath projectRoot = projects::projectContext().directory();
      BOOST_FOREACH(const Entry& entry, entries_)
      {
         // get the next file
         FilePath filePath(entry.fileInfo.absolutePath());

         // get name for comparison
         std::string name = filePath.filename();

         // compare for match (wildcard or standard)
         bool matches = false;
         if (hasWildcard)
         {
            matches = regex_utils::textMatches(name,
                                               pattern,
                                               prefixOnly,
                                               false);
         }
         else
         {
            if (prefixOnly)
               matches = boost::algorithm::istarts_with(name, term);
            else
               matches = boost::algorithm::icontains(name, term);
         }

         // add the file if we found a match
         if (matches)
         {
            // name and project relative directory
            pNames->push_back(filePath.filename());
            pPaths->push_back(filePath.relativePath(projectRoot));

            // return if we are past max results
            if (pNames->size() > maxResults)
            {
               *pMoreAvailable = true;
               pNames->resize(maxResults);
               pPaths->resize(maxResults);
               return;
            }
         }

      }
   }

private:

   // index entries we are managing
   struct Entry
   {
      Entry(const FileInfo& fileInfo,
            boost::shared_ptr<core::r_util::RSourceIndex> pIndex)
         : fileInfo(fileInfo), pIndex(pIndex)
      {
      }

      FileInfo fileInfo;

      boost::shared_ptr<core::r_util::RSourceIndex> pIndex;

      bool operator < (const Entry& other) const
      {
         return core::fileInfoPathLessThan(fileInfo, other.fileInfo);
      }
   };

private:

   bool dequeAndIndex()
   {
      using namespace core::system;

      // remove the event from the queue
      FileChangeEvent event = indexingQueue_.front();
      indexingQueue_.pop();

      // process the change
      const FileInfo& fileInfo = event.fileInfo();
      switch(event.type())
      {
         case FileChangeEvent::FileAdded:
         case FileChangeEvent::FileModified:
         {
            updateIndexEntry(fileInfo);
            break;
         }

         case FileChangeEvent::FileRemoved:
         {
            removeIndexEntry(fileInfo);
            break;
         }

         case FileChangeEvent::None:
            break;
      }

      // return status
      indexing_ = !indexingQueue_.empty();
      return indexing_;
   }

   void updateIndexEntry(const FileInfo& fileInfo)
   {
      // read the file
      FilePath filePath(fileInfo.absolutePath());
      std::string code;
      Error error = module_context::readAndDecodeFile(
                              filePath,
                              projects::projectContext().defaultEncoding(),
                              true,
                              &code);
      if (error)
      {
         error.addProperty("src-file", filePath.absolutePath());
         LOG_ERROR(error);
         return;
      }

      // compute project relative directory (used for context)
      std::string context = filePath.relativePath(projects::projectContext().directory());

      // index the source
      boost::shared_ptr<r_util::RSourceIndex> pIndex(
                                          new r_util::RSourceIndex(context, code));

      // attempt to add the entry
      Entry entry(fileInfo, pIndex);
      std::pair<std::set<Entry>::iterator,bool> result = entries_.insert(entry);

      // insert failed, remove then re-add
      if (result.second == false)
      {
         // was the first item, erase and re-insert without a hint
         if (result.first == entries_.begin())
         {
            entries_.erase(result.first);
            entries_.insert(entry);
         }
         // can derive a valid hint
         else
         {
            // derive hint iterator
            std::set<Entry>::iterator hintIter = result.first;
            hintIter--;

            // erase and re-insert with hint
            entries_.erase(result.first);
            entries_.insert(hintIter, entry);
         }
      }
   }

   void removeIndexEntry(const FileInfo& fileInfo)
   {
      // create a fake entry with a null source index to pass to find
      Entry entry(fileInfo, boost::shared_ptr<r_util::RSourceIndex>());

      // do the find (will use Entry::operator< for equivilance test)
      std::set<Entry>::iterator it = entries_.find(entry);
      if (it != entries_.end())
         entries_.erase(it);
   }

   static bool isRSourceFile(const FileInfo& fileInfo)
   {
      FilePath filePath(fileInfo.absolutePath());
      return !filePath.isDirectory() &&
              filePath.extensionLowerCase() == ".r";
   }

private:
   // index entries
   std::set<Entry> entries_;

   // indexing queue
   bool indexing_;
   std::queue<core::system::FileChangeEvent> indexingQueue_;
};

// global source file index
SourceFileIndex s_projectIndex;


void searchSourceDatabase(const std::string& term,
                          std::size_t maxResults,
                          bool prefixOnly,
                          std::vector<r_util::RSourceItem>* pItems,
                          std::set<std::string>* pContextsSearched)
{


   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > rIndexes =
                                             modules::source::rIndexes();

   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, rIndexes)
   {
      // get file path
      FilePath docPath = module_context::resolveAliasedPath(pIndex->context());

      // bail if the file isn't in the project
      std::string projRelativePath =
            docPath.relativePath(projects::projectContext().directory());
      if (projRelativePath.empty())
         continue;

      // record that we searched this path
      pContextsSearched->insert(projRelativePath);

      // scan the source index
      pIndex->search(term,
                     projRelativePath,
                     prefixOnly,
                     false,
                     std::back_inserter(*pItems));

      // return if we are past maxResults
      if (pItems->size() >= maxResults)
      {
         pItems->resize(maxResults);
         return;
      }
   }
}

void searchSource(const std::string& term,
                  std::size_t maxResults,
                  bool prefixOnly,
                  std::vector<r_util::RSourceItem>* pItems,
                  bool* pMoreAvailable)
{
   // default to no more available
   *pMoreAvailable = false;

   // first search the source database
   std::set<std::string> srcDBContexts;
   searchSourceDatabase(term, maxResults, prefixOnly, pItems, &srcDBContexts);

   // we are done if we had >= maxResults
   if (pItems->size() > maxResults)
   {
      *pMoreAvailable = true;
      pItems->resize(maxResults);
      return;
   }

   // compute project max results based on existing results
   std::size_t maxProjResults = maxResults - pItems->size();

   // now search the project (excluding contexts already searched in the source db)
   std::vector<r_util::RSourceItem> projItems;
   s_projectIndex.searchSource(term,
                               maxProjResults,
                               prefixOnly,
                               srcDBContexts,
                               &projItems);

   // add project items to the list
   BOOST_FOREACH(const r_util::RSourceItem& sourceItem, projItems)
   {
      // add the item
      pItems->push_back(sourceItem);

      // bail if we've hit the max
      if (pItems->size() > maxResults)
      {
         *pMoreAvailable = true;
         pItems->resize(maxResults);
         break;
      }
   }
}

template <typename TValue, typename TFunc>
json::Array toJsonArray(
      const std::vector<r_util::RSourceItem> &items,
      TFunc memberFunc)
{
   json::Array col;
   std::transform(items.begin(),
                  items.end(),
                  std::back_inserter(col),
                  boost::bind(json::toJsonValue<TValue>,
                                 boost::bind(memberFunc, _1)));
   return col;
}

bool compareItems(const r_util::RSourceItem& i1, const r_util::RSourceItem& i2)
{
   return i1.name() < i2.name();
}

Error searchCode(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // get params
   std::string term;
   int maxResultsInt;
   Error error = json::readParams(request.params, &term, &maxResultsInt);
   if (error)
      return error;
   std::size_t maxResults = safe_convert::numberTo<std::size_t>(maxResultsInt,
                                                                20);

   // object to return
   json::Object result;

   // search files
   json::Array names;
   json::Array paths;
   bool moreFilesAvailable = false;
   s_projectIndex.searchFiles(term,
                              maxResults,
                              true,
                              &names,
                              &paths,
                              &moreFilesAvailable);
   json::Object files;
   files["filename"] = names;
   files["path"] = paths;
   result["file_items"] = files;

   // search source (sort results by name)
   std::vector<r_util::RSourceItem> items;
   bool moreSourceItemsAvailable = false;
   searchSource(term, maxResults, true, &items, &moreSourceItemsAvailable);
   std::sort(items.begin(), items.end(), compareItems);

   // see if we need to do src truncation
   bool truncated = false;
   if ( (names.size() + items.size()) > maxResults )
   {
      // truncate source items
      std::size_t srcItems = maxResults - names.size();
      items.resize(srcItems);
      truncated = true;
   }

   // return rpc array list (wire efficiency)
   json::Object src;
   src["type"] = toJsonArray<int>(items, &r_util::RSourceItem::type);
   src["name"] = toJsonArray<std::string>(items, &r_util::RSourceItem::name);
   src["context"] = toJsonArray<std::string>(items, &r_util::RSourceItem::context);
   src["line"] = toJsonArray<int>(items, &r_util::RSourceItem::line);
   src["column"] = toJsonArray<int>(items, &r_util::RSourceItem::column);
   result["source_items"] = src;

   // set more available bit
   result["more_available"] =
         moreFilesAvailable || moreSourceItemsAvailable || truncated;

   pResponse->setResult(result);

   return Success();
}

void onFileMonitorEnabled(const tree<core::FileInfo>& files)
{
   s_projectIndex.enqueFiles(files.begin_leaf(), files.end_leaf());

   ClientEvent event(client_events::kCodeIndexingStatusChanged, true);
   module_context::enqueClientEvent(event);
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   std::for_each(
         events.begin(),
         events.end(),
         boost::bind(&SourceFileIndex::enqueFileChange, &s_projectIndex, _1));
}

void onFileMonitorDisabled()
{
   ClientEvent event(client_events::kCodeIndexingStatusChanged, false);
   module_context::enqueClientEvent(event);
}

   
} // anonymous namespace


bool enabled()
{
   return projects::projectContext().hasFileMonitor();
}
   
Error initialize()
{
   // subscribe to project context file monitoring state changes
   session::projects::FileMonitorCallbacks cb;
   cb.onMonitoringEnabled = onFileMonitorEnabled;
   cb.onFilesChanged = onFilesChanged;
   cb.onMonitoringDisabled = onFileMonitorDisabled;
   projects::projectContext().subscribeToFileMonitor("Code searching", cb);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "search_code", searchCode));
   ;

   return initBlock.execute();
}


} // namespace code_search
} // namespace modules
} // namespace session
