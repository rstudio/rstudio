/*
 * SessionAuthoring.cpp
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

#include "SessionAuthoring.hpp"

#include <string>

#include <boost/regex.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>
#include <core/BrowserUtils.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionRnwWeave.hpp"
#include "tex/SessionPdfLatex.hpp"
#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionCompilePdfSupervisor.hpp"
#include "tex/SessionSynctex.hpp"
#include "tex/SessionViewPdf.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace authoring {

namespace {

FilePath pdfFilePath(const FilePath& texFilePath)
{
   return texFilePath.parent().complete(texFilePath.stem() + ".pdf");
}

void viewPdfExternal(const FilePath& texPath)
{
   module_context::showFile(pdfFilePath(texPath),
                            "_rstudio_compile_pdf");
}

Error getTexCapabilities(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(authoring::texCapabilitiesAsJson());
   return Success();
}

Error getChunkOptions(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string weaveType;
   Error error = json::readParams(request.params, &weaveType);
   if (error)
      return error;

   pResponse->setResult(tex::rnw_weave::chunkOptions(weaveType));
   return Success();
}

Error isTexInstalled(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(tex::pdflatex::isInstalled());
   return Success();
}

Error compilePdf(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // read params
   std::string targetFile, encoding, completedAction;
   json::Object sourceLocation;
   Error error = json::readParams(request.params,
                                  &targetFile,
                                  &encoding,
                                  &sourceLocation,
                                  &completedAction);
   if (error)
      return error;

   // determine compilation target
   FilePath targetFilePath = module_context::resolveAliasedPath(targetFile);

   // initialize the completed function
   boost::function<void()> completedFunction;
   if (completedAction == "view_external")
      completedFunction = boost::bind(viewPdfExternal, targetFilePath);

   // attempt to kickoff the compile
   bool started = tex::compile_pdf::startCompile(targetFilePath,
                                                 encoding,
                                                 sourceLocation,
                                                 completedFunction);

   // return true
   pResponse->setResult(started);
   return Success();
}

Error isCompilePdfRunning(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{

   pResponse->setResult(tex::compile_pdf::compileIsRunning());

   return Success();
}


Error terminateCompilePdf(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{

   pResponse->setResult(tex::compile_pdf::terminateCompile());

   return Success();
}


Error compilePdfClosed(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{

   tex::compile_pdf::notifyTabClosed();
   return Success();
}

SEXP rs_rnwTangle(SEXP filePathSEXP,
                  SEXP encodingSEXP,
                  SEXP rnwWeaveSEXP)
{
   try
   {
      modules::tex::rnw_weave::runTangle(r::sexp::asString(filePathSEXP),
                                         r::sexp::asString(encodingSEXP),
                                         r::sexp::asString(rnwWeaveSEXP));
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}



} // anonymous namespace


json::Array supportedRnwWeaveTypes()
{
   return tex::rnw_weave::supportedTypes();
}

json::Array supportedLatexProgramTypes()
{
   return tex::pdflatex::supportedTypes();
}

json::Object texCapabilitiesAsJson()
{
   json::Object obj;

   obj["tex_installed"] = tex::pdflatex::isInstalled();

   tex::rnw_weave::getTypesInstalledStatus(&obj);

   return obj;
}

bool hasRunningChildren()
{
   return tex::compile_pdf_supervisor::hasRunningChildren();
}

json::Object compilePdfStateAsJson()
{
   return tex::compile_pdf::currentStateAsJson();
}


Error initialize()
{
   // register tanble function
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_rnwTangle" ;
   methodDef.fun = (DL_FUNC) rs_rnwTangle ;
   methodDef.numArgs = 3;
   r::routines::addCallMethod(methodDef);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionAuthoring.R"))
      (tex::compile_pdf::initialize)
      (tex::compile_pdf_supervisor::initialize)
      (tex::synctex::initialize)
      (tex::view_pdf::initialize)
      (bind(registerRpcMethod, "is_tex_installed", isTexInstalled))
      (bind(registerRpcMethod, "get_tex_capabilities", getTexCapabilities))
      (bind(registerRpcMethod, "get_chunk_options", getChunkOptions))
      (bind(registerRpcMethod, "compile_pdf", compilePdf))
      (bind(registerRpcMethod, "is_compile_pdf_running", isCompilePdfRunning))
      (bind(registerRpcMethod, "terminate_compile_pdf", terminateCompilePdf))
      (bind(registerRpcMethod, "compile_pdf_closed", compilePdfClosed))
   ;
  return initBlock.execute();
}

} // namespace authoring
} // namespace modules
} // namespace session
} // namespace rstudio

