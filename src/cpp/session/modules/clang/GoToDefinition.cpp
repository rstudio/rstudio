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


// NOTE: Go to definition works for types/functions declared in header files
// as well as for types/functions declared within the same translation unit.
// It does not however locate function definitions in other translation
// units (rather it just navigates to the header/declaration). In order to
// go to definitions across translation units we'll need to build (or create
// on demand) the source indexes for the other translation units.
//
// Note also that the right implementation for cross translation unit
// definition seeking is likely not to do keep full libclang parses of every
// translation unit in memory (as this could be expensive from a resource
// and time standpoint). Rather, we might want to parse with the following
// flag (CXTranslationUnit_SkipFunctionBodies) and then store the results
// in a separate index (as discussed on this thread:
// https://github.com/Rip-Rip/clang_complete/issues/134).

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

   // default to returning null (no-op)
   pResponse->setResult(json::Value());

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // get the translation unit and do the code completion
   std::string filename = filePath.absolutePath();
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename, true);
   if (!tu.empty())
   {
      // get the cursor
      Cursor cursor = tu.getCursor(filename, line, column);
      if (cursor.isNull())
         return Success();

      // if it's not a definition then get the definition
      if (!cursor.isDefinition())
      {
         cursor = cursor.getDefinition();
         if (cursor.isNull())
            return Success();
      }

      // get the source location of the definition
      SourceLocation loc = cursor.getSourceLocation();
      std::string filename;
      unsigned line, column;
      loc.getSpellingLocation(&filename, &line, &column);

      // return it
      using namespace module_context;
      json::Object jsonResult;
      jsonResult["file"] = createFileSystemItem(FilePath(filename));
      json::Object jsonPosition;
      jsonPosition["line"] = safe_convert::numberTo<double>(line, 1);
      jsonPosition["column"] = safe_convert::numberTo<double>(column, 1);
      jsonResult["position"] = jsonPosition;
      pResponse->setResult(jsonResult);
   }

   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

