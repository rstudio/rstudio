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

// TODO: initial load performance issue - it takes about 1 second to
//       forceLoad the deferred binding for the source pane. no idea why.

// TODO: window positioning and persistance for satellites
//       (also verify we like the positioning of other windows)

// TODO: base64 download for ggivs
// TODO: toolbar for secondary window

// TODO: use unsupported file type in viewer to investigate download handling

// TODO: merge onto master

// TOOD: test on snow leopoard

// TODO: main menu

// TODO: filterText callback

// TODO: file dialogs

// TODO: message box dialogs

// TODO: undo and clipboard callbacks (undo/redo/cut/copy/paste)
//       (also cleanClipboard and copyImageToClipboard)

// TODO: about box

// TODO: check for updates

// TODO: enter "boink" in vcs view
// TODO: strange layout of console progress dialog


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
