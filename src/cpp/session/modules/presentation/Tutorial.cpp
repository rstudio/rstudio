/*
 * PresentationOverlay.cpp
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

#include <core/Error.hpp>

#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "PresentationState.hpp"
#include "TutorialInstaller.hpp"
#include "SlideParser.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

void onSlideDeckChangedOverlay(const SlideDeck& slideDeck)
{
}

namespace {

bool readTutorialField(const FilePath& tutorialPath,
                       const std::pair<std::string,std::string>& field,
                       Tutorial* pTutorial)
{
   // ignore empty records
   if (field.first.empty())
      return true;

   std::string name = field.first;
   std::string value = field.second;
   if (boost::iequals(name, "slides"))
   {
      pTutorial->slidesPath = tutorialPath.complete(value);
   }
   else if (boost::iequals(name, "caption"))
   {
      pTutorial->caption = value;
   }
   else if (boost::iequals(name, "install"))
   {
      pTutorial->installFiles.push_back(value);
   }
   else if (boost::iequals(name, "installreadonly"))
   {
      pTutorial->installReadonlyFiles.push_back(value);
   }
   else
   {
      module_context::consoleWriteError(
               "Unrecognized field in TUTORIAL file: " + name + "\n");
   }
   return true;
}


bool readTutorialManifest(const FilePath& tutorialPath,
                          Tutorial* pTutorial,
                          std::string* pUserErrMsg)
{
   // look for the DCF file
   FilePath manifestPath = tutorialPath.childPath("TUTORIAL");
   if (!manifestPath.exists())
   {
      *pUserErrMsg = "TUTORIAL file not found at " +
                     module_context::createAliasedPath(manifestPath);
      return false;
   }

   // read it's contents
   std::string manifest;
   Error error = core::readStringFromFile(manifestPath,
                                          &manifest,
                                          string_utils::LineEndingPosix);
   if (error)
   {
      LOG_ERROR(error);
      *pUserErrMsg = error.summary();
      return false;
   }

   // parse the dcf file
   error = core::text::parseDcfFile(manifest,
                                    false,
                                    boost::bind(readTutorialField,
                                                 tutorialPath, _1, pTutorial),
                                    pUserErrMsg);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return true;
}

void initializeTutorial(const FilePath& tutorialPath)
{
   std::string errMsg;
   Tutorial tutorial;
   if (!readTutorialManifest(tutorialPath, &tutorial, &errMsg))
   {
      module_context::consoleWriteError(errMsg + "\n");
      return;
   }

   // if the slides path isn't the same as the current presentation
   // state then re-initialize it
   if (!presentation::state::isActive() ||
       (presentation::state::filePath() != tutorial.slidesPath))
   {
      presentation::state::init(tutorial.slidesPath, tutorial.caption, true);
   }
   else
   {
      presentation::state::setCaption(tutorial.caption);
   }

   // install the tutorial
   installTutorial(tutorialPath,
                   tutorial,
                   projects::projectContext().directory());
}

} // anonymous namespace

Error initializeTutorial()
{
   // bail if we aren't in server mode
   if (session::options().programMode() != kSessionProgramModeServer)
      return Success();

   // bail if there is no project
   if (!projects::projectContext().hasProject())
      return Success();

   // check for a tutorial tied to this project
   std::string tutorial = projects::projectContext().config().tutorialPath;
   if (!tutorial.empty())
   {
      FilePath tutorialPath = module_context::resolveAliasedPath(tutorial);
      if (tutorialPath.exists())
      {
         initializeTutorial(tutorialPath);
      }
      else
      {
         module_context::consoleWriteError(
           "Tutorial directory specified in project does not exist (" +
           module_context::createAliasedPath(tutorialPath) + ")\n");
      }
   }

   return Success();
}

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

