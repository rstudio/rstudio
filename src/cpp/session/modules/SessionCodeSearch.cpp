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
   {
   }

   virtual ~SourceFileIndex()
   {
   }

   // COPYING: prohibited

   template <typename ForwardIterator>
   void enqueFiles(ForwardIterator begin, ForwardIterator end)
   {
      // note whether we had anything in the queue before we start
      // (if we don't then we'll schedule indexing after the add)
      bool schedule = indexingQueue_.empty();

      // enque change events (but don't schedule indexing)
      using namespace core::system;
      for ( ; begin != end; ++begin)
         enqueFileChange(FileChangeEvent(FileChangeEvent::FileAdded, *begin), false);

      // schedule indexing if necessary
      if (schedule)
         scheduleIndexing();
   }

   void enqueFileChange(const core::system::FileChangeEvent& event, bool schedule)
   {
      // screen out files which aren't R source files
      FilePath filePath(event.fileInfo().absolutePath());
      if (filePath.isDirectory() || filePath.extensionLowerCase() != ".r")
         return;

      // note whether we need to schedule after we are done
      schedule = schedule && indexingQueue_.empty();

      // add to the queue
      indexingQueue_.push(event);

      // schedule indexing if necessary
      if (schedule)
         scheduleIndexing();
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

   void scheduleIndexing()
   {
      // schedule indexing -- perform up to 300ms of work immediately and then continue
      // in periodic 100ms chunks until we are completed. note also that we accept the
      // default behavior of only indexing during idle time so as not to interfere
      // with running computations
      module_context::scheduleIncrementalWork(
                        boost::posix_time::milliseconds(300),
                        boost::posix_time::milliseconds(100),
                        boost::bind(&SourceFileIndex::dequeAndIndex, this));
   }

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
         {
            addIndexEntry(fileInfo);
            break;
         }

         case FileChangeEvent::FileModified:
         {
            removeIndexEntry(fileInfo);
            addIndexEntry(fileInfo);
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
      return !indexingQueue_.empty();
   }

   void addIndexEntry(const FileInfo& fileInfo)
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

      // add the entry
      entries_.insert(Entry(fileInfo, pIndex));
   }

   void removeIndexEntry(const FileInfo& fileInfo)
   {
      for (std::set<Entry>::iterator it = entries_.begin(); it != entries_.end(); ++it)
      {
         if (core::fileInfoPathCompare(it->fileInfo, fileInfo) == 0)
         {
            entries_.erase(it);
            break;
         }
      }
   }

private:
   // index entries
   std::set<Entry> entries_;

   // indexing queue
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

void onFileMonitorRegistered(const tree<core::FileInfo>& files)
{
   s_projectIndex.enqueFiles(files.begin_leaf(), files.end_leaf());
}

void onFileMonitorRegistrationError(const core::Error& error)
{
   // TODO: disable code searching
}

void onFileMonitorUnregistered()
{
   // TODO: disable code searching
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   // index all of the changes
   std::for_each(
         events.begin(),
         events.end(),
         boost::bind(&SourceFileIndex::enqueFileChange, &s_projectIndex, _1, true));
}

   
} // anonymous namespace


bool enabled()
{
   return projects::projectContext().hasProject();
}
   
Error initialize()
{
   // subscribe to project context file monitoring state changes
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(onFileMonitorRegistered, _2);
   cb.onRegistrationError = onFileMonitorRegistrationError;
   cb.onUnregistered = boost::bind(onFileMonitorUnregistered);
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().registerFileMonitorCallbacks(cb);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "search_code", searchCode));
   ;

   return initBlock.execute();
}


} // namespace agreement
} // namespace modules
} // namespace session
