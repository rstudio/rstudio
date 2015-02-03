/*
 * SessionLinter.cpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#include "SessionLinter.hpp"

#include <set>

#include <core/Exec.hpp>
#include <core/Error.hpp>

#include <session/SessionModuleContext.hpp>
#include "SessionCodeSearch.hpp"

#include <boost/shared_ptr.hpp>
#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/FileUtils.hpp>
#include <core/r_util/RTokenizer.hpp>
#include <core/r_util/RParser.hpp>
#include <core/r_util/RSourceIndex.hpp>
#include <core/FileUtils.hpp>
#include <core/collection/Tree.hpp>
#include <core/collection/Stack.hpp>


#include <core/Macros.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;
using namespace core::collection;

namespace {

std::vector<std::string> makeNSEFunctions()
{
   std::vector<std::string> s;
   
   s.push_back("library");
   s.push_back("require");
   s.push_back("quote");
   s.push_back("substitute");
   s.push_back("enquote");
   s.push_back("expression");
   s.push_back("evalq");
   s.push_back("subset");
   
   return s;
}

static std::vector<std::string> s_nseFunctions =
      makeNSEFunctions();

} // anonymous namespace

namespace {

void addUnreferencedSymbol(const ParseItem& item,
                           LintItems& lint)
{
   const ParseNode* pNode = item.pNode;
   if (!pNode)
      return;
   
   // Attempt to find a similarly named candidate in scope
   std::string candidate = pNode->suggestSimilarSymbolFor(item);
   lint.noSymbolNamed(item, candidate);
   
   // Check to see if there is a symbol in that node of
   // the parse tree (but defined later)
   ParseNode::SymbolPositions& symbols =
         const_cast<ParseNode::SymbolPositions&>(pNode->getDefinedSymbols());
   
   if (symbols.count(item.symbol))
   {
      ParseNode::Positions positions = symbols[item.symbol];
      BOOST_FOREACH(const Position& position, positions)
      {
         lint.symbolDefinedAfterUsage(item, position);
      }
   }
}

} // end anonymous namespace

ParseResults parse(const std::string& rCode)
{
   ParseResults results = r_util::parse(rCode);
   ParseNode* pRoot = results.parseTree();
   
   std::vector<ParseItem> unresolvedItems;
   pRoot->findAllUnresolvedSymbols(&unresolvedItems);
   
   // Finally, prune the set of lintItems -- we exclude any symbols
   // that were on the search path.
   std::set<std::string> objects;
   Error error = r::exec::RFunction(".rs.availableRSymbols").call(&objects);
   if (error)
      LOG_ERROR(error);
   else
   {
      BOOST_FOREACH(const ParseItem& item, unresolvedItems)
      {
         if (objects.count(item.symbol) == 0)
         {
            addUnreferencedSymbol(item, results.lint());
         }
      }
   }
   
   return results;
}

namespace {

json::Array lintAsJson(const LintItems& items)
{
   json::Array jsonArray;
   jsonArray.reserve(items.size());
   
   BOOST_FOREACH(const LintItem& item, items)
   {
      json::Object jsonObject;
      
      jsonObject["start.row"] = item.startRow;
      jsonObject["end.row"] = item.endRow;
      jsonObject["start.column"] = item.startColumn;
      jsonObject["end.column"] = item.endColumn;
      jsonObject["text"] = item.message;
      jsonObject["raw"] = item.message;
      jsonObject["type"] = lintTypeToString(item.type);
      
      jsonArray.push_back(jsonObject);
      
   }
   return jsonArray;
}

void emitLintEvent(const std::string& documentId,
                   const LintItems& lint)
{
   json::Object eventDataJson;
   
   eventDataJson["lint"] = lintAsJson(lint);
   eventDataJson["document_id"] = documentId;
   
   ClientEvent event(client_events::kUpdateGutterMarkers, eventDataJson);
   module_context::enqueClientEvent(event);
}

Error lint(const json::JsonRpcRequest& request,
           json::JsonRpcResponse* pResponse)
{
   std::string documentId;
   Error error = json::readParams(request.params, &documentId);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Try to get the contents from the database
   boost::shared_ptr<source_database::SourceDocument> pDoc;
   error = source_database::get(documentId, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // TODO: Handle R code in multi-mode documents
   if (!pDoc->canContainRCode())
      return Success();
   
   ParseResults results = parse(pDoc->contents());
   pResponse->setResult(lintAsJson(results.lint()));
   
   return Success();
   
}

void lintFileAndUpdateGutter(const std::string& documentId,
                             const std::string& contents)
{
   ParseResults results = parse(contents);
   emitLintEvent(documentId, results.lint());
}

typedef std::map<std::string, std::string> IdToFile;
void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
   std::string absolutePath = filePath.absolutePath();

   // verify it's a lintable R file.
   // TODO: linting of '.Rmd. and so on?
   
   // schedule work
   module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(3000),
            boost::bind(lintFileAndUpdateGutter, pDoc->id(), pDoc->contents()),
            true); // require idle
}

} // anonymous namespace

core::Error initialize()
{
   // on client init, schedule incremental work
   // onDocUpdated, schedule work
   source_database::events().onDocUpdated.connect(
             boost::bind(onSourceDocUpdated, _1));
//    source_database::events().onDocRemoved.connect(
//                 boost::bind(onSourceDocRemoved, pIdToFile, _1));
//       source_database::events().onRemoveAll.connect(
//                 boost::bind(onAllSourceDocsRemoved, pIdToFile));

   
   // stateful object: shared_ptr<> 
   using namespace rstudio::core;
   using boost::bind;
   using namespace module_context;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionLinter.R"))
         (bind(registerRpcMethod, "lint", lint));

   return initBlock.execute();

}

} // end namespace linter
} // end namespace modules
} // end namespace session
} // end namespace rstudio
