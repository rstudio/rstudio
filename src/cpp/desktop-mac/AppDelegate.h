/*
 * AppDelegate.h
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

#import <AppKit/NSApplication.h>

#include <core/FilePath.hpp>

#import "ActivationOverlay.h"

@interface AppDelegate : NSObject <NSApplicationDelegate> {
   NSString* openFile_;
   NSMenu* dockMenu_;
   BOOL initialized_;
   rstudio::core::FilePath confPath_;
   rstudio::core::FilePath sessionPath_;
   std::string filename_;
}

- (void) launchFirstSession;

@end

