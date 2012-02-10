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
#include "tex/SessionCompilePdfSupervisor.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace authoring {

namespace {

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
      (tex::compile_pdf::initialize)
      (bind(registerRpcMethod, "is_tex_installed", isTexInstalled))
      (bind(registerRpcMethod, "get_tex_capabilities", getTexCapabilities))
   ;
  return initBlock.execute();
}

} // namespace authoring
} // namespace modules
} // namesapce session

