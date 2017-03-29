/*
 * SessionDiagnostics.cpp
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

#define RSTUDIO_DEBUG_LABEL "diagnostics"
// #define RSTUDIO_ENABLE_DEBUG_MACROS

#include <core/Macros.hpp>

#include "SessionDiagnostics.hpp"

#include "SessionCodeSearch.hpp"
#include "SessionAsyncPackageInformation.hpp"
#include "SessionRParser.hpp"

#include <set>

#include <core/Debug.hpp>
#include <core/Exec.hpp>
#include <core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/YamlUtil.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

#include "shiny/SessionShiny.hpp"

#include <boost/shared_ptr.hpp>
#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>

#include <core/r_util/RSourceIndex.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/CsvParser.hpp>
#include <core/collection/Tree.hpp>
#include <core/collection/Stack.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace diagnostics {

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

void doCheckDefinedButNotUsed(ParseNode* pNode, ParseResults& results)
{
   using namespace core::algorithm;
   
   // Find the definition positions.
   const ParseNode::SymbolPositions& definitions =
         pNode->getDefinedSymbols();
   
   for (ParseNode::SymbolPositions::const_iterator it = definitions.begin();
        it != definitions.end();
        ++it)
   {
      const std::string& symbolName = it->first;
      if (results.globals().count(symbolName))
         continue;
      
      if (pNode->isSymbolDefinedButNotUsed(symbolName, true, true))
      {
         ParseNode::Positions* symbolPos = NULL;
         if (get(definitions, symbolName, &symbolPos))
         {
            results.lint().symbolDefinedButNotUsed(
                     symbolName,
                     it->second[0]);
         }
      }
   }
}

void checkDefinedButNotUsed(ParseResults& results)
{
   ParseNode::Children children = results.parseTree()->getChildren();
   BOOST_FOREACH(const boost::shared_ptr<ParseNode>& child, children)
   {
      doCheckDefinedButNotUsed(child.get(), results);
   }
}

void addInferredSymbols(const FilePath& filePath,
                        const std::string& documentId,
                        std::set<std::string>* pSymbols)
{
   using namespace code_search;
   using namespace source_database;
   
   boost::shared_ptr<RSourceIndex> pIndex = rSourceIndex().get(documentId);
   
   // If that failed, try getting the index from the project index.
   if (!pIndex)
      pIndex = code_search::getIndexedProjectFile(filePath);
   
   // If we still don't have an index, bail
   if (!pIndex)
      return;
   
   // We have the index -- now list the packages discovered in
   // 'library' calls, and add those here.
   BOOST_FOREACH(const std::string& package, pIndex->getInferredPackages())
   {
      const PackageInformation& completions =
            pIndex->getPackageInformation(package);
      
      pSymbols->insert(completions.exports.begin(),
                       completions.exports.end());
   }
   
   // make 'shiny' implicitly available in shiny documents
   if (modules::shiny::getShinyFileType(filePath) != modules::shiny::ShinyNone)
   {
      const PackageInformation& completions = pIndex->getPackageInformation("shiny");
      pSymbols->insert(completions.exports.begin(), completions.exports.end());
   }
   
   // make 'params' implicitly available if we have a YAML header
   if (yaml::hasYamlHeader(filePath))
      pSymbols->insert("params");
   
   // make 'input', 'output' implicitly available in Shiny documents
   if (modules::shiny::isShinyRMarkdownDocument(filePath))
   {
      pSymbols->insert("input");
      pSymbols->insert("output");
   }
}

void addNamespaceSymbols(std::set<std::string>* pSymbols)
{
   // Add symbols specifically mentioned as 'importFrom'
   // directives in the NAMESPACE.
   BOOST_FOREACH(const std::set<std::string>& symbolNames,
                 RSourceIndex::getImportFromDirectives() | boost::adaptors::map_values)
   {
      pSymbols->insert(symbolNames.begin(), symbolNames.end());
   }
   
   // Make all (exported) symbols published by packages
   // that are 'import'ed in the NAMESPACE.
   BOOST_FOREACH(const std::string& package,
                 RSourceIndex::getImportedPackages())
   {
      DEBUG("- Adding imports for package '" << package << "'");
      const PackageInformation& pkgInfo =
            RSourceIndex::getPackageInformation(package);
      
      DEBUG("--- Adding " << pkgInfo.exports.size() << " symbols");
      pSymbols->insert(
               pkgInfo.exports.begin(),
               pkgInfo.exports.end());
   }
}

class PackageSymbolRegistry : boost::noncopyable
{
public:
   
   typedef std::map<std::string, std::vector<std::string> > Registry;
   
   void fillPackageSymbols(const std::string& pkgName,
                           std::set<std::string>* pOutput)
   {
      if (!registry_.count(pkgName))
      {
         SEXP envSEXP = r::sexp::asEnvironment(pkgName);
         if (envSEXP == R_EmptyEnv)
            return;
         
         Error error = r::sexp::objects(envSEXP, true, &registry_[pkgName]);
         if (error) LOG_ERROR(error);
      }
      
      const std::vector<std::string>& symbols = registry_[pkgName];
      pOutput->insert(
               symbols.begin(),
               symbols.end());
   }
   
   void fillNamespaceSymbols(const std::string& pkgName,
                             std::set<std::string>* pOutput,
                             bool exportsOnly = true)
   {
      if (!registry_.count(pkgName))
      {
         SEXP envSEXP = r::sexp::asNamespace(pkgName);
         if (envSEXP == R_EmptyEnv)
            return;

         if (exportsOnly)
         {
            Error error = r::sexp::getNamespaceExports(envSEXP, &registry_[pkgName]);
            if (error) LOG_ERROR(error);
         }
         else
         {
            Error error = r::sexp::objects(envSEXP, true, &registry_[pkgName]);
            if (error) LOG_ERROR(error);
         }
      }
      
      const std::vector<std::string>& symbols = registry_[pkgName];
      pOutput->insert(
               symbols.begin(),
               symbols.end());
   }
   
private:
   Registry registry_;
};

PackageSymbolRegistry& packageSymbolRegistry()
{
   static PackageSymbolRegistry instance;
   return instance;
}

void addBaseSymbols(std::set<std::string>* pSymbols)
{
   PackageSymbolRegistry& registry = packageSymbolRegistry();
   registry.fillPackageSymbols("base", pSymbols);
   registry.fillPackageSymbols("datasets", pSymbols);
   registry.fillPackageSymbols("graphics", pSymbols);
   registry.fillPackageSymbols("grDevices", pSymbols);
   registry.fillPackageSymbols("methods", pSymbols);
   registry.fillPackageSymbols("stats", pSymbols);
   registry.fillPackageSymbols("utils", pSymbols);
}

void addRcppExportedSymbols(const FilePath& filePath,
                            const std::string& documentId,
                            std::set<std::string>* pSymbols)
{
   if (!(filePath.hasExtensionLowerCase(".cpp") || filePath.hasExtensionLowerCase(".cc")))
      return;
   
   static boost::regex reRcppExport("^\\s*//\\s*\\[\\[\\s*Rcpp::export\\s*\\]\\]\\s*$");
   
   boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument());
   Error error = source_database::get(documentId, pDoc);
   if (error)
      return;
   
   std::vector<std::string> contents = core::algorithm::split(pDoc->contents(), "\n");
   std::size_t n = contents.size();
   
   for (std::size_t i = 0; i < n - 1; ++i)
   {
      const std::string& line = contents[i];
      if (regex_utils::match(line, reRcppExport))
      {
         const std::string& next = contents[i + 1];
         std::size_t leftParenIndex = next.find('(');
         if (leftParenIndex == std::string::npos)
            continue;
         
         std::string start = string_utils::substring(next, 0, leftParenIndex);
         std::size_t lastSpace = start.find_last_of(" \t");
         if (lastSpace == std::string::npos)
            continue;
         
         std::string fnName = string_utils::substring(start, lastSpace + 1);
         pSymbols->insert(fnName);
      }
   }
}

