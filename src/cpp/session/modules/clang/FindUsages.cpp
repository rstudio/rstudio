/*
 * FindUsages.cpp
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

#include "FindUsages.hpp"

#include <boost/foreach.hpp>

#include <core/libclang/LibClang.hpp>

#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

struct FindUsagesData
{
   explicit FindUsagesData(const std::string& USR)
      : USR(USR)
   {
   }
   std::string USR;
   std::vector<FileLocation> locations;
};

CXChildVisitResult findUsagesVisitor(CXCursor cxCursor,
                                     CXCursor,
                                     CXClientData data)
{
   // get pointer to data struct
   FindUsagesData* pData = (FindUsagesData*)data;

   // get the cursor and check if it's in the right file
   Cursor cursor(cxCursor);
   SourceLocation location = cursor.getSourceLocation();
   if (!location.isFromMainFile())
      return CXChildVisit_Continue;

   // get referenced cursor
   Cursor referencedCursor = cursor.getReferenced();
   if (!referencedCursor.isNull())
   {
      // get USR and compare to the USR we are seeking
      if (referencedCursor.getUSR() == pData->USR)
      {
         std::cout << pData->USR << " " << cursor.getFileLocation().line << std::endl;

      }
   }

   // recurse into namespaces, classes, etc.
   return CXChildVisit_Recurse;
}


} // anonymous namespace

Error findUsages(const json::JsonRpcRequest& request,
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

   // get the definition cursor for this file location
   core::libclang::FileLocation location(filePath, line, column);
   Cursor cursor = rSourceIndex().definitionForFileLocation(location);
   if (cursor.isNull())
      return Success();

   // get it's USR (bail if it doesn't have one)
   std::string USR = cursor.getUSR();
   if (USR.empty())
      return Success();

   // now look for references in the current translation unit
   TranslationUnit tu = rSourceIndex().getTranslationUnit(
                                          filePath.absolutePath(), true);
   if (tu.empty())
      return Success();

   // visit the cursors and accumulate file locations
   FindUsagesData findUsagesData(USR);
   libclang::clang().visitChildren(tu.getCursor().getCXCursor(),
                                   findUsagesVisitor,
                                   (CXClientData)&findUsagesData);


   BOOST_FOREACH(const FileLocation& loc, findUsagesData.locations)
   {
      std::cerr << loc.filePath << " [" << loc.line << ":" << loc.column << "]"
                << std::endl;
   }

   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session
} // namespace rstudio

