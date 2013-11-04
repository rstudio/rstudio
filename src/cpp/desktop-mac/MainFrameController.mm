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

#include <boost/algorithm/string/replace.hpp>

#include <core/FilePath.hpp>

#import "GwtCallbacks.h"
#import "MainFrameMenu.h"

#include "SessionLauncher.hpp"

@implementation MainFrameController

static MainFrameController* instance_;

+ (MainFrameController*) instance
{
   return instance_;
}

- (id) initWithURL: (NSURL*) url openFile: (NSString*) openFile
{
   if (self = [super initWithURLRequest: [NSURLRequest requestWithURL: url]
                                   name: nil])
   {
      // initialize the global instance
      instance_ = self;
      
      // initialize flags
      quitConfirmed_ = NO;
      firstWorkbenchInitialized_ = NO;
      
      // retain openFile request
      if (openFile)
         openFile_ = [openFile retain];
      
      // create the main menu
      id menubar = [[NSMenu new] autorelease];
      id appMenuItem = [[NSMenuItem new] autorelease];
      [menubar addItem: appMenuItem];
      [NSApp setMainMenu: menubar];
      id appMenu = [[NSMenu new] autorelease];
      id appName = [[NSProcessInfo processInfo] processName];
      id quitTitle = [@"Quit " stringByAppendingString:appName];
      id quitMenuItem = [[[NSMenuItem alloc] initWithTitle:quitTitle
                                                    action: NULL
                                             keyEquivalent:@"q"] autorelease];
      [quitMenuItem setTarget: self];
      [quitMenuItem setAction: @selector(initiateQuit:)];
      [quitMenuItem setEnabled: YES];
      [appMenu addItem: quitMenuItem];
      [appMenuItem setSubmenu: appMenu];
      
      // auto-save window position
      [self setWindowFrameAutosaveName: @"RStudio"];
      
      // set title
      [[self window] setTitle: @"RStudio"];
   }
   
   return self;
}

- (void) dealloc
{
   [openFile_ release];
   [super dealloc];
}

- (void) onWorkbenchInitialized
{
   // reset state (in case this occurred in response to a manual reload
   // or reload for a new project context)
   quitConfirmed_ = NO;

   // see if there is a project dir to display in the titlebar
   // if there are unsaved changes then resolve them before exiting
   NSString* projectDir = [self evaluateJavaScript:
                                @"window.desktopHooks.getActiveProjectDir()"] ;
   if ([projectDir length] > 0)
      [[self window] setTitle: [projectDir stringByAppendingString:
                                                            @" - RStudio"]];
   else
      [[self window] setTitle: @"RStudio"];
   
   // open file if requested for first workbench
   if (!firstWorkbenchInitialized_)
   {
      if (openFile_)
         [self openFileInRStudio: openFile_];
      
      firstWorkbenchInitialized_ = YES;
   }
   
   // TODO: check for updates
}

- (void) openFileInRStudio: (NSString*) openFile
{
   // must be absolute
   std::string filename = [openFile UTF8String];
   if (!core::FilePath::isRootPath(filename))
       return;
   
   // must exist and be a standard file rather than a directory
   core::FilePath filePath(filename);
   if (!filePath.exists() || filePath.isDirectory())
      return;
   
   // fixup for passing as a javascript string
   boost::algorithm::replace_all(filename, "\\", "\\\\");
   boost::algorithm::replace_all(filename, "\"", "\\\"");
   boost::algorithm::replace_all(filename, "\n", "\\n");
   
   // execute the openFile command
   std::string js = "window.desktopHooks.openFile(\"" + filename + "\")";
   [self evaluateJavaScript: [NSString stringWithUTF8String: js.c_str()]];
}


- (NSString*) evaluateJavaScript: (NSString*) js
{
   id win = [webView_ windowScriptObject];
   return [win evaluateWebScript: js];
}

- (BOOL) hasDesktopObject
{
   WebScriptObject* script = [webView_ windowScriptObject];
   if (script == nil)
      return NO;
   
   return [[script evaluateWebScript: @"!!window.desktopHooks"] boolValue];   
}

- (void) initiateQuit
{
   [[self window] performClose: self];
}

- (void) quit
{
   quitConfirmed_ = YES;
   [[self window] performClose: self];
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
   }
}

- (BOOL) windowShouldClose: (id) sender
{
   if (quitConfirmed_)
   {
      return YES;
   }
   else if (!desktop::sessionLauncher().sessionProcessActive())
   {
      return YES;
   }
   else if (![self hasDesktopObject])
   {
      return YES;
   }
   else
   {
      [self evaluateJavaScript: @"window.desktopHooks.quitR()"];
      return NO;
   }
}


@end
