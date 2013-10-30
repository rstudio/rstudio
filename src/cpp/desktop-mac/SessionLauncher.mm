/*
 * SessionLauncher.mm
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


#import "SessionLauncher.hpp"

#import "MainFrameController.h"

using namespace core;

namespace desktop {
   
SessionLauncher& sessionLauncher()
{
   static SessionLauncher instance;
   return instance;
}
   
void SessionLauncher::init(const core::FilePath& sessionPath,
                           const core::FilePath& confPath)
{
   sessionPath_ = sessionPath;
   confPath_ = confPath;
}
   
Error SessionLauncher::launchFirstSession(const std::string& filename)
{

   // load the main window
   NSURL *url = [NSURL URLWithString: @"http://localhost:8787"];
   [[MainFrameController alloc] initWithURL: url];
   
   
   // activate the app
   [NSApp activateIgnoringOtherApps: YES];
   
   return Success();
}
   
std::string SessionLauncher::launchFailedErrorMessage()
{
   return std::string();
}
   
void SessionLauncher::cleanupAtExit()
{
      
}


} // namespace desktop

