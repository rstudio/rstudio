/*
 * Diagnostics.cpp
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

#include "Diagnostics.hpp"

#include <map>
#include <set>

#include <core/libclang/LibClang.hpp>

#include <session/SessionModuleContext.hpp>

#include "CompilationDatabase.hpp"
#include "SourceIndex.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

json::Object locationToPositionJson(const FileLocation& location)
{
   json::Object locationJson;
   locationJson["line"] = safe_convert::numberTo<double>(location.line, 1);
   locationJson["column"] = safe_convert::numberTo<double>(location.column, 1);
   return locationJson;
}

json::Object rangeToJson(const FileRange& range)
{
   json::Object rangeJson;
   rangeJson["start"] = locationToPositionJson(range.start);
   rangeJson["end"] = locationToPositionJson(range.end);
   return rangeJson;
}

json::Object rangeToJson(const SourceRange& range)
{
   return rangeToJson(range.getFileRange());
}

json::Object fixitToJson(const FixIt& fixit)
{
   json::Object fixitJson;
   fixitJson["range"] = rangeToJson(fixit.sourceRange());
   fixitJson["replacement"] = fixit.replacement();
   return fixitJson;
}

} // anonymous namespace


json::Object diagnosticToJson(const TranslationUnit& tu,
                              const Diagnostic& diagnostic)
{
   json::Object diagnosticJson;

   diagnosticJson["severity"] = safe_convert::numberTo<double>(
                                             diagnostic.severity(), 0);
   diagnosticJson["category"] = safe_convert::numberTo<double>(
                                             diagnostic.category(), 0);
   diagnosticJson["category_text"] = diagnostic.categoryText();

   diagnosticJson["enable_option"] = diagnostic.enableOption();
   diagnosticJson["disable_option"] = diagnostic.disableOption();

   diagnosticJson["message"] = diagnostic.spelling();

   json::Array jsonRanges;
   if (!diagnostic.location().empty())
   {
      FileLocation location = diagnostic.location().getSpellingLocation();
      diagnosticJson["file"] = module_context::createAliasedPath(location.filePath);
      diagnosticJson["position"] = locationToPositionJson(location);

      // source ranges (if there are no source ranges then create one based on
      // the token at the location of the diagnostic)
      unsigned numRanges = diagnostic.numRanges();
      if (numRanges > 0)
      {
         for (unsigned int i=0; i < diagnostic.numRanges(); i++)
            jsonRanges.push_back(rangeToJson(diagnostic.getSourceRange(i)));
      }
      else
      {
         Cursor cursor = tu.getCursor(
            location.filePath.getAbsolutePath(),
                                      location.line,
                                      location.column);

         Tokens tokens(tu.getCXTranslationUnit(), cursor.getExtent());
         for (unsigned int i = 0; i<tokens.numTokens(); i++)
         {
            Token token = tokens.getToken(i);
            FileRange tokenRange = token.extent().getFileRange();
            if (tokenRange.start == location)
            {
               jsonRanges.push_back(rangeToJson(tokenRange));
               break;
            }
         }
      }
   }
   diagnosticJson["ranges"] = jsonRanges;

   // fixits
   json::Array fixitsJson;
   for (unsigned int i = 0; i<diagnostic.numFixIts(); i++)
      fixitsJson.push_back(fixitToJson(diagnostic.getFixIt(i)));
   diagnosticJson["fixits"] = fixitsJson;

   // recurse over children
   json::Array childrenJson;
   boost::shared_ptr<DiagnosticSet> pChildren = diagnostic.children();
   if (pChildren)
   {
      for (unsigned i = 0; i < pChildren->diagnostics(); i++)
      {
         childrenJson.push_back(
            diagnosticToJson(tu, *pChildren->getDiagnostic(i)));
      }
   }
   diagnosticJson["children"] = childrenJson;

   return diagnosticJson;
}

// A diagnostic of our own, anchored to the top of the file
json::Object syntheticDiagnosticJson(const FilePath& filePath,
                                     CXDiagnosticSeverity severity,
                                     const std::string& message)
{
   json::Object diagnosticJson;
   diagnosticJson["severity"] = safe_convert::numberTo<double>(severity, 0);
   diagnosticJson["category"] = 0;
   diagnosticJson["category_text"] = "";
   diagnosticJson["enable_option"] = "";
   diagnosticJson["disable_option"] = "";
   diagnosticJson["message"] = message;
   diagnosticJson["file"] = module_context::createAliasedPath(filePath);
   diagnosticJson["position"] = locationToPositionJson(FileLocation(filePath, 1, 1));
   diagnosticJson["ranges"] = json::Array();
   diagnosticJson["fixits"] = json::Array();
   diagnosticJson["children"] = json::Array();
   return diagnosticJson;
}

// The location-less diagnostics clang emits for the flags we generate (an
// '-fopenmp' from Makevars that this libclang doesn't support, say). Those
// were never shown, and the user can't act on them from compile_flags.txt,
// so when a flags file is in play we show only the location-less
// diagnostics beyond this baseline. Clang doesn't say which flag produced a
// diagnostic, so the baseline comes from parsing an empty copy of the file
// with just the generated flags, once per distinct set of them.
const std::set<std::string>& baselineDiagnostics(const std::string& filename)
{
   static std::map<std::vector<std::string>, std::set<std::string>> s_baselines;

   std::vector<std::string> args = clangCompilationDatabase().generatedCompileArgs(filename);
   auto it = s_baselines.find(args);
   if (it == s_baselines.end())
   {
      std::vector<std::string> messages = sourceIndex().commandLineDiagnostics(filename, args);
      it = s_baselines.emplace(args, std::set<std::string>(messages.begin(), messages.end())).first;
   }

   return it->second;
}

json::Array getCppDiagnosticsJson(const FilePath& filePath)
{
   json::Array diagnosticsJson;

   // a bad flag in a compile_flags.txt is reported by clang against the
   // command line rather than the file (or makes the parse fail outright),
   // so when one is in play we surface those at the top of the file instead
   // of silently dropping them
   FilePath flagsFile = compileFlagsFileForSource(filePath);
   bool hasUserFlags = !flagsFile.isEmpty() && !readCompileFlags(flagsFile).empty();

   // get diagnostics from translation unit
   std::string filename = filePath.getAbsolutePath();
   TranslationUnit tu = sourceIndex().getTranslationUnit(filename, true);
   if (tu.empty())
   {
      if (hasUserFlags)
      {
         // (without the PCH, so a bad flag doesn't trigger a second failed PCH build)
         bool indexable = !clangCompilationDatabase().compileArgsForTranslationUnit(filename, false).empty();
         if (indexable)
         {
            std::string message =
                  "libclang could not parse this file; check the flags in " +
                  module_context::createAliasedPath(flagsFile);
            diagnosticsJson.push_back(syntheticDiagnosticJson(filePath, CXDiagnostic_Error, message));
         }
      }

      return diagnosticsJson;
   }

   const std::set<std::string>* pBaseline = nullptr;

   unsigned numDiagnostics = tu.getNumDiagnostics();
   for (unsigned i = 0; i < numDiagnostics; i++)
   {
      boost::shared_ptr<Diagnostic> pDiag = tu.getDiagnostic(i);
      FileLocation location = pDiag->location().getSpellingLocation();
      if (location.filePath == filePath)
      {
         diagnosticsJson.push_back(diagnosticToJson(tu, *pDiag));
      }
      else if (hasUserFlags && location.empty())
      {
         if (pBaseline == nullptr)
            pBaseline = &baselineDiagnostics(filename);

         if (pBaseline->count(pDiag->spelling()) > 0)
            continue;

         json::Object diagnosticJson = diagnosticToJson(tu, *pDiag);
         diagnosticJson["file"] = module_context::createAliasedPath(filePath);
         diagnosticJson["position"] = locationToPositionJson(FileLocation(filePath, 1, 1));
         diagnosticsJson.push_back(diagnosticJson);
      }
   }

   return diagnosticsJson;
}

Error getCppDiagnostics(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
{
   // get params
   std::string docPath;
   Error error = json::readParams(request.params, &docPath);
   if (error)
      return error;

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // don't lint files that belong to unmonitored projects
   if (module_context::isUnmonitoredPackageSourceFile(filePath))
   {
      pResponse->setResult(json::Array());
      return Success();
   }

   pResponse->setResult(getCppDiagnosticsJson(filePath));
   return Success();
}

} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