// For an R package, symbols are looked up in this order:
//
// 1) The package's own objects (exported or not),
// 2) In a special environment for 'importFrom' objects,
// 3) In the set of namespaces gathered through 'import',
// 4) The base namespace.
//
// We don't want to search for symbols on the search path here,
// since they would not get properly resolved at runtime.
Error getAvailableSymbolsForPackage(const FilePath& filePath,
                                    const std::string& documentId,
                                    std::set<std::string>* pSymbols)
{
   // Add project symbols (ie, top-level symbols within an R package)
   code_search::addAllProjectSymbols(pSymbols);
   
   // Symbols inferred from the NAMESPACE (importFrom, import)
   addNamespaceSymbols(pSymbols);
   
   // Add symbols made available by explicit `library()` calls
   // within this document.
   addInferredSymbols(filePath, documentId, pSymbols);
   
   // Add in symbols that would be made available by `// [[Rcpp::export]]`
   addRcppExportedSymbols(filePath, documentId, pSymbols);
   
   // Symbols that are 'automatically' made available to packages. In other
   // words, symbols that packages can use without explicitly importing them.
   // In other words, symbols that `R CMD check` will silently resolve to one
   // of the base packages. Note that not all `base` packages are allowed here.
   // These packages appear to be (as of R 3.1.0):
   //
   //     base, graphics, grDevices, methods, stats, stats4, utils
   //
   addBaseSymbols(pSymbols);
   
   return Success();
}

