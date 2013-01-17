/*
 * SessionLearning.cpp
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


#include "SessionLearning.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Exec.hpp>
#include <core/text/TemplateFilter.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionLearningState.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {

FilePath learningResourcesPath()
{
   return session::options().rResourcesPath().complete("learning");
}

SEXP rs_showLearningPane(SEXP dirSEXP)
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // TODO: validate path

      // initialize learning state
      learning::state::init(FilePath(r::sexp::asString(dirSEXP)));

      // notify the client
      ClientEvent event(client_events::kShowLearningPane,
                        learning::state::asJson());
      module_context::enqueClientEvent(event);
   }

   return R_NilValue;
}

core::Error closeLearningPane(const json::JsonRpcRequest&,
                              json::JsonRpcResponse*)
{
   learning::state::clear();

   return Success();
}

void handleLearningContentRequest(const http::Request& request,
                                  http::Response* pResponse)
{
   // return not found if learning isn't active
   if (!learning::state::isActive())
   {
      pResponse->setError(http::status::NotFound, request.uri() + " not found");
      return;
   }

   // get the requested path
   std::string path = http::util::pathAfterPrefix(request, "/learning/");

   // special handling for the root (process learning template)
   if (path.empty())
   {
      // build template variables
      std::map<std::string,std::string> vars;
      vars["title"] = "My title";

      // process the template
      pResponse->setFile(learningResourcesPath().complete("slides.html"),
                         request,
                         text::TemplateFilter(vars));

   }

   // special handling for reveal.js assets
   else if (boost::algorithm::starts_with(path, "revealjs/"))
   {
      path = http::util::pathAfterPrefix(request, "/learning/revealjs/");
      FilePath filePath = learningResourcesPath().complete("revealjs/" + path);
      pResponse->setFile(filePath, request);
   }

   // just serve the file back
   else
   {
      pResponse->setFile(learning::state::directory().childPath(path),
                         request);
   }
}

} // anonymous namespace

json::Value learningStateAsJson()
{
   return learning::state::asJson();
}

Error initialize()
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // register rs_showLearningPane
      R_CallMethodDef methodDefShowLearningPane;
      methodDefShowLearningPane.name = "rs_showLearningPane" ;
      methodDefShowLearningPane.fun = (DL_FUNC) rs_showLearningPane;
      methodDefShowLearningPane.numArgs = 1;
      r::routines::addCallMethod(methodDefShowLearningPane);

      using boost::bind;
      using namespace session::module_context;
      ExecBlock initBlock ;
      initBlock.addFunctions()
         (bind(registerUriHandler, "/learning", handleLearningContentRequest))
         (bind(registerRpcMethod, "close_learning_pane", closeLearningPane))
         (bind(learning::state::initialize))
         (bind(sourceModuleRFile, "SessionLearning.R"));

      return initBlock.execute();
   }
   else
   {
      return Success();
   }
}

} // namespace learning
} // namespace modules
} // namesapce session

