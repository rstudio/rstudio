/*
 * SessionCodeSearch.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <boost/regex.hpp>
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

#include <r/RExec.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionSource.hpp"

using namespace core ;

namespace session {  
namespace modules {
namespace code_search {

namespace {

bool isGlobalFunctionNamed(const r_util::RSourceItem& sourceItem,
                           const std::string& name)
{
   return sourceItem.braceLevel() == 0 &&
          (sourceItem.type() == r_util::RSourceItem::Function ||
           sourceItem.type() == r_util::RSourceItem::Method) &&
          sourceItem.name() == name;
}

boost::regex regexFromTerm(const std::string& term)
{
   // create wildcard pattern if the search has a '*'
   bool hasWildcard = term.find('*') != std::string::npos;
   boost::regex pattern;
   if (hasWildcard)
      pattern = regex_utils::wildcardPatternToRegex(term);
   return pattern;
}

// return if we are past max results
bool enforceMaxResults(std::size_t maxResults,
                        json::Array* pNames,
                        json::Array* pPaths,
                        bool* pMoreAvailable)
{
   if (pNames->size() > maxResults)
   {
      *pMoreAvailable = true;
      pNames->resize(maxResults);
      pPaths->resize(maxResults);
      return true;
   }
   else
   {
      return false;
   }
}


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
      // add all source files to the indexing queue
      using namespace core::system;
      for ( ; begin != end; ++begin)
      {
         if (isSourceFile(*begin))
         {
            FileChangeEvent addEvent(FileChangeEvent::FileAdded, *begin);
            indexingQueue_.push(addEvent);
         }
      }

      // schedule indexing if necessary. perform up to 200ms of work
      // immediately and then continue in periodic 20ms chunks until
      // we are completed.
      if (!indexingQueue_.empty() && !indexing_)
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
      // screen out files which aren't source files
      if (!isSourceFile(event.fileInfo()))
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

   bool findGlobalFunction(const std::string& functionName,
                           const std::set<std::string>& excludeContexts,
                           r_util::RSourceItem* pFunctionItem)
   {
      std::vector<r_util::RSourceItem> sourceItems;
      BOOST_FOREACH(const Entry& entry, entries_)
      {
         // bail if there is no index
         if (!entry.hasIndex())
            continue;

         // bail if this is an exluded context
         if (excludeContexts.find(entry.pIndex->context()) !=
             excludeContexts.end())
         {
            continue;
         }

         // scan the next index
         sourceItems.clear();
         entry.pIndex->search(
                  boost::bind(isGlobalFunctionNamed, _1, functionName),
                  std::back_inserter(sourceItems));

         // return if we got a hit
         if (sourceItems.size() > 0)
         {
            *pFunctionItem = sourceItems[0];
            return true;
         }
      }

      // none found
      return false;
   }

   void searchSource(const std::string& term,
                     std::size_t maxResults,
                     bool prefixOnly,
                     const std::set<std::string>& excludeContexts,
                     std::vector<r_util::RSourceItem>* pItems)
   {
      BOOST_FOREACH(const Entry& entry, entries_)
      {
         // skip if it has no index
         if (!entry.hasIndex())
            continue;

         // bail if this is an exluded context
         if (excludeContexts.find(entry.pIndex->context()) !=
             excludeContexts.end())
         {
            continue;
         }

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
      boost::regex pattern = regexFromTerm(term);

      // iterate over the files
      BOOST_FOREACH(const Entry& entry, entries_)
      {
         // get file and name
         FilePath filePath(entry.fileInfo.absolutePath());
         std::string name = filePath.filename();

         // compare for match (wildcard or standard)
         bool matches = false;
         if (!pattern.empty())
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
            // name and aliased path
            pNames->push_back(filePath.filename());
            pPaths->push_back(module_context::createAliasedPath(filePath));

            // return if we are past max results
            if (enforceMaxResults(maxResults, pNames, pPaths, pMoreAvailable))
               return;
         }
      }
   }

   void clear()
   {
      indexing_ = false;
      indexingQueue_ = std::queue<core::system::FileChangeEvent>();
      entries_.clear();
   }

private:

   // index entries we are managing
   struct Entry
   {
      explicit Entry(const FileInfo& fileInfo)
         : fileInfo(fileInfo)
      {
      }

      Entry(const FileInfo& fileInfo,
            boost::shared_ptr<core::r_util::RSourceIndex> pIndex)
         : fileInfo(fileInfo), pIndex(pIndex)
      {
      }

      FileInfo fileInfo;

      boost::shared_ptr<core::r_util::RSourceIndex> pIndex;

      bool hasIndex() const { return pIndex.get() != NULL; }

      bool operator < (const Entry& other) const
      {
         return core::fileInfoPathLessThan(fileInfo, other.fileInfo);
      }
   };

private:

   bool dequeAndIndex()
   {
      using namespace core::system;

      if (!indexingQueue_.empty())
      {
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
      }

      // return status
      indexing_ = !indexingQueue_.empty();
      return indexing_;
   }

   void updateIndexEntry(const FileInfo& fileInfo)
   {
      // index the source if necessary
      boost::shared_ptr<r_util::RSourceIndex> pIndex;
      if (isIndexableSourceFile(fileInfo))
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

         // add index entry
         std::string context = module_context::createAliasedPath(filePath);
         pIndex.reset(new r_util::RSourceIndex(context, code));
      }

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

   static bool isSourceFile(const FileInfo& fileInfo)
   {
      FilePath filePath(fileInfo.absolutePath());
      std::string ext = filePath.extensionLowerCase();
      return !filePath.isDirectory() &&
              (ext == ".r" || ext == ".rnw" ||
               ext == ".rmd" || ext == ".rmarkdown" ||
               ext == ".rhtml" || ext == ".rd");
   }

   static bool isIndexableSourceFile(const FileInfo& fileInfo)
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


// if we have a project active then restrict results to the project
bool sourceDatabaseFilter(const r_util::RSourceIndex& index)
{
   if (projects::projectContext().hasProject())
   {
      // get file path
      FilePath docPath = module_context::resolveAliasedPath(index.context());
      return docPath.isWithin(projects::projectContext().directory());
   }
   else
   {
      return true;
   }
}

bool findGlobalFunctionInSourceDatabase(
                        const std::string& functionName,
                        r_util::RSourceItem* pFunctionItem,
                        std::set<std::string>* pContextsSearched)
{
   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > rIndexes =
                                             modules::source::rIndexes();

   std::vector<r_util::RSourceItem> sourceItems;
   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, rIndexes)
   {
      // apply the filter
      if (!sourceDatabaseFilter(*pIndex))
         continue;

      // record this context
      pContextsSearched->insert(pIndex->context());

      // scan the next index
      sourceItems.clear();
      pIndex->search(
               boost::bind(isGlobalFunctionNamed, _1, functionName),
               std::back_inserter(sourceItems));

      // return if we got a hit
      if (sourceItems.size() > 0)
      {
         *pFunctionItem = sourceItems[0];
         return true;
      }
   }

   // none found
   return false;
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
      // apply the filter
      if (!sourceDatabaseFilter(*pIndex))
         continue;

      // record this context
      pContextsSearched->insert(pIndex->context());

      // scan the source index
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

void searchSourceDatabaseFiles(const std::string& term,
                               std::size_t maxResults,
                               json::Array* pNames,
                               json::Array* pPaths,
                               bool* pMoreAvailable)
{
   // default to no more available
   *pMoreAvailable = false;

   // create wildcard pattern if the search has a '*'
   boost::regex pattern = regexFromTerm(term);

   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > rIndexes =
                                             modules::source::rIndexes();

   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, rIndexes)
   {
      // bail if there is no path
      std::string context = pIndex->context();
      if (context.empty())
         continue;

      // get filename
      FilePath filePath = module_context::resolveAliasedPath(context);
      std::string filename = filePath.filename();

      // compare for match (wildcard or standard)
      bool matches = false;
      if (!pattern.empty())
      {
         matches = regex_utils::textMatches(filename,
                                            pattern,
                                            true,
                                            false);
      }
      else
      {
         matches = boost::algorithm::istarts_with(filename, term);
      }

      // add the file if we found a match
      if (matches)
      {
         // name and aliased path
         pNames->push_back(filename);
         pPaths->push_back(pIndex->context());

         // return if we are past max results
         if (enforceMaxResults(maxResults, pNames, pPaths, pMoreAvailable))
            return;
      }

   }
}

void searchFiles(const std::string& term,
                 std::size_t maxResults,
                 json::Array* pNames,
                 json::Array* pPaths,
                 bool* pMoreAvailable)
{
   // if we have a file monitor then search the project index
   if (session::projects::projectContext().hasFileMonitor())
   {
      s_projectIndex.searchFiles(term,
                                 maxResults,
                                 true,
                                 pNames,
                                 pPaths,
                                 pMoreAvailable);
   }
   else
   {
      searchSourceDatabaseFiles(term,
                                maxResults,
                                pNames,
                                pPaths,
                                pMoreAvailable);
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


json::Array signatureToJson(const std::vector<r_util::RS4MethodParam>& sig)
{
   json::Array sigJson;
   BOOST_FOREACH(const r_util::RS4MethodParam& param, sig)
   {
      json::Object paramJson;
      paramJson["name"] = param.name();
      paramJson["type"] = param.type();
      sigJson.push_back(paramJson);
   }
   return sigJson;
}

json::Array signaturesToJsonArray(
                        const std::vector<r_util::RSourceItem> &items)
{
   json::Array sigCol;
   std::transform(items.begin(),
                  items.end(),
                  std::back_inserter(sigCol),
                  boost::bind(
                        signatureToJson,
                        boost::bind(&r_util::RSourceItem::signature, _1)));
   return sigCol;
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
   int maxResultsInt = 20;
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
   searchFiles(term, maxResults, &names, &paths, &moreFilesAvailable);
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
   src["signature"] = signaturesToJsonArray(items);
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


bool namespaceIsPackage(const std::string& namespaceName,
                        std::string* pPackage)
{
   if (namespaceName.empty())
      return false;

   std::string pkgPrefix("package:");
   std::string::size_type pkgPrefixPos = namespaceName.find(pkgPrefix);
   if (pkgPrefixPos == 0 && namespaceName.length() > pkgPrefix.length())
   {
      *pPackage = namespaceName.substr(pkgPrefix.length());
      return true;
   }
   else
   {
      return false;
   }
}

bool findFunction(const std::string& name,
                  const std::string& fromWhere,
                  std::string* pNamespaceName)
{
   // if fromWhere is a package name then we should first directly
   // search that package (so that we can find hidden functions)
   Error error;
   std::string pkgName;
   pNamespaceName->clear();
   if (namespaceIsPackage(fromWhere, &pkgName))
   {
      r::sexp::Protect rProtect;
      SEXP functionSEXP = R_NilValue;
      r::exec::RFunction func(".rs.getPackageFunction", name, pkgName);
      error = func.call(&functionSEXP, &rProtect);
      if (!error && !r::sexp::isNull(functionSEXP))
         *pNamespaceName = fromWhere;
   }

   // if we haven't found it yet
   if (pNamespaceName->empty())
   {
      r::exec::RFunction func(".rs.findFunctionNamespace",
                              name,
                              fromWhere);
      error = func.call(pNamespaceName);
   }

   // log error and return appropriate status
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else if (pNamespaceName->empty())
   {
      return false;
   }
   else
   {
      return true;
   }
}


void getFunctionSource(SEXP functionSEXP,
                       std::vector<std::string>* pLines,
                       bool* pFromSrcAttrib)
{
   // check if the function has a "srcref" attribute
   *pFromSrcAttrib = false;
   r::exec::RFunction getSrcRefFunc(".rs.functionHasSrcRef", functionSEXP);
   Error error = getSrcRefFunc.call(pFromSrcAttrib);
   if (error)
      LOG_ERROR(error);

   // deparse
   r::exec::RFunction deparseFunc(".rs.deparseFunction");
   deparseFunc.addParam(functionSEXP);
   deparseFunc.addParam(*pFromSrcAttrib);
   error = deparseFunc.call(pLines);
   if (error)
      LOG_ERROR(error);
}

void getFunctionS3Methods(const std::string& methodName, json::Array* pMethods)
{
   // lookup S3 methods for that base name
   std::vector<std::string> methods;
   r::exec::RFunction rfunc(".rs.getS3MethodsForFunction", methodName);
   Error error = rfunc.call(&methods);
   if (error)
      LOG_ERROR(error);

   // provide them to the caller
   std::transform(methods.begin(),
                  methods.end(),
                  std::back_inserter(*pMethods),
                  boost::bind(json::toJsonString, _1));

}


std::string trimToken(const std::string& token)
{
   return boost::algorithm::trim_copy(token);
}

class FunctionInfo
{
public:
   explicit FunctionInfo(const std::string& name)
      : name_(name)
   {
      boost::regex pattern("^([^{]+)\\{([^}]+)\\}$");
      boost::smatch match;
      if (boost::regex_search(name_, match, pattern))
      {
         // read method name
         methodName_ = match[1];
         boost::algorithm::trim(methodName_);

         // read , separated fields
         std::string types = match[2];
         using namespace boost ;
         char_separator<char> comma(",");
         tokenizer<char_separator<char> > typeTokens(types, comma);
         std::transform(typeTokens.begin(),
                        typeTokens.end(),
                        std::back_inserter(paramTypes_),
                        trimToken);
      }
   }

   bool isS4Method() const { return !methodName_.empty(); }

   const std::string& name() const { return name_; }
   const std::string& methodName() const { return methodName_; }
   const std::vector<std::string>& paramTypes() const { return paramTypes_; }

private:
   std::string name_;
   std::string methodName_;
   std::vector<std::string> paramTypes_;
};


void getFunctionS4Methods(const std::string& methodName, json::Array* pMethods)
{
   // check if the function isGeneric
   bool generic = false;
   if (methodName != "class")
   {
      Error error = r::exec::RFunction("methods:::isGeneric", methodName).call(
                                                                     &generic);
      if (error)
         LOG_ERROR(error);
   }

   if (generic)
   {
      std::vector<std::string> methods;
      r::exec::RFunction rFunc(".rs.getS4MethodsForFunction", methodName);
      Error error = rFunc.call(&methods);
      if (error)
         LOG_ERROR(error);

      // provide them to the caller
      std::transform(methods.begin(),
                     methods.end(),
                     std::back_inserter(*pMethods),
                     boost::bind(json::toJsonString, _1));
   }
}


json::Object createErrorFunctionDefinition(const std::string& name,
                                           const std::string& namespaceName)
{
   json::Object funDef;
   funDef["name"] = name;
   funDef["namespace"] = namespaceName;
   funDef["methods"] = json::Array();
   boost::format fmt("\n# ERROR: Defintion of function '%1%' not found\n"
                     "# in namespace '%2%'");
   funDef["code"] = boost::str(fmt % name % namespaceName);
   funDef["from_src_attrib"] = false;

   return funDef;
}

std::string baseMethodName(const std::string& name)
{
   // strip type qualifiers for S4 methods
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
   {
      return functionInfo.methodName();
   }
   // strip content after the '.' for S3 methods
   else
   {
      // first find the base name of the function
      std::string baseName = name;
      std::size_t periodLoc = baseName.find('.');
      if (periodLoc != std::string::npos && periodLoc > 0)
         baseName = baseName.substr(0, periodLoc);
      return baseName;
   }
}

json::Object createFunctionDefinition(const std::string& name,
                                      const std::string& namespaceName,
                                      SEXP functionSEXP)
{
   // basic metadata
   json::Object funDef;
   funDef["name"] = name;
   funDef["namespace"] = namespaceName;

   // function source code
   bool fromSrcAttrib = false;
   std::vector<std::string> lines;
   getFunctionSource(functionSEXP, &lines, &fromSrcAttrib);

   // did we get some lines back?
   if (lines.size() > 0)
   {
      // append the lines to the code and set it
      std::string code;
      BOOST_FOREACH(const std::string& line, lines)
      {
         code.append(line);
         code.append("\n");
      }
      funDef["code"] = code;
      funDef["from_src_attrib"] = fromSrcAttrib;

      // methods
      std::string methodName = baseMethodName(name);
      json::Array methodsJson;
      getFunctionS4Methods(methodName, &methodsJson);
      getFunctionS3Methods(methodName, &methodsJson);
      funDef["methods"] = methodsJson;

      return funDef;
   }
   else
   {
      return createErrorFunctionDefinition(name, namespaceName);
   }
}

Error getS4Method(const FunctionInfo& functionInfo,
                  std::string* pNamespaceName,
                  SEXP* pFunctionSEXP,
                  r::sexp::Protect* pProtect)
{
   // get the method
   r::exec::RFunction rFunc("methods:::getMethod");
   rFunc.addParam(functionInfo.methodName());
   rFunc.addParam(functionInfo.paramTypes());
   Error error = rFunc.call(pFunctionSEXP, pProtect);
   if (error)
      return error;

   // get the namespace
   r::exec::RFunction rNsFunc(".rs.getS4MethodNamespaceName", *pFunctionSEXP);
   return rNsFunc.call(pNamespaceName);
}

json::Object createFunctionDefinition(const std::string& name,
                                      const std::string& namespaceName)
{
   // stuff we are trying to find
   std::string functionNamespace = namespaceName;
   r::sexp::Protect rProtect;
   SEXP functionSEXP = R_NilValue;
   Error error;

   // what type of function are we looking for?
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
   {
      // check for S4 method definition
      error = getS4Method(functionInfo,
                          &functionNamespace,
                          &functionSEXP,
                          &rProtect);
   }
   else
   {
      // get the function -- if it within a package namespace then do special
      // handling to make sure we can find hidden functions as well
      std::string pkgName;
      if (namespaceIsPackage(functionNamespace, &pkgName))
      {
         r::exec::RFunction getFunc(".rs.getPackageFunction", name, pkgName);
         error = getFunc.call(&functionSEXP, &rProtect);
      }
      else
      {
         r::exec::RFunction getFunc(".rs.getFunction", name, functionNamespace);
         error = getFunc.call(&functionSEXP, &rProtect);
      }
   }

   // check find status and return appropriate definiton
   if (!error)
   {
      if (!r::sexp::isNull(functionSEXP))
         return createFunctionDefinition(name, functionNamespace, functionSEXP);
      else
         return createErrorFunctionDefinition(name, functionNamespace);
   }
   else
   {
      LOG_ERROR(error);
      return createErrorFunctionDefinition(name, functionNamespace);
   }

}

json::Value createS3MethodDefinition(const std::string& name)
{
   // first call getAnywhere to see if we can find a definition
   r::sexp::Protect rProtect;
   SEXP getAnywhereSEXP;
   r::exec::RFunction getAnywhereFunc("utils:::getAnywhere", name);
   Error error = getAnywhereFunc.call(&getAnywhereSEXP, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }

   // access the "where" element
   std::vector<std::string> whereList;
   error = r::sexp::getNamedListElement(getAnywhereSEXP, "where", &whereList);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }

   // find an element beginning with "package:" or "namespace:"
   std::string packagePrefix = "package:";
   std::string namespacePrefix = "namespace:";
   std::string namespaceName;
   BOOST_FOREACH(const std::string& where, whereList)
   {
      if (boost::algorithm::starts_with(where, packagePrefix))
      {
         namespaceName = where;
         break;
      }

      if (boost::algorithm::starts_with(where, namespacePrefix) &&
          (where.length() > namespacePrefix.length()))
      {
         namespaceName = "package:" +
                         where.substr(namespacePrefix.length());
         break;
      }
   }

   // if we found one then go through standard route, else return null
   if (!namespaceName.empty())
      return createFunctionDefinition(name, namespaceName);
   else
      return json::Value();
}


json::Value createS4MethodDefinition(const FunctionInfo& functionInfo)
{
   // lookup the method
   std::string functionNamespace ;
   r::sexp::Protect rProtect;
   SEXP functionSEXP = R_NilValue;
   Error error = getS4Method(functionInfo,
                             &functionNamespace,
                             &functionSEXP,
                             &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }
   else
   {
      return createFunctionDefinition(functionInfo.name(),
                                      functionNamespace,
                                      functionSEXP);
   }
}


struct FunctionToken
{
   std::string package;
   std::string name;
};


json::Object createFunctionDefinition(const FunctionToken& token)
{
   return createFunctionDefinition(token.name, "package:" + token.package);
}

Error guessFunctionToken(const std::string& line,
                         int pos,
                         FunctionToken* pToken)
{
   // call into R to determine the token
   std::string token;
   Error error = r::exec::RFunction(".rs.guessToken", line, pos).call(&token);
   if (error)
      return error;

   // see if it has a namespace qualifier
   boost::regex pattern("^([^:]+):{2,3}([^:]+)$");
   boost::smatch match;
   if (boost::regex_search(token, match, pattern))
   {
      pToken->package = match[1];
      pToken->name = match[2];
   }
   else
   {
      pToken->name = token;
   }

   return Success();
}

Error getFunctionDefinition(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // read params
   std::string line;
   int pos;
   Error error = json::readParams(request.params, &line, &pos);
   if (error)
      return error;

   // call into R to determine the token
   FunctionToken token;
   error = guessFunctionToken(line, pos, &token);
   if (error)
      return error;

   // default return value is null function name (indicating no results)
   json::Object defJson;
   defJson["function_name"] = json::Value();

   // if there was a package then we go straight to the search path
   if (!token.package.empty())
   {
      defJson["function_name"] = token.name;
      defJson["search_path_definition"] = createFunctionDefinition(token);
   }

   // if we got a name token then search the code
   else if (!token.name.empty())
   {
      // discovered a token so we have at least a function name to return
      defJson["function_name"] = token.name;

      // find in source database then in project index
      std::set<std::string> contexts;
      r_util::RSourceItem sourceItem;
      bool found =
         findGlobalFunctionInSourceDatabase(token.name, &sourceItem, &contexts) ||
         s_projectIndex.findGlobalFunction(token.name, contexts, &sourceItem);

      // found the file
      if (found)
      {
         // return full path to file
         FilePath srcFilePath = module_context::resolveAliasedPath(
                                                      sourceItem.context());
         defJson["file"] = module_context::createFileSystemItem(srcFilePath);

         // return location in file
         json::Object posJson;
         posJson["line"] = sourceItem.line();
         posJson["column"] = sourceItem.column();
         defJson["position"] = posJson;
      }
      // didn't find the file, check the search path
      else
      {
         // find the function
         std::string namespaceName;
         if (findFunction(token.name, "", &namespaceName))
         {
            defJson["search_path_definition"] =
                              createFunctionDefinition(token.name,
                                                       namespaceName);
         }
      }
   }

   // set result
   pResponse->setResult(defJson);

   return Success();
}


Error getSearchPathFunctionDefinition(const json::JsonRpcRequest& request,
                                      json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name;
   std::string namespaceName;
   Error error = json::readParams(request.params, &name, &namespaceName);
   if (error)
      return error;

   // return result
   pResponse->setResult(createFunctionDefinition(name, namespaceName));
   return Success();
}

Error getMethodDefinition(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name;
   Error error = json::readParam(request.params, 0, &name);
   if (error)
      return error;

   // return result (distinguish between S3 and S4 methods)
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
      pResponse->setResult(createS4MethodDefinition(functionInfo));
   else
      pResponse->setResult(createS3MethodDefinition(name));

   return Success();
}

Error findFunctionInSearchPath(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // read params
   std::string line;
   int pos;
   json::Value fromWhereJSON;
   Error error = json::readParams(request.params, &line, &pos, &fromWhereJSON);
   if (error)
      return error;

   // handle fromWhere NULL case
   std::string fromWhere = fromWhereJSON.is_null() ? "" :
                                                     fromWhereJSON.get_str();


   // call into R to determine the token
   FunctionToken token;
   error = guessFunctionToken(line, pos, &token);
   if (error)
      return error;

   // lookup the namespace if we need to
   std::string namespaceName;
   if (!token.package.empty())
       namespaceName = "package:" + token.package;
   else
      findFunction(token.name, fromWhere, &namespaceName);

   // return either just the name or the full function
   if (!namespaceName.empty())
   {
      pResponse->setResult(createFunctionDefinition(token.name,
                                                    namespaceName));
   }
   else
   {
      json::Object funDefName;
      funDefName["name"] = token.name;
      pResponse->setResult(funDefName);
   }

   return Success();
}

void onFileMonitorEnabled(const tree<core::FileInfo>& files)
{
   s_projectIndex.enqueFiles(files.begin_leaf(), files.end_leaf());
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
   // clear the index so we don't ever get stale results
   s_projectIndex.clear();
}

   
} // anonymous namespace
   
Error initialize()
{
   // subscribe to project context file monitoring state changes
   // (note that if there is no project this will no-op)
   session::projects::FileMonitorCallbacks cb;
   cb.onMonitoringEnabled = onFileMonitorEnabled;
   cb.onFilesChanged = onFilesChanged;
   cb.onMonitoringDisabled = onFileMonitorDisabled;
   projects::projectContext().subscribeToFileMonitor("R source file indexing",
                                                     cb);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "search_code", searchCode))
      (bind(registerRpcMethod, "get_function_definition", getFunctionDefinition))
      (bind(registerRpcMethod, "get_search_path_function_definition", getSearchPathFunctionDefinition))
      (bind(registerRpcMethod, "get_method_definition", getMethodDefinition))
      (bind(registerRpcMethod, "find_function_in_search_path", findFunctionInSearchPath));

   return initBlock.execute();
}


} // namespace code_search
} // namespace modules
} // namespace session
