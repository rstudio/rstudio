/*
 * SatelliteController.mm
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

// TODO: initial load performance issue
//  - has to be related to initial R process startup time since
//    it doesn't happen for reload but does for reload project
//    not unique to safari, git bisect for issues introduced?


// TODO: window positioning and persistance for satellites
// TODO: other windows types (minimal window, std browser window)
// TODO Cmd+W and other stuff from main window

// TODO: more robust wait for server to be ready (currently wait for 100ms)

// TOOD: file downloading (force preview on mac)
// TODO: main menu
// TODO: GwtCallbacks
// TODO: about box
// TODO: check for updates

// TODO: system shutdown notice
// TODO: webkit version check

// TODO: enter "boink" in vcs view
// TODO: strange layout of console progress dialog

// TODO: improve system shutdown handling (confirm our current logic
//       is airtight and return ShutdownLater (with subsequent callback)
//       rather than ShutdownCancel)

#import "SatelliteController.h"

#import "GwtCallbacks.h"

@implementation SatelliteController

// Inject our script ojbect when the window object becomes available
- (void) webView: (WebView*) webView
didClearWindowObject:(WebScriptObject *)windowObject
        forFrame:(WebFrame *)frame
{
   // only set the Desktop object for the top level frame
   if (frame == [webView mainFrame])
   {
      // register objective-c objects with javascript
      [self registerDesktopObject];
   }
}

- (BOOL) windowShouldClose: (id) sender
{
   id win = [webView_ windowScriptObject];
   [win evaluateWebScript:
      @"if (window.notifyRStudioSatelliteClosing) "
      "   window.notifyRStudioSatelliteClosing();"];
   
   return YES;
}

- (void) windowDidBecomeMain: (NSNotification *) notification
{
   id win = [webView_ windowScriptObject];
   [win evaluateWebScript:
      @"if (window.notifyRStudioSatelliteReactivated) "
      "   window.notifyRStudioSatelliteReactivated(null);"];
}

@end
