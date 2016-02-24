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
   if ([clientName_ hasPrefix: SOURCE_WINDOW_PREFIX])
   {
      // the source window has special close semantics (consider: should it also
      // have its own controller object?)
      if (![[win evaluateWebScript:@"window.rstudioReadyToClose"] boolValue])
      {
         [win evaluateWebScript: @"window.rstudioCloseSourceWindow();"];
         return NO;
      }
   }
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

- (BOOL) performKeyEquivalent: (NSEvent *) theEvent
{
   NSString* chr = [theEvent charactersIgnoringModifiers];
   NSUInteger mod = [theEvent modifierFlags] & NSDeviceIndependentModifierFlagsMask;

   // enable the preferences keyboard shortcut in satellites (brings up pref
   // dialog in main window)
   if (([chr isEqualToString: @","] && mod == NSCommandKeyMask))
   {
      if ([[NSApp mainMenu] performKeyEquivalent: theEvent])
         return YES;
   }
   
   return [super performKeyEquivalent: theEvent];
}

@end
