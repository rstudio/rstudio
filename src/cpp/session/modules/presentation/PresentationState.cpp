/*
 * PresentationState.cpp
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


#include "PresentationState.hpp"

#include <core/FilePath.hpp>
#include <core/Settings.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace presentation {
namespace state {

namespace {
      
struct PresentationState
{
   PresentationState()
      : active(false), slideIndex(0)
   {
   }

   bool active;
   std::string paneCaption;
   FilePath filePath;
   int slideIndex;
};

// write-through cache of presentation state
PresentationState s_presentationState;

FilePath presentationStatePath()
{
   FilePath path = module_context::scopedScratchPath().childPath("presentation");
   Error error = path.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return path.childPath("presentation-state-v2");
}

std::string toPersistentPath(const FilePath& filePath)
{
   projects::ProjectContext& projectContext = projects::projectContext();

   if (projectContext.hasProject() &&
       filePath.isWithin(projectContext.directory()))
   {
      return filePath.relativePath(projectContext.directory());
   }
   else
   {
      return filePath.absolutePath();
   }
}

FilePath fromPersistentPath(const std::string& path)
{
   projects::ProjectContext& projectContext = projects::projectContext();
   if (projectContext.hasProject())
   {
      return projectContext.directory().complete(path);
   }
   else
   {
      return FilePath(path);
   }
}


void savePresentationState(const PresentationState& state)
{
   // update write-through cache
   s_presentationState = state;

   // save to disk
   Settings settings;
   Error error = settings.initialize(presentationStatePath());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   settings.beginUpdate();
   settings.set("active", state.active);
   settings.set("pane-caption", state.paneCaption);
   settings.set("file-path", toPersistentPath(state.filePath));
   settings.set("slide-index", state.slideIndex);
   settings.endUpdate();
}

void loadPresentationState()
{
   FilePath statePath = presentationStatePath();
   if (statePath.exists())
   {
      Settings settings;
      Error error = settings.initialize(presentationStatePath());
      if (error)
         LOG_ERROR(error);

      s_presentationState.active = settings.getBool("active", false);
      s_presentationState.paneCaption = settings.get("pane-caption", "Presentation");
      s_presentationState.filePath = fromPersistentPath(settings.get("file-path"));
      s_presentationState.slideIndex = settings.getInt("slide-index", 0);
   }
   else
   {
      s_presentationState = PresentationState();
   }
}

} // anonymous namespace


void init(const FilePath& filePath)
{
   PresentationState state;
   state.active = true;
   state.paneCaption = "Presentation";
   state.filePath = filePath;
   state.slideIndex = 0;
   savePresentationState(state);
}

void setSlideIndex(int index)
{
   s_presentationState.slideIndex = index;
   savePresentationState(s_presentationState);
}

bool isActive()
{
   return s_presentationState.active;
}

FilePath filePath()
{
   return s_presentationState.filePath;
}

FilePath directory()
{
   return s_presentationState.filePath.parent();
}

void clear()
{
   savePresentationState(PresentationState());
}

json::Value asJson()
{
   json::Object stateJson;
   stateJson["active"] = s_presentationState.active;
   stateJson["pane_caption"] = s_presentationState.paneCaption;
   stateJson["file_path"] = module_context::createAliasedPath(
                                                s_presentationState.filePath);
   stateJson["slide_index"] = s_presentationState.slideIndex;
   return stateJson;
}

Error initialize()
{
   loadPresentationState();
   return Success();
}

} // namespace state
} // namespace presentation
} // namespace modules
} // namesapce session

