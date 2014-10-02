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

Error printCppCompletions(const core::json::JsonRpcRequest& request,
                          core::json::JsonRpcResponse* pResponse)
{
   std::string docId, docPath, docContents;
   bool docDirty;
   int line, column;
   Error error = json::readParams(request.params,
                                  &docId,
                                  &docPath,
                                  &docContents,
                                  &docDirty,
                                  &line,
                                  &column);
   if (error)
      return error;

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // first update the unsaved file database
   std::string filename = filePath.absolutePath();
   rSourceIndex().unsavedFiles().update(filename, docContents, docDirty);

   // now get the translation unit and do the code completion
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename);
   if (!tu.empty())
   {
      CodeCompleteResults results = tu.codeCompleteAt(filename, line, column);
      if (!results.empty())
      {
         for (unsigned i = 0; i<results.getNumResults(); i++)
         {
            std::string result = results.getResult(i).getText();
            module_context::consoleWriteOutput(result + "\n");
         }
      }
   }

   return Success();
}



} // namespace clang
} // namespace modules
} // namesapce session

