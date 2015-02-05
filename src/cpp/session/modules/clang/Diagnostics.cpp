/*
 * Diagnostics.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "Diagnostics.hpp"

#include <core/libclang/LibClang.hpp>

#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"

using namespace rstudio::core ;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

json::Object locationToJson(const FileLocation& location,
                            bool includeFile = true)
{
   json::Object locationJson;
   if (includeFile)
      locationJson["file"] = module_context::createAliasedPath(location.filePath);
   locationJson["line"] = safe_convert::numberTo<double>(location.line, 1);
   locationJson["column"] = safe_convert::numberTo<double>(location.column, 1);
   return locationJson;
}

json::Object rangeToJson(const FileRange& range, bool includeFile = true)
{
   json::Object rangeJson;
   rangeJson["start"] = locationToJson(range.start, includeFile);
   rangeJson["end"] = locationToJson(range.end, includeFile);
   return rangeJson;
}

json::Object rangeToJson(const SourceRange& range, bool includeFile = true)
{
   return rangeToJson(range.getFileRange(), includeFile);
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
   diagnosticJson["message"] = diagnostic.spelling();
   FileLocation location = diagnostic.location().getSpellingLocation();
   diagnosticJson["location"] = locationToJson(location);

   // source ranges (if there are no source ranges then create one based on
   // the token at the location of the diagnostic)
   json::Array jsonRanges;
   unsigned numRanges = diagnostic.numRanges();
   if (numRanges > 0)
   {
      for (unsigned int i=0; i < diagnostic.numRanges(); i++)
         jsonRanges.push_back(rangeToJson(diagnostic.getSourceRange(i), false));
   }
   else
   {
      Cursor cursor = tu.getCursor(location.filePath.absolutePath(),
                                   location.line,
                                   location.column);

      Tokens tokens(tu.getCXTranslationUnit(), cursor.getExtent());
      for (unsigned int i = 0; i<tokens.numTokens(); i++)
      {
         Token token = tokens.getToken(i);
         FileRange tokenRange = token.extent().getFileRange();
         if (tokenRange.start == location)
         {
            jsonRanges.push_back(rangeToJson(tokenRange, false));
            break;
         }
      }
   }
   diagnosticJson["ranges"] = jsonRanges;

   // fixits



   return diagnosticJson;
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

   // diagnostics to return
   json::Array diagnosticsJson;

   // get diagnostics from translation unit
   TranslationUnit tu = rSourceIndex().getTranslationUnit(
                                             filePath.absolutePath(),
                                             true);
   if (!tu.empty())
   {
      unsigned numDiagnostics = tu.getNumDiagnostics();
      for (unsigned i = 0; i < numDiagnostics; i++)
      {
         diagnosticsJson.push_back(diagnosticToJson(tu, *tu.getDiagnostic(i)));
      }
   }

   // return success
   pResponse->setResult(diagnosticsJson);
   return Success();
}

} // namespace clang
} // namespace modules
} // namesapce session
} // namespace rstudio