// For a generic R project, we are less strict on where we attempt
// to discover objects -- we simply consider all symbols available on
// the current search path.
Error getAvailableSymbolsForProject(const FilePath& filePath,
                                    const std::string& documentId,
                                    std::set<std::string>* pSymbols)
{
   // Get all available symbols on the search path.
   Error error = r::exec::RFunction(".rs.availableRSymbols").call(pSymbols);
   if (error)
      return error;
   
   // Add in symbols that would be made available by `// [[Rcpp::export]]`
   addRcppExportedSymbols(filePath, documentId, pSymbols);
   
   // Get all of the symbols made available by `library()` calls
   // within this document.
   addInferredSymbols(filePath, documentId, pSymbols);
   
   return Success();
}

void addTestPackageSymbols(std::set<std::string>* pSymbols)
{
   if (!projects::projectContext().isPackageProject())
      return;
   
   PackageSymbolRegistry& registry = packageSymbolRegistry();
   
   const r_util::RPackageInfo& pkgInfo =
         projects::projectContext().packageInfo();
   
   std::string packageFields;
   
   packageFields += pkgInfo.depends();
   packageFields += pkgInfo.imports();
   packageFields += pkgInfo.suggests();
   
   if (packageFields.find("testthat") != std::string::npos)
      registry.fillNamespaceSymbols("testthat", pSymbols, false);
   else if (packageFields.find("RUnit") != std::string::npos)
      registry.fillNamespaceSymbols("RUnit", pSymbols, false);
   else if (packageFields.find("assertthat") != std::string::npos)
      registry.fillNamespaceSymbols("assertthat", pSymbols, false);
}

