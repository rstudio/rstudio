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
   FilePath directory;
};

// write-through cache of learning state
LearningState s_learningState;

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
   // update write-through cache
   s_learningState = state;

   // save to disk
   Settings settings;
   Error error = settings.initialize(learningStatePath());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   settings.beginUpdate();
   settings.set("active", state.active);
   settings.set("directory", state.directory.absolutePath());
   settings.endUpdate();
}

void loadLearningState()
{
   FilePath statePath = learningStatePath();
   if (statePath.exists())
   {
      Settings settings;
      Error error = settings.initialize(learningStatePath());
      if (error)
         LOG_ERROR(error);

      s_learningState.active = settings.getBool("active", false);
      s_learningState.directory = FilePath(settings.get("directory"));
   }
   else
   {
      s_learningState = LearningState();
   }
}

SEXP rs_showLearningPane(SEXP dirSEXP)
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // setup new state and save it
      LearningState state;
      state.active = true;
      state.directory = FilePath(r::sexp::asString(dirSEXP));
      saveLearningState(state);

      // notify the client
      ClientEvent event(client_events::kShowLearningPane,
                        learning::learningStateAsJson());
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

FilePath learningFilePath(const std::string& path)
{
   if (s_learningState.directory.empty())
      return FilePath();

   std::string resolvedPath = path;

   if (resolvedPath.empty())
   {
      if (s_learningState.directory.childPath("index.html").exists())
         resolvedPath = "index.html";
      else
         resolvedPath = "index.htm";
   }

   return s_learningState.directory.childPath(resolvedPath);
}

void handleLearningContentRequest(const http::Request& request,
                                  http::Response* pResponse)
{
   // get the requested path
   std::string path = http::util::pathAfterPrefix(request, "/learning/");

   // resolve the file
   FilePath filePath = learning::learningFilePath(path);
   if (filePath.empty())
   {
      pResponse->setError(http::status::NotFound, "Not found");
      return;
   }

   // serve it back
   pResponse->setFile(filePath, request);
}

} // anonymous namespace


json::Value learningStateAsJson()
{
   json::Object stateJson;
   stateJson["active"] = s_learningState.active;
   stateJson["directory"] = s_learningState.directory.absolutePath();
   return stateJson;
}

Error initialize()
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // load learning state -- subsequent reads of learning state
      // are done from the in-memory cache
      loadLearningState();

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

