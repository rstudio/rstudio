/*
 * SessionViewer.cpp
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

#include "SessionViewer.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace viewer {

namespace {

// show error message from R
SEXP rs_browserInternal(SEXP urlSEXP, SEXP fullHeightSEXP)
{
   try
   {
      json::Object dataJson;
      dataJson["url"] = r::sexp::safeAsString(urlSEXP);
      dataJson["full_height"] = r::sexp::asLogical(fullHeightSEXP);

      ClientEvent event(client_events::kViewerNavigate, dataJson);
      module_context::enqueClientEvent(event);
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

} // anonymous namespace


Error initialize()
{
   // register rs_askForPassword with R
   R_CallMethodDef methodDefBrowserInternal ;
   methodDefBrowserInternal.name = "rs_browserInternal" ;
   methodDefBrowserInternal.fun = (DL_FUNC) rs_browserInternal ;
   methodDefBrowserInternal.numArgs = 2;
   r::routines::addCallMethod(methodDefBrowserInternal);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   //initBlock.addFunctions()
   //   (bind(registerRpcMethod, "get_public_key", getPublicKey));
   return initBlock.execute();
}


} // namespace viewer
} // namespace modules
} // namesapce session

