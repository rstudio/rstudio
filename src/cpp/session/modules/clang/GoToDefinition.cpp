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
#include "DefinitionIndex.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

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

   // try to find the location
   FileLocation loc = findDefinitionLocation(FileLocation(filePath, line, column));
   if (!loc.empty())
   {
      using namespace module_context;
      json::Object jsonResult;
      jsonResult["file"] = createFileSystemItem(loc.filePath);
      json::Object jsonPosition;
      jsonPosition["line"] = safe_convert::numberTo<double>(loc.line, 1);
      jsonPosition["column"] = 1; // always return column 1 so the user
                                  // isn't surprised by cursor location
      jsonResult["position"] = jsonPosition;
      pResponse->setResult(jsonResult);
   }
   else
   {
      pResponse->setResult(json::Value());
   }

   return Success();
}


} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

