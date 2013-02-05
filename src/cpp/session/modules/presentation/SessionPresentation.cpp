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


SEXP rs_showPresentation(SEXP directorySEXP,
                         SEXP tabCaptionSEXP,
                         SEXP authorModeSEXP)
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
         presentation::state::init(dir,
                                   r::sexp::asString(tabCaptionSEXP),
                                   r::sexp::asLogical(authorModeSEXP));

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

core::Error setPresentationSlideIndex(const json::JsonRpcRequest& request,
                                      json::JsonRpcResponse*)
{
   int index;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error;

   presentation::state::setSlideIndex(index);

   return Success();
}

core::Error closePresentationPane(const json::JsonRpcRequest&,
                                 json::JsonRpcResponse*)
{
   presentation::state::clear();

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
      methodDefShowPresentation.numArgs = 3;
      r::routines::addCallMethod(methodDefShowPresentation);

      using boost::bind;
      using namespace session::module_context;
      ExecBlock initBlock ;
      initBlock.addFunctions()
         (bind(registerUriHandler, "/presentation", handlePresentationPaneRequest))
         (bind(registerRpcMethod, "set_presentation_slide_index", setPresentationSlideIndex))
         (bind(registerRpcMethod, "close_presentation_pane", closePresentationPane))
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

