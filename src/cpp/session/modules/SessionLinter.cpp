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

ParseResults parseAndLintRFile(const FilePath& filePath)
{
   std::string contents = file_utils::readFile(filePath);
   if (contents.empty() || contents.find_first_not_of(" \n\t\v") == std::string::npos)
      return ParseResults();
   
   ParseResults results = parse(contents);
   ParseNode* pRoot = results.parseTree();
   
   // Augment the parse results.
   //
   // Now that we have parsed through the whole document and
   // built a parse / lint tree, we now walk through each identifier and
   // ensure that they exist either on the search path, or a parent
   // closure.
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

SEXP rs_parseAndLintRFile(SEXP pathSEXP)
{
   std::string path = r::sexp::asString(pathSEXP);
   FilePath filePath(module_context::resolveAliasedPath(path));
   
   ParseResults parsed = parseAndLintRFile(filePath);
   const std::vector<LintItem>& lintItems = parsed.lint().get();
   
   using namespace rstudio::r::sexp;
   Protect protect;
   ListBuilder result(&protect);
   for (std::size_t i = 0; i < lintItems.size(); ++i)
   {
      const LintItem& item = lintItems[i];
      
      ListBuilder lintList(&protect);
      
      // NOTE: Convert from C++ to R indexing
      lintList.add("start.row", item.startRow + 1);
      lintList.add("end.row", item.endRow + 1);
      lintList.add("start.column", item.startColumn + 1);
      lintList.add("end.column", item.endColumn + 1);
      lintList.add("type", lintTypeToString(item.type));
      lintList.add("message", item.message);
      
      result.add(static_cast<SEXP>(lintList));
   }
   
   return result;
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

} // anonymous namespace

void showGutterMarkers(const LintItems& items)
{
   json::Array json = lintAsJson(items);
   ClientEvent event(client_events::kUpdateGutterMarkers, json);
   module_context::enqueClientEvent(event);
}

namespace {

Error lint(const json::JsonRpcRequest& request,
           json::JsonRpcResponse* pResponse)
{
   std::string docPath;
   Error error = json::readParams(request.params, &docPath);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   FilePath filePath(module_context::resolveAliasedPath(docPath));
   
   // TODO: No file exists?
   if (!filePath.exists())
      return Success();
   
   ParseResults results = parseAndLintRFile(filePath);
   pResponse->setResult(lintAsJson(results.lint()));
   
   return Success();
   
}

SEXP rs_showGutterMarkers(SEXP filePathSEXP)
{
   FilePath path = FilePath(r::sexp::asString(filePathSEXP));
   std::string contents = file_utils::readFile(path);
   ParseResults results = parse(contents);
   showGutterMarkers(results.lint());
   return R_NilValue;
}

} // anonymous namespace

core::Error initialize()
{
   // on client init, schedule incremental work
   // onDocUpdated, schedule work
   // stateful object: shared_ptr<> 
   using namespace rstudio::core;
   using boost::bind;
   using namespace module_context;
   
   RS_REGISTER_CALL_METHOD(rs_parseAndLintRFile, 1);
   RS_REGISTER_CALL_METHOD(rs_showGutterMarkers, 0);
   
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
