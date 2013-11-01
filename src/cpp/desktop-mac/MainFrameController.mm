/*
 * MainFrameController.mm
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

#import "MainFrameController.h"

#import "GwtCallbacks.h"
#import "MenuCallbacks.h"

@implementation MainFrameController

- (id) initWithURL: (NSURL*) url
{
   if (self = [super initWithURLRequest: [NSURLRequest requestWithURL: url]])
   {
      // create the main menu
      id menubar = [[NSMenu new] autorelease];
      id appMenuItem = [[NSMenuItem new] autorelease];
      [menubar addItem: appMenuItem];
      [NSApp setMainMenu: menubar];
      id appMenu = [[NSMenu new] autorelease];
      id appName = [[NSProcessInfo processInfo] processName];
      id quitTitle = [@"Quit " stringByAppendingString:appName];
      id quitMenuItem = [[[NSMenuItem alloc] initWithTitle:quitTitle
                                                    action:@selector(terminate:)
                                             keyEquivalent:@"q"] autorelease];
      [appMenu addItem: quitMenuItem];
      [appMenuItem setSubmenu: appMenu];
      

      // auto-save window position
      [self setWindowFrameAutosaveName: @"RStudio"];
   }
   
   return self;
}


- (void) windowDidLoad
{
    [super windowDidLoad];
    
    
}

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
      [self registerDesktopMenuCallbackObject];
      
      
      // execute some js
      //NSString *href = [win evaluateWebScript:@"window.location.href"];
      //NSLog(@"href: %@",href);
   }
}


@end
