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

// #define RSTUDIO_ENABLE_PROFILING
// #define RSTUDIO_ENABLE_DEBUG_MACROS
#define RSTUDIO_DEBUG_LABEL "linter"
#include <core/Macros.hpp>

#include "SessionLinter.hpp"
#include "SessionCodeSearch.hpp"
#include "SessionAsyncRCompletions.hpp"

#include <set>

#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include <boost/shared_ptr.hpp>
#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>

#include <core/r_util/RSourceIndex.hpp>
#include <core/FileUtils.hpp>
#include <core/collection/Tree.hpp>
#include <core/collection/Stack.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;
using namespace core::collection;
using namespace rparser;

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

void addInferredSymbols(std::set<std::string>* pSymbols)
{
   BOOST_FOREACH(const AsyncLibraryCompletions& completions,
                 RSourceIndex::getAllCompletions() | boost::adaptors::map_values)
   {
      pSymbols->insert(completions.exports.begin(),
                       completions.exports.end());
   }
}

void addNamespaceSymbols(std::set<std::string>* pSymbols)
{
   BOOST_FOREACH(const std::set<std::string>& symbolNames,
                 RSourceIndex::getImportFromDirectives() | boost::adaptors::map_values)
   {
      pSymbols->insert(symbolNames.begin(), symbolNames.end());
   }
}

Error getAllAvailableRSymbols(const FilePath& filePath,
                              std::set<std::string>* pSymbols)
{
   using namespace r::exec;
   
   Error error = RFunction(".rs.availableRSymbols").call(pSymbols);
   if (error)
      return error;
   
   // Get all of the symbols made available by `library()` calls
   addInferredSymbols(pSymbols);
   
   // Add symbols made available from a package NAMESPACE
   if (projects::projectContext().directory().complete("NAMESPACE").exists())
      addNamespaceSymbols(pSymbols);

   // If this is an R package project, get the symbols for all functions
   // etc. in the project.
   if (filePath.isWithin(projects::projectContext().directory()))
      code_search::addAllProjectSymbols(pSymbols);
   
   return Success();
}

} // end anonymous namespace

ParseResults parse(const std::wstring& rCode,
                   const FilePath& origin)
{
   ParseResults results;
   
   ParseOptions options;
   options.setRecordStyleLint(userSettings().enableStyleDiagnostics());
   options.setLintRFunctions(userSettings().lintRFunctionCalls());
   
   results = rparser::parse(rCode, options);
   
   ParseNode* pRoot = results.parseTree();
   if (!pRoot)
   {
      std::string codeSnippet;
      if (rCode.length() > 40)
         codeSnippet = string_utils::wideToUtf8(rCode.substr(0, 40)) + "...";
      else
         codeSnippet = string_utils::wideToUtf8(rCode);
      
      std::string message = std::string() +
            "Parse failed: no parse tree available for code " +
            "'" + codeSnippet + "'";
      
      LOG_ERROR_MESSAGE(message);
      return ParseResults();
   }
   
   // First, get all of the symbols within the parse tree that do not have
   // an associated definition in scope.
   std::vector<ParseItem> unresolvedItems;
   pRoot->findAllUnresolvedSymbols(&unresolvedItems);
   
   // Now, find all available R symbols -- that is, objects on the search path,
   // or symbols that would otherwise be made available at runtime (e.g.
   // pacakge imports)
   std::set<std::string> objects;
   Error error = getAllAvailableRSymbols(origin, &objects);
   if (error)
   {
      LOG_ERROR(error);
      return ParseResults();
   }
   
   // For each unresolved symbol, add it to the lint if it's not on the search
   // path.
   BOOST_FOREACH(const ParseItem& item, unresolvedItems)
   {
      if (!r::util::isRKeyword(item.symbol) &&
          objects.count(string_utils::strippedOfBackQuotes(item.symbol)) == 0)
      {
         addUnreferencedSymbol(item, results.lint());
      }
   }
   
   return results;
}

