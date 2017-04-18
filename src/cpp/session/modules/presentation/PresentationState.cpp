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
#include <session/projects/SessionProjects.hpp>

#include "Tutorial.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {
namespace state {

namespace {
      
struct PresentationState
{
   PresentationState()
      : active(false), isTutorial(false), slideIndex(0)
   {
   }

   bool active;
   std::string paneCaption;
   bool isTutorial;
   FilePath filePath;
   int slideIndex;
   FilePath viewInBrowserPath; // not saved, is created on demand
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
   settings.set("is-tutorial", state.isTutorial);
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
      s_presentationState.isTutorial = settings.getBool("is-tutorial");
      s_presentationState.filePath = fromPersistentPath(settings.get("file-path"));
      s_presentationState.slideIndex = settings.getInt("slide-index", 0);
   }
   else
   {
      s_presentationState = PresentationState();
   }
}

} // anonymous namespace


void init(const FilePath& filePath,
          const std::string& caption,
          bool isTutorial)
{
   PresentationState state;
   state.active = true;
   state.paneCaption = caption;
   state.isTutorial = isTutorial;
   state.filePath = filePath;
   state.slideIndex = 0;
   savePresentationState(state);
}

void setSlideIndex(int index)
{
   s_presentationState.slideIndex = index;
   savePresentationState(s_presentationState);
}

void setCaption(const std::string& caption)
{
   s_presentationState.paneCaption = caption;
   savePresentationState(s_presentationState);
}

bool isActive()
{
   return s_presentationState.active;
}

bool isTutorial()
{
   return s_presentationState.isTutorial;
}

FilePath filePath()
{
   return s_presentationState.filePath;
}

FilePath directory()
{
   return s_presentationState.filePath.parent();
}

FilePath viewInBrowserPath()
{
   if (s_presentationState.viewInBrowserPath.empty())
   {
      FilePath viewDir = module_context::tempFile("view", "dir");
      Error error = viewDir.ensureDirectory();
      if (!error)
      {
         s_presentationState.viewInBrowserPath =
                                    viewDir.childPath("presentation.html");
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   return s_presentationState.viewInBrowserPath;
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
   stateJson["is_tutorial"] = s_presentationState.isTutorial;
   stateJson["file_path"] = module_context::createAliasedPath(
                                                s_presentationState.filePath);
   stateJson["slide_index"] = s_presentationState.slideIndex;
   return stateJson;
}

Error initialize()
{
   // attempt to load any cached state
   loadPresentationState();

   // call tutorial init
   return initializeTutorial();
}

} // namespace state
} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

