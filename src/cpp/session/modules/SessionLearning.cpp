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

#include <core/Settings.hpp>

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
};

FilePath learningStatePath()
{
   FilePath path = module_context::scopedScratchPath().childPath("learning");
   Error error = path.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return path;
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

   settings.endUpdate();
}

LearningState loadLearningState()
{
   LearningState state;

   Settings settings;
   Error error = settings.initialize(learningStatePath());
   if (error)
   {
      LOG_ERROR(error);
      return state;
   }

   state.active = settings.getBool("active", false);

   return state;
}

} // anonymous namespace


json::Value learningStateAsJson()
{
   LearningState state = loadLearningState();

   json::Object stateJson;
   stateJson["active"] = state.active;
   return stateJson;
}

Error initialize()
{  


   return Success();
}
   


} // namespace learning
} // namespace modules
} // namesapce session

