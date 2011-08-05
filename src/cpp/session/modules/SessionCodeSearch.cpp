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

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/IncrementalCommand.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionSource.hpp"

using namespace core ;

namespace session {  
namespace modules {
namespace code_search {

namespace {
   
std::vector<boost::shared_ptr<core::r_util::RSourceIndex> > s_sourceIndexes;

bool isRSourceFile(const FilePath& filePath)
{
   return filePath.extensionLowerCase() == ".r";
}

void indexProjectFile(const FilePath& rootDir, const FilePath& filePath)
{
   if (isRSourceFile(filePath))
   {
      // read the file (assumes utf8)
      std::string code;
      Error error = core::readStringFromFile(filePath,
                                             &code,
                                             string_utils::LineEndingPosix);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // compute project relative directory (used for context)
      std::string context = filePath.relativePath(rootDir);

      // index the source
      s_sourceIndexes.push_back(boost::shared_ptr<r_util::RSourceIndex>(
                                    new r_util::RSourceIndex(context, code)));
   }
}

void indexProjectFiles()
{
   Error error = projects::projectContext().directory().childrenRecursive(
                     boost::bind(&indexProjectFile,
                        projects::projectContext().directory(),_2));
    if (error)
       LOG_ERROR(error);
}

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

void searchProject(const std::string& term,
                   std::size_t maxResults,
                   bool prefixOnly,
                   const std::set<std::string>& excludeContexts,
                   std::vector<r_util::RSourceItem>* pItems)
{
   BOOST_FOREACH(const boost::shared_ptr<core::r_util::RSourceIndex>& pIndex,
                 s_sourceIndexes)
   {
      // bail if this is an exluded context
      if (excludeContexts.find(pIndex->context()) != excludeContexts.end())
         continue;

      // scan the next index
      pIndex->search(term,
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

void search(const std::string& term,
            std::size_t maxResults,
            bool prefixOnly,
            std::vector<r_util::RSourceItem>* pItems)
{
   // first search the source database
   std::set<std::string> srcDBContexts;
   searchSourceDatabase(term, maxResults, prefixOnly, pItems, &srcDBContexts);

   // we are done if we had >= maxResults
   if (pItems->size() >= maxResults)
   {
      pItems->resize(maxResults);
      return;
   }

   // now search the project (excluding contexts already searched in the source db)
   std::vector<r_util::RSourceItem> projItems;
   searchProject(term, maxResults, prefixOnly, srcDBContexts, &projItems);

   // add project items to the list
   BOOST_FOREACH(const r_util::RSourceItem& sourceItem, projItems)
   {
      // add the item
      pItems->push_back(sourceItem);

      // bail if we've hit the max
      if (pItems->size() >= maxResults)
      {
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
   int maxResults;
   Error error = json::readParams(request.params, &term, &maxResults);
   if (error)
      return error;

   // find functions
   std::vector<r_util::RSourceItem> items;
   search(term, maxResults + 1, true, &items);

   // sort by name
   std::sort(items.begin(), items.end(), compareItems);

   // return rpc array list (wire efficiency)
   json::Object res;
   res["type"] = toJsonArray<int>(items, &r_util::RSourceItem::type);
   res["name"] = toJsonArray<std::string>(items, &r_util::RSourceItem::name);
   res["context"] = toJsonArray<std::string>(items, &r_util::RSourceItem::context);
   res["line"] = toJsonArray<int>(items, &r_util::RSourceItem::line);
   res["column"] = toJsonArray<int>(items, &r_util::RSourceItem::column);

   pResponse->setResult(res);

   return Success();
}

void onDeferredInit()
{
   // initialize source index
   if (code_search::enabled())
      indexProjectFiles();
}
   
} // anonymous namespace


bool enabled()
{
   return projects::projectContext().hasProject() &&
          userSettings().indexingEnabled();
}
   
Error initialize()
{
   // sign up for deferred init
   module_context::events().onDeferredInit.connect(onDeferredInit);


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
