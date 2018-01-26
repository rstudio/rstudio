/*
 * Main.mm
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import <AppKit/NSApplication.h>
#import <Foundation/NSAutoreleasePool.h>

#import "AppDelegate.h"
#import "Utils.hpp"

using namespace rstudio;
using namespace rstudio::core;

int main(int argc, char* argv[])
{
   // initialize autorelease pool
   NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
   
   // clean duplicate environment variables (work around Yosemite bug)
   desktop::utils::cleanDuplicateEnvVars();
   
   // initialize language environment variables
   desktop::utils::initializeLang();
 
   // initialize prefs
   desktop::utils::initializeSystemPrefs();
   
   // initialize log
   core::system::initializeLog("rdesktop",
                               core::system::kLogLevelWarning,
                               desktop::utils::userLogPath());
   
   // ignore SIGPIPE
   Error error = core::system::ignoreSignal(core::system::SigPipe);
   if (error)
      LOG_ERROR(error);
   
   // initialize application instance
   NSApplication* app = [NSApplication sharedApplication];
   
   // create our app delegate
   AppDelegate* appDelegate = [[[AppDelegate alloc] init] autorelease];
   [app setDelegate: appDelegate];
   
   // run the event loop
   [app run];

   // free the autorelease pool
   [pool drain];

   return EXIT_SUCCESS;
}


