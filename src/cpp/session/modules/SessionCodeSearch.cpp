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

#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RSourceIndex.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace code_search {

namespace {
   
std::vector<boost::shared_ptr<core::r_util::RSourceIndex> > s_sourceIndexes;

void indexProjectFile(const FilePath& rootDir, const FilePath& filePath)
{
   if (filePath.extensionLowerCase() == ".r")
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


void findFunctions(const std::string& term,
                   int maxResults,
                   bool prefixOnly,
                   std::vector<r_util::RFunctionInfo>* pFunctions)
{
   BOOST_FOREACH(boost::shared_ptr<core::r_util::RSourceIndex> index,
                 s_sourceIndexes)
   {
      // scan the next index
      index->findFunction(term,
                          prefixOnly,
                          false,
                          std::back_inserter(*pFunctions));

      // return if we are past maxResults
      if (pFunctions->size() >= boost::numeric_cast<std::size_t>(maxResults))
      {
         pFunctions->resize(maxResults);
         return;
      }
   }
}


json::Object codeSearchResult(const r_util::RFunctionInfo& functionInfo)
{
   json::Object result;
   result["name"] = functionInfo.name();
   result["context"] = functionInfo.context();
   try
   {
      result["line"] = boost::numeric_cast<int>(functionInfo.line());
      result["column"] = boost::numeric_cast<int>(functionInfo.column());
   }
   catch (const boost::bad_numeric_cast& e)
   {
      result["line"] = 1;
      result["column"] = 1;
   }

   return result;
}

Error searchCode(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string term;
   int maxResults;
   Error error = json::readParams(request.params, &term, &maxResults);
   if (error)
      return error;

   std::vector<r_util::RFunctionInfo> functions;
   findFunctions(term, maxResults + 1, true, &functions);

   json::Array results;
   std::transform(functions.begin(),
                  functions.end(),
                  std::back_inserter(results),
                  codeSearchResult);

   pResponse->setResult(results);

   return Success();
}


void onDeferredInit()
{
   // initialize source index
   if (userSettings().indexingEnabled())
      indexProjectFiles();
}

   
} // anonymous namespace


   
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
