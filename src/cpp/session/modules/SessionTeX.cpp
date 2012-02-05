/*
 * SessionTeX.cpp
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

#include "SessionTeX.hpp"

#include <string>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "tex/SessionTexEngine.hpp"
#include "tex/SessionRnwWeave.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace tex {

namespace {


FilePath pdfPathForTexPath(const FilePath& texPath)
{
   return texPath.parent().complete(texPath.stem() + ".pdf");
}

SEXP rs_viewPdf(SEXP texPathSEXP)
{
   FilePath pdfPath = pdfPathForTexPath(FilePath(r::sexp::asString(texPathSEXP)));
   module_context::showFile(pdfPath, "_rstudio_compile_pdf");
   return R_NilValue;
}

Error getTexCapabilities(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(tex::capabilitiesAsJson());
   return Success();
}

} // anonymous namespace


core::json::Array supportedRnwWeaveTypes()
{
   return tex::rnw_weave::supportedTypes();
}

json::Object capabilitiesAsJson()
{
   json::Object obj;

   bool texInstalled;
   Error error = r::exec::RFunction(".rs.is_tex_installed").call(&texInstalled);
   obj["tex_installed"] = !error ? texInstalled : false;

   rnw_weave::getTypesInstalledStatus(&obj);

   return obj;
}

Error initialize()
{
   R_CallMethodDef viewPdfMethodDef ;
   viewPdfMethodDef.name = "rs_viewPdf" ;
   viewPdfMethodDef.fun = (DL_FUNC) rs_viewPdf ;
   viewPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(viewPdfMethodDef);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (tex::rnw_weave::initialize)
      (tex::engine::initialize)
      (bind(registerRpcMethod, "get_tex_capabilities", getTexCapabilities))
      (bind(sourceModuleRFile, "SessionTeX.R"))
      ;
  return initBlock.execute();
}

} // namespace tex
} // namespace modules
} // namesapce session

