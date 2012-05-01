/*
 * SessionSynctex.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <session/projects/SessionProjects.hpp>

#include "SessionRnwConcordance.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace synctex {

namespace {

json::Value toJson(const FilePath& pdfFile,
                   const core::tex::PdfLocation& pdfLoc,
                   bool fromClick)
{
   if (!pdfLoc.empty())
   {
      json::Object pdfJson;
      pdfJson["file"] = module_context::createAliasedPath(pdfFile);
      pdfJson["page"] = pdfLoc.page();
      pdfJson["x"] = pdfLoc.x();
      pdfJson["y"] = pdfLoc.y();
      pdfJson["width"] = pdfLoc.width();
      pdfJson["height"] = pdfLoc.height();
      pdfJson["from_click"] = fromClick;
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

void applyForwardConcordance(const FilePath& mainFile,
                             core::tex::SourceLocation* pLoc)
{
   // skip if this isn't an Rnw
   if (pLoc->file().extensionLowerCase() != ".rnw")
      return;

   // try to read concordance
   using namespace tex::rnw_concordance;
   Concordances concordances;
   Error error = readIfExists(mainFile, &concordances);
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

Error parseConcoranceRequest(const json::JsonRpcRequest& request,
                             FilePath* pRootDocPath,
                             core::tex::SourceLocation* pLoc,
                             bool* pFromClick)
{
   // read params
   std::string rootDoc;
   json::Object sourceLocation;
   Error error = json::readParams(request.params, &rootDoc, &sourceLocation);
   if (error)
      return error;
   *pRootDocPath = module_context::resolveAliasedPath(rootDoc);

   // read source location
   std::string file;
   int line, column;
   error = json::readObject(sourceLocation,
                                  "file", &file,
                                  "line", &line,
                                  "column", &column,
                                  "from_click", pFromClick);
   if (error)
      return error;


   FilePath srcPath = module_context::resolveAliasedPath(file);

   *pLoc = core::tex::SourceLocation(srcPath, line, column);

   return Success();
}

json::Object sourceLocationAsJson(const core::tex::SourceLocation& srcLoc,
                                  bool fromClick)
{
   json::Object sourceLocation;
   sourceLocation["file"] = module_context::createAliasedPath(srcLoc.file());
   sourceLocation["line"] = srcLoc.line();
   sourceLocation["column"] = srcLoc.column();
   sourceLocation["from_click"] = fromClick;
   return sourceLocation;
}

Error synctexForwardSearch(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // read params
   std::string rootDoc;
   json::Object sourceLocation;
   Error error = json::readParams(request.params, &rootDoc, &sourceLocation);
   if (error)
      return error;
   FilePath rootDocPath = module_context::resolveAliasedPath(rootDoc);


   // do the search
   json::Value pdfLocation;
   error = forwardSearch(rootDocPath, sourceLocation, &pdfLocation);
   if (error)
      return error;

   // return the results
   pResponse->setResult(pdfLocation);

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

Error rpcApplyForwardConcordance(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   // parse params
   FilePath rootDocPath;
   core::tex::SourceLocation srcLoc;
   bool fromClick;
   Error error = parseConcoranceRequest(request,
                                        &rootDocPath,
                                        &srcLoc,
                                        &fromClick);
   if (error)
      return error;

   applyForwardConcordance(rootDocPath, &srcLoc);

   pResponse->setResult(sourceLocationAsJson(srcLoc, fromClick));

   return Success();
}

Error rpcApplyInverseConcordance(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   FilePath rootDocPath;
   core::tex::SourceLocation srcLoc;
   bool fromClick;
   Error error = parseConcoranceRequest(request,
                                        &rootDocPath,
                                        &srcLoc,
                                        &fromClick);
   if (error)
      return error;

   applyInverseConcordance(&srcLoc);

   pResponse->setResult(sourceLocationAsJson(srcLoc, fromClick));

   return Success();
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
         // find the top of the page content, however override it with
         // the passed x and y coordinates since they represent the
         // top of the user-visible content (in case the page is
         // scrolled down from the top)
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


Error forwardSearch(const FilePath& rootFile,
                    const json::Object& sourceLocation,
                    json::Value* pPdfLocation)
{
   // read params
   std::string file;
   int line, column;
   bool fromClick;
   Error error = json::readObject(sourceLocation,
                                  "file", &file,
                                  "line", &line,
                                  "column", &column,
                                  "from_click", &fromClick);
   if (error)
      return error;

   // determine input file
   FilePath inputFile = module_context::resolveAliasedPath(file);

   // determine pdf
   FilePath pdfFile = rootFile.parent().complete(rootFile.stem() + ".pdf");

   core::tex::Synctex synctex;
   if (synctex.parse(pdfFile))
   {
      core::tex::SourceLocation srcLoc(inputFile, line, column);
      applyForwardConcordance(rootFile, &srcLoc);

      core::tex::PdfLocation pdfLoc = synctex.forwardSearch(srcLoc);
      *pPdfLocation = toJson(pdfFile, pdfLoc, fromClick);
   }
   else
   {
      *pPdfLocation = json::Value();
   }

   return Success();
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "apply_forward_concordance", rpcApplyForwardConcordance))
      (bind(registerRpcMethod, "apply_inverse_concordance", rpcApplyInverseConcordance))
      (bind(registerRpcMethod, "synctex_forward_search", synctexForwardSearch))
      (bind(registerRpcMethod, "synctex_inverse_search", synctexInverseSearch))
   ;
   return initBlock.execute();
}

} // namespace synctex
} // namespace tex
} // namespace modules
} // namesapce session