ParseResults parse(const std::string& rCode,
                   const FilePath& origin)
{
   return parse(string_utils::utf8ToWide(rCode), origin);
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

module_context::SourceMarkerSet asSourceMarkerSet(const LintItems& items,
                                                  const FilePath& filePath)
{
   using namespace module_context;
   std::vector<SourceMarker> markers;
   markers.reserve(items.size());
   BOOST_FOREACH(const LintItem& item, items)
   {
      markers.push_back(SourceMarker(
                           sourceMarkerTypeFromString(lintTypeToString(item.type)),
                           filePath,
                           item.startRow + 1,
                           item.startColumn + 1,
                           core::html_utils::HTML(item.message),
                           true));
   }
   return SourceMarkerSet("Diagnostics", markers);
}

Error extractRCode(const std::string& contents,
                   const std::string& reOpen,
                   const std::string& reClose,
                   std::string* pContent)
{
   using namespace r::exec;
   RFunction extract(".rs.extractRCode");
   extract.addParam(contents);
   extract.addParam(reOpen);
   extract.addParam(reClose);
   Error error = extract.call(pContent);
   return error;
}

Error lintRSourceDocument(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   using namespace source_database;
   
   // Ensure response is always at least an array, even on 'failure'
   pResponse->setResult(json::Array());
   
   std::string documentId;
   std::string documentPath;
   bool showMarkersTab = false;
   Error error = json::readParams(request.params,
                                  &documentId,
                                  &documentPath,
                                  &showMarkersTab);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Try to get the contents from the database
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = get(documentId, pDoc);
   
   // don't log on error here (it's possible that we might attempt to lint a
   // document immediately after a suspend-resume, and so we fail to get the
   // contents of that document)
   if (error)
      return error;
   
   FilePath origin = module_context::resolveAliasedPath(documentPath);
   
   // Don't lint files that belong to unmonitored projects
   if (module_context::isUnmonitoredPackageSourceFile(origin))
      return Success();
   
   // Extract R code from various R-code-containing filetypes.
   std::string content;
   if (pDoc->type() == SourceDocument::SourceDocumentTypeRSource)
      content = pDoc->contents();
   else if (pDoc->type() == SourceDocument::SourceDocumentTypeRMarkdown)
      error = extractRCode(pDoc->contents(),
                           "^\\s*[`]{3}{\\s*r.*}\\s*$",
                           "^\\s*[`]{3}\\s*$",
                           &content);
   else if (pDoc->type() == SourceDocument::SourceDocumentTypeSweave)
      error = extractRCode(pDoc->contents(),
                           "^\\s*<<.*>>=\\s*$",
                           "^\\s*@\\s*$",
                           &content);
   else if (pDoc->type() == SourceDocument::SourceDocumentTypeCpp)
      error = extractRCode(pDoc->contents(),
                           "^\\s*/[*]{3,}\\s*[rR]\\s*$",
                           "^\\s*[*]+/",
                           &content);
   else
      return Success();
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   ParseResults results = parse(content, origin);
   
   pResponse->setResult(lintAsJson(results.lint()));
   
   if (showMarkersTab)
   {
      using namespace module_context;
      SourceMarkerSet markers = asSourceMarkerSet(results.lint(),
                                                  core::FilePath(pDoc->path()));
      showSourceMarkers(markers, MarkerAutoSelectNone);
   }
   
   return Success();
}

SEXP rs_lintRFile(SEXP filePathSEXP)
{
   using namespace r::sexp;
   
   Protect protect;
   ListBuilder builder(&protect);
   
   std::string path = safeAsString(filePathSEXP);
   FilePath filePath(module_context::resolveAliasedPath(path));
   
   if (!filePath.exists())
      return r::sexp::create(builder, &protect);
   
   std::string contents;
   Error error = module_context::readAndDecodeFile(
            filePath,
            projects::projectContext().defaultEncoding(),
            false,
            &contents);
   
   if (error)
   {
      LOG_ERROR(error);
      return r::sexp::create(builder, &protect);
   }
   
   std::string rCode = file_utils::readFile(filePath);
   ParseResults results = parse(rCode, filePath);
   const std::vector<LintItem>& lint = results.lint().get();
   
   std::size_t n = lint.size();
   for (std::size_t i = 0; i < n; ++i)
   {
      const LintItem& item = lint[i];
      
      ListBuilder el(&protect);
      
      // NOTE: R / document indexing is 1-based, so adjust for that.
      el.add("start.row", item.startRow + 1);
      el.add("start.column", item.startColumn + 1);
      el.add("end.row", item.endRow + 1);
      el.add("end.column", item.endColumn + 1);
      el.add("message", item.message);
      el.add("type", lintTypeToString(item.type));
      
      builder.add(el);
   }
   
   return r::sexp::create(builder, &protect);
}

static std::wstring s_rCode;

SEXP rs_loadString(SEXP rCodeSEXP)
{
   s_rCode = string_utils::utf8ToWide(CHAR(STRING_ELT(rCodeSEXP, 0)));
   return R_NilValue;
}

SEXP rs_parse(SEXP rCodeSEXP)
{
   std::string code = r::sexp::safeAsString(rCodeSEXP);
   ParseResults results = parse(code, FilePath());
   r::sexp::Protect protect;
   return r::sexp::create((int) results.lint().size(), &protect);
}

void onNAMESPACEchanged()
{
   using namespace r::exec;
   using namespace r::sexp;
   
   if (!projects::projectContext().hasProject())
      return;
   
   FilePath NAMESPACE(projects::projectContext().directory().complete("NAMESPACE"));
   if (!NAMESPACE.exists())
      return;
   
   RFunction parseNamespace(".rs.parseNamespaceImports");
   parseNamespace.addParam(NAMESPACE.absolutePath());
   
   r::sexp::Protect protect;
   SEXP result;
   Error error = parseNamespace.call(&result, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   std::set<std::string> importPkgNames;
   error = getNamedListElement(result, "import", &importPkgNames);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   RSourceIndex::ImportFromMap importFromSymbols;
   error = getNamedListElement(result, "importFrom", &importFromSymbols);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   RSourceIndex::setImportedPackages(importPkgNames);
   RSourceIndex::setImportFromDirectives(importFromSymbols);
   
   // Kick off an update of the cached async completions
   r_completions::AsyncRCompletions::update();
}

void onLintBlacklistChanged()
{
   NseFunctionBlacklist::instance().sync();
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   std::string namespacePath =
         projects::projectContext().directory().complete("NAMESPACE").absolutePath();
   
   std::string lintFilePath =
         projects::projectContext().directory().complete(".rstudio_lint_blacklist").absolutePath();
   
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      std::string eventPath = event.fileInfo().absolutePath();
      if (eventPath == namespacePath)
         onNAMESPACEchanged();
      else if (eventPath == lintFilePath)
         onLintBlacklistChanged();
   }
}

bool isSnippetFilePath(const FilePath& filePath,
                       std::string* pMode)
{
   if (filePath.isDirectory())
      return false;
   
   std::string filename = filePath.filename();
   std::size_t lastDotIndex = filename.rfind('.');
   if (lastDotIndex == std::string::npos)
      return false;
   
   if (filename.substr(lastDotIndex) != ".snippets")
      return false;
   
   *pMode = filename.substr(0, lastDotIndex - 1);
   return true;
}

void checkAndNotifyClientIfSnippetsAvailable()
{
   // Check to see if we have a snippets folder locally
   FilePath homeDir(core::system::getenv("HOME"));
   if (!homeDir.exists())
      return;
   
   FilePath snippetsDir = homeDir.complete(".R").complete("snippets");
   if (!snippetsDir.exists())
      return;
   
   // Get the contents of each file here, and pass that info back up
   // to the client
   std::vector<FilePath> snippetPaths;
   Error error = snippetsDir.children(&snippetPaths);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   json::Array jsonData;
   BOOST_FOREACH(const FilePath& filePath, snippetPaths)
   {
      // bail if this doesn't appear to be a snippets file
      std::string mode;
      if (!isSnippetFilePath(filePath, &mode))
         continue;
      
      std::string contents;
      error = readStringFromFile(filePath, &contents);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      json::Object snippetJson;
      snippetJson["mode"] = mode;
      snippetJson["contents"] = contents;
      jsonData.push_back(snippetJson);
   }

   ClientEvent event(client_events::kSnippetsChanged, jsonData);
   module_context::enqueClientEvent(event);
}

void afterSessionInitHook(bool newSession)
{
   if (projects::projectContext().hasProject() &&
       projects::projectContext().directory().complete("NAMESPACE").exists())
   {
      onNAMESPACEchanged();
   }
   
   checkAndNotifyClientIfSnippetsAvailable();
}

} // anonymous namespace

core::Error initialize()
{
   using namespace rstudio::core;
   using boost::bind;
   using namespace module_context;
   
   events().afterSessionInitHook.connect(afterSessionInitHook);
   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("Diagnostics", cb);
   
   RS_REGISTER_CALL_METHOD(rs_lintRFile, 1);
   RS_REGISTER_CALL_METHOD(rs_loadString, 1);
   RS_REGISTER_CALL_METHOD(rs_parse, 1);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionLinter.R"))
         (bind(registerRpcMethod, "lint_r_source_document", lintRSourceDocument));
   
   // call once on initialization to ensure lint up to date
   onLintBlacklistChanged();

   return initBlock.execute();

}

} // end namespace linter
} // end namespace modules
} // end namespace session
} // end namespace rstudio
