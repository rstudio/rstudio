/*
 * SessionSynctex.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionSynctex.hpp"

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/tex/TexSynctex.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace synctex {

namespace {

Error synctexForwardSearch(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string file;
   int line, column;
   Error error = json::readObjectParam(request.params, 0,
                                       "file", &file,
                                       "line", &line,
                                       "column", &column);
   if (error)
      return error;
   FilePath inputFile = module_context::resolveAliasedPath(file);
   FilePath pdfFile = inputFile.parent().complete(inputFile.stem() + ".pdf");

   core::tex::Synctex synctex;
   if (synctex.parse(pdfFile))
   {
      core::tex::SourceLocation srcLoc(inputFile, line, column);
      core::tex::PdfLocation pdfLoc = synctex.forwardSearch(srcLoc);
      if (!pdfLoc.empty())
      {
         json::Object pdfJson;
         pdfJson["file"] = module_context::createAliasedPath(pdfFile);
         pdfJson["page"] = pdfLoc.page();
         pdfJson["x"] = pdfLoc.x();
         pdfJson["y"] = pdfLoc.y();
         pdfJson["width"] = pdfLoc.height();
         pdfJson["height"] = pdfLoc.width();
         pResponse->setResult(pdfJson);
      }
      else
      {
         pResponse->setResult(json::Value());
      }
   }
   else
   {
      pResponse->setResult(json::Value());
   }

   return Success();
}

Error synctexInverseSearch(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string file;
   int page;
   double x, y, width, height;
   Error error = json::readObjectParam(request.params, 0,
                                       "file", &file,
                                       "page", &page,
                                       "x", &x,
                                       "y", &y,
                                       "width", &width,
                                       "height", &height);
   if (error)
      return error;
   FilePath pdfPath = module_context::resolveAliasedPath(file);

   core::tex::Synctex synctex;
   if (synctex.parse(pdfPath))
   {
      core::tex::PdfLocation pdfLocation(page, x, y, width, height);
      core::tex::SourceLocation srcLoc = synctex.inverseSearch(pdfLocation);
      if (!srcLoc.empty())
      {
         json::Object srcJson;
         srcJson["file"] = module_context::createAliasedPath(srcLoc.file());
         srcJson["line"] = srcLoc.line();
         srcJson["column"] = srcLoc.column();
         pResponse->setResult(srcJson);
      }
      else
      {
         pResponse->setResult(json::Value());
      }
   }
   else
   {
      pResponse->setResult(json::Value());
   }

   return Success();
}


} // anonymous namespace

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "synctex_forward_search", synctexForwardSearch))
      (bind(registerRpcMethod, "synctex_inverse_search", synctexInverseSearch))
   ;
   return initBlock.execute();
}

} // namespace synctex
} // namespace tex
} // namespace modules
} // namesapce session