Error getAllAvailableRSymbols(const FilePath& filePath,
                              const std::string& documentId,
                              const ParseResults& results,
                              std::set<std::string>* pSymbols)
{
   // If this file lies within the current project, then
   // we want to pull symbols from specific places -- specifically,
   // _not_ the current search path. We want to infer whether the
   // functions in the package would work at runtime.
   //
   // For R package development, when linting a 'test' file, we can
   // safely assume that the package itself will be loaded.
   FilePath projDir = projects::projectContext().directory();
   Error error;
   
   if (projects::projectContext().isPackageProject() && filePath.isWithin(projDir))
   {
      DEBUG("- Package file: '" << filePath.absolutePath() << "'");
      error = getAvailableSymbolsForPackage(filePath, documentId, pSymbols);
   }
   else
   {
      DEBUG("- Project file: '" << filePath.absolutePath() << "'");
      error = getAvailableSymbolsForProject(filePath, documentId, pSymbols);
   }
   
   if (error) LOG_ERROR(error);
   
   // Add common 'testing' packages, based on the DESCRIPTION's
   // 'Imports' and 'Suggests' fields, and use that if we're within a
   // common 'test'ing directory.
   if (filePath.isWithin(projDir.childPath("inst")) ||
       filePath.isWithin(projDir.childPath("tests")))
   {
      addTestPackageSymbols(pSymbols);
   }
   
   if (filePath.isWithin(projects::projectContext().directory().childPath("tests/testthat")))
   {
      PackageSymbolRegistry& registry = packageSymbolRegistry();
      registry.fillNamespaceSymbols("testthat", pSymbols, false);
   }
   
   // If the file is named 'server.R', 'ui.R' or 'app.R', we'll implicitly
   // assume that it depends on Shiny.
   std::string basename = boost::algorithm::to_lower_copy(
            filePath.filename());
   
   if (basename == "server.r" ||
       basename == "ui.r" ||
       basename == "app.r")
   {
      PackageSymbolRegistry& registry = packageSymbolRegistry();
      registry.fillNamespaceSymbols("shiny", pSymbols, false);
   }
   
   pSymbols->insert(results.globals().begin(), results.globals().end());
   
   return error;
      
}

void checkNoDefinitionInScope(const FilePath& origin,
                              const std::string& documentId,
                              ParseResults& results)
{
   ParseNode* pRoot = results.parseTree();
   
   std::vector<ParseItem> unresolvedItems;
   pRoot->findAllUnresolvedSymbols(&unresolvedItems);
   
   // Now, find all available R symbols -- that is, objects on the search path,
   // or symbols that would otherwise be made available at runtime (e.g.
   // package imports)
   std::set<std::string> objects;
   Error error = getAllAvailableRSymbols(origin, documentId, results, &objects);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // For each unresolved symbol, add it to the lint if it's not on the search
   // path.
   BOOST_FOREACH(const ParseItem& item, unresolvedItems)
   {
      if (!r::util::isRKeyword(item.symbol) &&
          !r::util::isWindowsOnlyFunction(item.symbol) &&
          objects.count(string_utils::strippedOfBackQuotes(item.symbol)) == 0)
      {
         addUnreferencedSymbol(item, results.lint());
      }
   }
}

bool lintOptionValueAsBool(const std::string& value)
{
   if (value.empty())
      return false;
   
   std::string lower = boost::algorithm::to_lower_copy(value);
   if (lower[0] == 'n' || lower[0] == 'f')
      return false;
   
   if (lower[0] == 'y' || lower[0] == 't')
      return true;
   
   return false;
}

struct FileLocalLintOptions
{
   std::vector< std::pair<std::string, std::string> > options;
   std::set<std::string> globals;
};

typedef std::pair< std::vector<std::string>, std::string::const_iterator> ParsedCSVLine;

void parseLintOptionGlobals(const std::string& text, FileLocalLintOptions* pOptions)
{
   using namespace core::text;
   
   // Find the first '=' after "globals"
   std::string::const_iterator begin = std::find(text.begin(), text.end(), '=');
   if (begin == text.end())
      return;
   
   // Parse the rest as a CSV line
   std::string::const_iterator end = text.end();
   
   ParsedCSVLine parsed = parseCsvLine(begin + 1, end, true);
   BOOST_FOREACH(const std::string element, parsed.first)
   {
      pOptions->globals.insert(string_utils::trimWhitespace(element));
   }
}

