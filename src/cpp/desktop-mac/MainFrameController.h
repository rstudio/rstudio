/*
 * MainFrameController.h
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

#import "WebViewController.h"

#import "MainFrameMenu.h"
#import "DockTileView.h"

@interface MainFrameController : WebViewController {
   BOOL quitConfirmed_;
   BOOL firstWorkbenchInitialized_;
   NSString* openFile_;
   MainFrameMenu* menu_;
   DockTileView* dockTile_;
}

// access single instance
+ (MainFrameController*) instance;

// designated initializer
- (id) initWithURL: (NSURL*) url openFile: (NSString*) openFile;

// noification of workbench initialized
- (void) onWorkbenchInitialized;

// set the window title
- (void) setWindowTitle: (NSString*) title;

// open a file association file
- (void) openFileInRStudio: (NSString*) filename;

// evaluate javascript
- (id) evaluateJavaScript: (NSString*) js;

- (id) invokeCommand: (NSString*) command;

- (BOOL) isCommandEnabled: (NSString*) command;


// initiate a quit sequence
- (void) initiateQuit;

// quit for real
- (void) quit;

@end
