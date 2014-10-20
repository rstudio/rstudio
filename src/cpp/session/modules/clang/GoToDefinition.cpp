/*
 * GoToDefinition.cpp
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

#include "GoToDefinition.hpp"


#include <core/libclang/LibClang.hpp>

#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"

using namespace core;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {

namespace {


} // anonymous namespace


// https://github.com/Rip-Rip/clang_complete/issues/134

Error goToCppDefinition(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
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
      json::Object resultJson;

      resultJson["file"] = module_context::createFileSystemItem(filePath);

      json::Object posJson;
      posJson["line"] = line + 5;
      posJson["column"] = 1;
      resultJson["position"] = posJson;

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

