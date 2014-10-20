/*
 * CodeCompletion.cpp
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

#include "CodeCompletion.hpp"

#include <iostream>

#include <core/Error.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"

using namespace core ;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {

namespace {

core::json::Object toJson(const CodeCompleteResult& result)
{
   json::Object resultJson;
   resultJson["text"] = result.getText();
   return resultJson;
}


} // anonymous namespace


Error getCppCompletions(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
{
   // get params
   std::string docPath;
   int line, column;
   Error error = json::readParams(request.params,
                                  &docPath,
                                  &line,
                                  &column);
   if (error)
      return error;

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // get the translation unit and do the code completion
   std::string filename = filePath.absolutePath();
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename);
   if (!tu.empty())
   {
      std::string lastCompletionText;
      json::Array completionsJson;
      CodeCompleteResults results = tu.codeCompleteAt(filename, line, column);
      if (!results.empty())
      {
         for (unsigned i = 0; i<results.getNumResults(); i++)
         {
            std::string completionText = results.getResult(i).getText();

            // de-dup (works because we know the completions have been sorted)
            if (completionText != lastCompletionText)
               completionsJson.push_back(toJson(results.getResult(i)));

            lastCompletionText = completionText;
         }

      }

      json::Object resultJson;
      resultJson["completions"] = completionsJson;
      pResponse->setResult(resultJson);
   }
   else
   {
      // set null result indicating this file doesn't support completions
      pResponse->setResult(json::Value());
   }

   return Success();
}

} // namespace clang
} // namespace modules
} // namesapce session

