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

#include "SessionRnwConcordance.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace synctex {

namespace {

json::Value toJson(const FilePath& pdfFile,
                   const core::tex::PdfLocation& pdfLoc)
{
   if (!pdfLoc.empty())
   {
      json::Object pdfJson;
      pdfJson["file"] = module_context::createAliasedPath(pdfFile);
      pdfJson["page"] = pdfLoc.page();
      pdfJson["x"] = pdfLoc.x();
      pdfJson["y"] = pdfLoc.y();
      pdfJson["width"] = pdfLoc.height();
      pdfJson["height"] = pdfLoc.width();
      pdfJson["from_click"] = false;
      return pdfJson;
   }
   else
   {
      return json::Value();
   }
}

json::Value toJson(const core::tex::SourceLocation& srcLoc)
{
   if (!srcLoc.empty())
   {
      json::Object srcJson;
      srcJson["file"] = module_context::createAliasedPath(srcLoc.file());
      srcJson["line"] = srcLoc.line();
      srcJson["column"] = srcLoc.column();
      return srcJson;
   }
   else
   {
      return json::Value();
   }
}

void applyForwardConcordance(core::tex::SourceLocation* pLoc)
{
   // skip if this isn't an Rnw
   if (pLoc->file().extensionLowerCase() != ".rnw")
      return;

   // try to read concordance
   using namespace tex::rnw_concordance;
   Concordances concordances;
   Error error = readIfExists(pLoc->file(), &concordances);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // try to find a match
   FileAndLine texLine = concordances.texLine(FileAndLine(pLoc->file(),
                                                          pLoc->line()));
   if (!texLine.empty())
   {
      *pLoc = core::tex::SourceLocation(texLine.filePath(),
                                        texLine.line(),
                                        pLoc->column());
   }
}

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
      applyForwardConcordance(&srcLoc);

      core::tex::PdfLocation pdfLoc = synctex.forwardSearch(srcLoc);
      pResponse->setResult(toJson(pdfFile, pdfLoc));
   }
   else
   {
      pResponse->setResult(json::Value());
   }

   return Success();
}


void applyInverseConcordance(core::tex::SourceLocation* pLoc)
{
    // try to read concordance
   using namespace tex::rnw_concordance;
   Concordances concordances;
   Error error = readIfExists(pLoc->file(), &concordances);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // try to find a match
   FileAndLine rnwLine = concordances.rnwLine(FileAndLine(pLoc->file(),
                                                          pLoc->line()));
   if (!rnwLine.empty())
   {
      *pLoc = core::tex::SourceLocation(rnwLine.filePath(),
                                        rnwLine.line(),
                                        pLoc->column());
   }
}

Error synctexInverseSearch(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string file;
   int page;
   double x, y, width, height;
   bool fromClick;
   Error error = json::readObjectParam(request.params, 0,
                                       "file", &file,
                                       "page", &page,
                                       "x", &x,
                                       "y", &y,
                                       "width", &width,
                                       "height", &height,
                                       "from_click", &fromClick);
   if (error)
      return error;
   FilePath pdfPath = module_context::resolveAliasedPath(file);

   core::tex::Synctex synctex;
   if (synctex.parse(pdfPath))
   {
      if (!fromClick)
      {
         core::tex::PdfLocation contLoc = synctex.topOfPageContent(page);
         x = std::max((float)x, contLoc.x());
         y = std::max((float)y, contLoc.y());

      }

      core::tex::PdfLocation pdfLocation(page, x, y, width, height);

      core::tex::SourceLocation srcLoc = synctex.inverseSearch(pdfLocation);
      applyInverseConcordance(&srcLoc);

      pResponse->setResult(toJson(srcLoc));
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

