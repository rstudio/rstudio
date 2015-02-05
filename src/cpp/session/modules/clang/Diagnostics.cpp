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





} // anonymous namespace


json::Object diagnosticToJson(const Diagnostic& diagnostic)
{
   json::Object diagnosticJson;

   diagnosticJson["severity"] = safe_convert::numberTo<int>(
                                             diagnostic.getSeverity(), 0);
   diagnosticJson["message"] = diagnostic.getSpelling();
   FileLocation loc = diagnostic.getLocation().getSpellingLocation();
   diagnosticJson["file"] = module_context::createAliasedPath(loc.filePath);
   diagnosticJson["line"] = safe_convert::numberTo<int>(loc.line, 1);
   diagnosticJson["column"] = safe_convert::numberTo<int>(loc.column, 1);
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
         diagnosticsJson.push_back(diagnosticToJson(*tu.getDiagnostic(i)));
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

