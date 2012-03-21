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

#include <boost/regex.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionRnwWeave.hpp"
#include "tex/SessionPdfLatex.hpp"
#include "tex/SessionCompilePdf.hpp"
#include "tex/SessionCompilePdfSupervisor.hpp"
#include "tex/SessionSynctex.hpp"
#include "tex/SessionViewPdf.hpp"

using namespace core;

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
   std::string targetFile, completedAction;
   json::Object sourceLocation;
   Error error = json::readParams(request.params,
                                  &targetFile,
                                  &sourceLocation,
                                  &completedAction);
   if (error)
      return error;
   FilePath targetFilePath = module_context::resolveAliasedPath(targetFile);

   // initialize the completed function
   boost::function<void()> completedFunction;
   if (completedAction == "view_external")
      completedFunction = boost::bind(viewPdfExternal, targetFilePath);

   // attempt to kickoff the compile
   bool started = tex::compile_pdf::startCompile(targetFilePath,
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

bool isBrowserSupported(const std::string& userAgent,
                        const boost::regex& versionRegEx,
                        double requiredVersion)
{
   double detectedVersion = requiredVersion;
   boost::smatch match;
   if (boost::regex_search(userAgent, match, versionRegEx))
   {
      std::string versionString = match[1];
      detectedVersion = safe_convert::stringTo<double>(versionString,
                                                       detectedVersion);
      if (detectedVersion < requiredVersion)
         return false;
   }

   return true;
}


} // anonymous namespace


bool isPdfViewerSupported(const std::string& userAgent)
{
   // Qt 4.7 not supported
   bool isQt47 = userAgent.find("Qt/4.7") != std::string::npos;
   if (isQt47)
      return false;

   // Firefox >= 6 required
   boost::regex ffRegEx("Firefox/(\\d{1,4})");
   if (!isBrowserSupported(userAgent, ffRegEx, 6.0))
      return false;

   // Safari >= 5.1 required (look for both Safari and not Chrome in the UA
   // since Chrome also includes Safari in its UA)
   if ( userAgent.find("Safari") != std::string::npos &&
        userAgent.find("Chrome") == std::string::npos)
   {
      boost::regex safariRegEx("Version/(\\d{1,4}\\.\\d)");
      if (!isBrowserSupported(userAgent, safariRegEx, 5.1))
         return false;
   }

   return true;
}

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
} // namesapce session

