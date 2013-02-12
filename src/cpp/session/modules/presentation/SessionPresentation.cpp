/*
 * SessionPresentation.cpp
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


#include "SessionPresentation.hpp"


#include <boost/bind.hpp>

#include <core/Exec.hpp>
#include <core/http/Util.hpp>


#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "PresentationState.hpp"
#include "SlideRequestHandler.hpp"


using namespace core;

namespace session {
namespace modules { 
namespace presentation {

namespace {


SEXP rs_showPresentation(SEXP directorySEXP)
{
   try
   {
      if (session::options().programMode() == kSessionProgramModeServer)
      {
         // validate path
         FilePath dir(r::sexp::asString(directorySEXP));
         if (!dir.exists())
            throw r::exec::RErrorException("Directory " + dir.absolutePath() +
                                           " does not exist.");

         // initialize state
         presentation::state::init(dir);

         // notify the client
         ClientEvent event(client_events::kShowPresentationPane,
                           presentation::state::asJson());
         module_context::enqueClientEvent(event);
      }
      else
      {
         throw r::exec::RErrorException("Presentations are not supported "
                                        "in desktop mode.");
      }
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

SEXP rs_showPresentationHelpDoc(SEXP helpDocSEXP)
{
   try
   {
      if (session::options().programMode() == kSessionProgramModeServer)
      {
         // verify a presentation is active
         if (!presentation::state::isActive())
         {
            throw r::exec::RErrorException(
                                    "No presentation is currently active");
         }

         // resolve against presentation directory
         std::string helpDoc = r::sexp::asString(helpDocSEXP);
         FilePath helpDocPath = presentation::state::directory().childPath(
                                                                     helpDoc);
         if (!helpDocPath.exists())
         {
            throw r::exec::RErrorException("Path " + helpDocPath.absolutePath()
                                           + " not found.");
         }

         // build url and fire event
         std::string url = "help/presentation/?file=";
         std::string file = module_context::createAliasedPath(helpDocPath);
         url += http::util::urlEncode(file, true);

         ClientEvent event(client_events::kShowHelp, url);
         module_context::enqueClientEvent(event);
      }
      else
      {
         throw r::exec::RErrorException("Presentations are not supported "
                                        "in desktop mode.");
      }
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

Error setPresentationSlideIndex(const json::JsonRpcRequest& request,
                                json::JsonRpcResponse*)
{
   int index;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error;

   presentation::state::setSlideIndex(index);

   return Success();
}

Error closePresentationPane(const json::JsonRpcRequest&,
                            json::JsonRpcResponse*)
{
   presentation::state::clear();

   return Success();
}

Error presentationExecuteCode(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // get the code
   std::string code;
   Error error = json::readParam(request.params, 0, &code);
   if (error)
      return error;

   // confirm we are active
   if (!presentation::state::isActive())
   {
      pResponse->setError(json::errc::MethodUnexpected);
      return Success();
   }

   // execute within the context of the presentation directory
   RestoreCurrentPathScope restorePathScope(
                                    module_context::safeCurrentPath());
   error = presentation::state::directory().makeCurrentPath();
   if (error)
      return error;


   // actually execute the code (show error in the console)
   error = r::exec::executeString(code);
   if (error)
   {
      std::string errMsg = "Error executing code: " + code + "\n";
      errMsg += r::endUserErrorMessage(error);
      module_context::consoleWriteError(errMsg + "\n");
   }

   return Success();
}



} // anonymous namespace


json::Value presentationStateAsJson()
{
   return presentation::state::asJson();
}

Error initialize()
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // register rs_showPresentation
      R_CallMethodDef methodDefShowPresentation;
      methodDefShowPresentation.name = "rs_showPresentation" ;
      methodDefShowPresentation.fun = (DL_FUNC) rs_showPresentation;
      methodDefShowPresentation.numArgs = 1;
      r::routines::addCallMethod(methodDefShowPresentation);

      // register rs_showPresentationHelpDoc
      R_CallMethodDef methodDefShowHelpDoc;
      methodDefShowHelpDoc.name = "rs_showPresentationHelpDoc" ;
      methodDefShowHelpDoc.fun = (DL_FUNC) rs_showPresentationHelpDoc;
      methodDefShowHelpDoc.numArgs = 1;
      r::routines::addCallMethod(methodDefShowHelpDoc);

      using boost::bind;
      using namespace session::module_context;
      ExecBlock initBlock ;
      initBlock.addFunctions()
         (bind(registerUriHandler, "/presentation", handlePresentationPaneRequest))
         (bind(registerRpcMethod, "set_presentation_slide_index", setPresentationSlideIndex))
         (bind(registerRpcMethod, "close_presentation_pane", closePresentationPane))
         (bind(registerRpcMethod, "presentation_execute_code", presentationExecuteCode))
         (bind(presentation::state::initialize))
         (bind(sourceModuleRFile, "SessionPresentation.R"));

      return initBlock.execute();
   }
   else
   {
      return Success();
   }
}

} // namespace presentation
} // namespace modules
} // namesapce session

