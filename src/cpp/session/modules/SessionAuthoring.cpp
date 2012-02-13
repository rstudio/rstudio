/*
 * SessionAuthoring.cpp
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

#include "SessionAuthoring.hpp"

#include <string>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionRnwWeave.hpp"
#include "tex/SessionPdfLatex.hpp"
#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionCompilePdfSupervisor.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace authoring {

namespace {

void viewPdf(const FilePath& texPath)
{
   FilePath pdfPath = texPath.parent().complete(texPath.stem() + ".pdf");
   module_context::showFile(pdfPath, "_rstudio_compile_pdf");
}

void publishPdf(const FilePath& texPath)
{
   std::string aliasedPath = module_context::createAliasedPath(texPath);
   ClientEvent event(client_events::kPublishPdf, aliasedPath);
   module_context::enqueClientEvent(event);
}


Error getTexCapabilities(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(authoring::texCapabilitiesAsJson());
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
   std::string targetFile, completedAction;
   bool terminateExisting;
   Error error = json::readParams(request.params,
                                  &targetFile,
                                  &completedAction,
                                  &terminateExisting);
   if (error)
      return error;
   FilePath targetFilePath = module_context::resolveAliasedPath(targetFile);

   // attempt to terminate existing if requested (this will wait up to
   // 1 second for the processes to exit). continue on even if we
   // are unable to terminate existing
   if (tex::compile_pdf::compileIsRunning() && terminateExisting)
   {
      tex::compile_pdf::terminateCompile();
   }

   // initialize the completed function
   boost::function<void()> completedFunction;
   if (completedAction == "view")
      completedFunction = boost::bind(viewPdf, targetFilePath);
   else if (completedAction == "publish")
      completedFunction = boost::bind(publishPdf, targetFilePath);

   // attempt to kickoff the compile
   bool started = tex::compile_pdf::startCompile(targetFilePath,
                                                 completedFunction);

   // return true
   pResponse->setResult(started);
   return Success();
}

Error compilePdfRunning(const json::JsonRpcRequest& request,
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

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (tex::compile_pdf_supervisor::initialize)
      (bind(registerRpcMethod, "is_tex_installed", isTexInstalled))
      (bind(registerRpcMethod, "get_tex_capabilities", getTexCapabilities))
      (bind(registerRpcMethod, "compile_pdf", compilePdf))
      (bind(registerRpcMethod, "compile_pdf_running", compilePdfRunning))
      (bind(registerRpcMethod, "terminate_compile_pdf", terminateCompilePdf))
   ;
  return initBlock.execute();
}

} // namespace authoring
} // namespace modules
} // namesapce session

