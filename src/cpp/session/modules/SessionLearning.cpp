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

#include <core/Exec.hpp>
#include <core/Settings.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {
      
struct LearningState
{
   LearningState()
      : active(false)
   {
   }

   bool active;
   std::string url;
};

FilePath learningStatePath()
{
   FilePath path = module_context::userScratchPath().childPath("learning");
   Error error = path.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return path.childPath("learning-state");
}

void saveLearningState(const LearningState& state)
{
   Settings settings;
   Error error = settings.initialize(learningStatePath());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   settings.beginUpdate();
   settings.set("active", state.active);
   settings.set("url", state.url);

   settings.endUpdate();
}

LearningState loadLearningState()
{
   LearningState state;

   FilePath statePath = learningStatePath();
   if (statePath.exists())
   {
      Settings settings;
      Error error = settings.initialize(learningStatePath());
      if (error)
      {
         LOG_ERROR(error);
         return state;
      }
      state.active = settings.getBool("active", false);
      state.url = settings.get("url");
   }

   return state;
}

json::Value learningStateAsJson(const LearningState& state)
{
   json::Object stateJson;
   stateJson["active"] = state.active;
   stateJson["url"] = state.url;
   return stateJson;
}

SEXP rs_showLearningPane(SEXP paneUrlSEXP)
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      std::string paneUrl = r::sexp::safeAsString(paneUrlSEXP);

      // setup new state and save it
      LearningState state;
      state.active = true;
      state.url = paneUrl;
      saveLearningState(state);

      // notify the client
      ClientEvent event(client_events::kShowLearningPane,
                        learningStateAsJson(state));
      module_context::enqueClientEvent(event);
   }

   return R_NilValue;
}

core::Error closeLearningPane(const json::JsonRpcRequest&,
                              json::JsonRpcResponse*)
{
   saveLearningState(LearningState());
   return Success();
}

} // anonymous namespace


json::Value learningStateAsJson()
{
   LearningState state;

   // learning module is server only
   if (session::options().programMode() == kSessionProgramModeServer)
      state = loadLearningState();

   return learningStateAsJson(state);
}

Error initialize()
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
      (bind(registerRpcMethod, "close_learning_pane", closeLearningPane))
      (bind(sourceModuleRFile, "SessionLearning.R"));

   return initBlock.execute();
}

} // namespace learning
} // namespace modules
} // namesapce session