void parseLintOption(const std::string& text, FileLocalLintOptions* pOptions)
{
   using namespace core::text;
   
   boost::regex reGlobals("^\\s*suppress\\s*=");
   if (regex_utils::search(text, reGlobals))
      return parseLintOptionGlobals(text, pOptions);
   
   ParsedCSVLine line = parseCsvLine(text.begin(), text.end(), true);
   
   BOOST_FOREACH(const std::string& entry, line.first)
   {
      std::string::const_iterator it = 
            std::find(entry.begin(), entry.end(), '=');
      
      if (it == entry.end()) continue;
      pOptions->options.push_back(
               std::make_pair(
                  string_utils::trimWhitespace(std::string(entry.begin(), it)),
                  string_utils::trimWhitespace(std::string(it + 1, entry.end()))));
   }
}

FileLocalLintOptions parseLintOptions(const std::vector<std::string>& lintText)
{
   FileLocalLintOptions options;
   BOOST_FOREACH(const std::string text, lintText)
   {
      parseLintOption(text, &options);
   }
   return options;
}

void applyOptions(const FileLocalLintOptions& fileOptions,
                  ParseOptions* pOptions)
{
   if (!fileOptions.globals.empty())
      pOptions->globals().insert(fileOptions.globals.begin(),
                                 fileOptions.globals.end());
   
   typedef std::pair<std::string, std::string> PairStringString;
   BOOST_FOREACH(const PairStringString& option, fileOptions.options)
   {
      if (option.first == "style")
         pOptions->setRecordStyleLint(lintOptionValueAsBool(option.second));
      else if (option.first == "level")
      {
         if (option.second == "syntax")
            pOptions->setSyntaxOnly();
         else if (option.second == "core")
            pOptions->setCoreDiagnostics();
         else if (option.second == "all")
            pOptions->enableAllDiagnostics();
      }
   }
}

const char * const kLintComment = "(?:^|\\n)#+\\s+\\!diagnostics";

void setFileLocalParseOptions(const std::wstring& rCode,
                              ParseOptions* pOptions,
                              bool* pNoLint)
{
   using namespace string_utils;
   
   // Extract all of the lint commands.
   boost::regex reLintComments(kLintComment);
   std::vector<std::string> lintCommands;
   boost::wsmatch match;
   
   std::wstring::const_iterator start = rCode.begin();
   std::wstring::const_iterator end = rCode.end();
   while (regex_utils::search(start, end, match, reLintComments))
   {
      std::wstring::const_iterator matchBegin = match[0].second;
      std::wstring::const_iterator matchEnd   = std::find(matchBegin, end, L'\n');
      std::string command = string_utils::trimWhitespace(std::string(matchBegin, matchEnd));
      
      if (command == "off")
      {
         *pNoLint = true;
         return;
      }
      
      lintCommands.push_back(command);
      start = match[0].second;
   }
   
   FileLocalLintOptions options = parseLintOptions(lintCommands);
   applyOptions(options, pOptions);
}

} // end anonymous namespace

ParseResults parse(const std::wstring& rCode,
                   const FilePath& origin,
                   const std::string& documentId = std::string(),
                   bool isExplicit = false)
{
   ParseResults results;
   ParseOptions options;
   
   options.setLintRFunctions(
            userSettings().lintRFunctionCalls());
   
   options.setCheckArgumentsToRFunctionCalls(
            userSettings().checkArgumentsToRFunctionCalls());
   
   options.setWarnIfVariableIsDefinedButNotUsed(
            isExplicit && userSettings().warnIfVariableDefinedButNotUsed());
   
   options.setWarnIfNoSuchVariableInScope(
            userSettings().warnIfNoSuchVariableInScope());
   
   options.setRecordStyleLint(
            userSettings().enableStyleDiagnostics());
   
   bool noLint = false;
   setFileLocalParseOptions(rCode, &options, &noLint);
   if (noLint)
      return ParseResults();
   
   results = rparser::parse(origin, rCode, options);
   
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
   
   if (options.warnIfNoSuchVariableInScope())
      checkNoDefinitionInScope(origin, documentId, results);
   
   if (options.warnIfVariableIsDefinedButNotUsed())
      checkDefinedButNotUsed(results);
   
   return results;
}

