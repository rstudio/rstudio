/*
 * DesktopPosixApplication.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "DesktopPosixApplication.hpp"

#include <shared_core/FilePath.hpp>

#include <QFileOpenEvent>

#include "DesktopOptions.hpp"
#include "DesktopUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

bool PosixApplication::event(QEvent* pEvent)
{
   switch(pEvent->type())
   {
   case QEvent::FileOpen:
   {
      // get filename
      QString filename = static_cast<QFileOpenEvent*>(pEvent)->file();

      if (activationWindow() == nullptr)
      {
         // if we don't yet have an activation window then this is a startup
         // request -- save it so DesktopMain can pull it out later
         startupOpenFileRequest_ = filename;
      }
      else
      {
         // otherwise we are already running so this is an apple event
         // targeted at opening a file in an existing instance

         // if this is a project then re-post the request back to
         // another instance using the command line (this is to
         // circumvent the fact that the first RStudio application
         // launched on OSX gets all of the apple events). note that
         // we don't make this code conditional for __APPLE__ because
         // we'd need the same logic if other platforms started posting
         // FileOpen back to existing instances (e.g. via DDE)

         FilePath filePath(filename.toUtf8().constData());
         if (filePath.exists() && filePath.getExtensionLowerCase() == ".rproj")
         {
            std::vector<std::string> args;
            args.push_back(filePath.getAbsolutePath());
            pAppLauncher_->launchRStudio(args);
         }
         else
         {
            openFileRequest(filename);
         }
      }

      return true;
   }

   default:
      return QtSingleApplication::event(pEvent);
   }
}

} // namespace desktop
} // namespace rstudio