ParseResults parse(const std::string& rCode,
                   const FilePath& origin,
                   const std::string& documentId)
{
   return parse(string_utils::utf8ToWide(rCode), origin, documentId);
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

module_context::SourceMarkerSet asSourceMarkerSet(
      std::map<FilePath, LintItems>& lint)
{
   using namespace module_context;
   std::vector<SourceMarker> markers;
   for (std::map<FilePath, LintItems>::const_iterator it = lint.begin();
        it != lint.end();
        ++it)
   {
      const FilePath& path = it->first;
      const LintItems& lintItems = it->second;
      BOOST_FOREACH(const LintItem& item, lintItems)
      {
         markers.push_back(SourceMarker(
                              sourceMarkerTypeFromString(lintTypeToString(item.type)),
                              path,
                              item.startRow + 1,
                              item.startColumn + 1,
                              core::html_utils::HTML(item.message),
                              true));
      }
   }
   
   return SourceMarkerSet("Diagnostics", markers);
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
   bool isExplicit = false;
   Error error = json::readParams(request.params,
                                  &documentId,
                                  &documentPath,
                                  &showMarkersTab,
                                  &isExplicit);
   
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
   error = r_utils::extractRCode(pDoc->contents(), pDoc->type(), &content);
   if (error)
      return error;
   
   ParseResults results = diagnostics::parse(
            string_utils::utf8ToWide(content),
            origin,
            documentId,
            isExplicit);
   
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
   
   std::string rCode;
   error = core::readStringFromFile(
            filePath,
            &rCode,
            string_utils::LineEndingPosix);
   
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   ParseResults results = parse(rCode, filePath, std::string());
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
   r_packages::AsyncPackageInformationProcess::update();
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   std::string namespacePath =
         projects::projectContext().directory().complete("NAMESPACE").absolutePath();
   
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      std::string eventPath = event.fileInfo().absolutePath();
      if (eventPath == namespacePath)
         onNAMESPACEchanged();
   }
}

void afterSessionInitHook(bool newSession)
{
   if (projects::projectContext().hasProject() &&
       projects::projectContext().directory().complete("NAMESPACE").exists())
   {
      onNAMESPACEchanged();
   }
   
   if (projects::projectContext().isPackageProject())
   {
      RSourceIndex::addGloballyInferredPackage(
               projects::projectContext().packageInfo().name());
      r_packages::AsyncPackageInformationProcess::update();
   }
}

bool collectLint(int depth,
                 const FilePath& path,
                 std::map<FilePath, LintItems>* pLint)
{
   if (path.extensionLowerCase() != ".r")
      return true;
   
   std::string contents;
   Error error = core::readStringFromFile(
            path,
            &contents,
            string_utils::LineEndingPosix);
   
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }
   
   ParseResults results = diagnostics::parse(
            string_utils::utf8ToWide(contents),
            path,
            std::string(),
            true);
   
   (*pLint)[path] = results.lint();
   return true;
}

SEXP rs_lintDirectory(SEXP directorySEXP)
{
   std::string directory = r::sexp::asString(directorySEXP);
   FilePath dirPath = module_context::resolveAliasedPath(directory);
   if (!dirPath.exists())
      return R_NilValue;
   
   std::map<FilePath, LintItems> lint;
   Error error = dirPath.childrenRecursive(
            boost::bind(collectLint, _1, _2, &lint));
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   using namespace module_context;
   SourceMarkerSet markers = asSourceMarkerSet(lint);
   showSourceMarkers(markers, MarkerAutoSelectNone);
   return R_NilValue;
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
   RS_REGISTER_CALL_METHOD(rs_lintDirectory, 1);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionDiagnostics.R"))
         (bind(registerRpcMethod, "lint_r_source_document", lintRSourceDocument));
   
   return initBlock.execute();

}

} // end namespace linter
} // end namespace modules
} // end namespace session
} // end namespace rstudio
