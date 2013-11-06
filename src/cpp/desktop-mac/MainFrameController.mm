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

#include <boost/regex.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>

#import "GwtCallbacks.h"
#import "MainFrameMenu.h"
#import "Utils.hpp"

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
      menu_ = [[MainFrameMenu alloc] init];
      
      // auto-save window position
      [self setWindowFrameAutosaveName: @"RStudio"];
      
      // set title
      [[self window] setTitle: @"RStudio"];
      
      // set primary fullscreen mode
      desktop::utils::enableFullscreenMode([self window], true);
      
      // webkit version check
      NSString* userAgent = [webView_
               stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];
      [self checkWebkitVersion: userAgent];

   }
   
   return self;
}

- (void) dealloc
{
   instance_ = nil;
   [menu_ release];
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


- (id) evaluateJavaScript: (NSString*) js
{
   id win = [webView_ windowScriptObject];
   return [win evaluateWebScript: js];
}

- (id) invokeCommand: (NSString*) command
{
   [[self window] makeKeyAndOrderFront: self];
   
   return [self evaluateJavaScript: [NSString stringWithFormat: @"window.desktopHooks.invokeCommand(\"%@\");",
                                     command]];
}

- (BOOL) isCommandEnabled: (NSString*) command
{
   return [[self evaluateJavaScript: [NSString stringWithFormat: @"window.desktopHooks.isCommandEnabled(\"%@\");",
                                     command]] boolValue];
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

- (void) checkWebkitVersion: (NSString*) userAgent
{
   // parse version info out of user agent string
   boost::regex re("^.*?AppleWebKit/(\\d+).*$");
   boost::smatch match;
   if (boost::regex_match(std::string([userAgent UTF8String]), match, re))
   {
      int version = core::safe_convert::stringTo<int>(match[1], 0);
      if (version < 534)
      {
         desktop::utils::showMessageBox(
             NSWarningAlertStyle,
             @"Older Version of Safari Detected",
             @"RStudio uses the Safari WebKit browser engine for rendering "
             @"its user interface. The minimum required version of Safari is "
             @"5.1 and an earlier version was detected on your system.\n\n"
             @"Please update to a more recent version of Safari to ensure that "
             @"all RStudio features work correctly.");
      }
   }
}

// Inject our script object when the window object becomes available
- (void) webView: (WebView*) webView
didClearWindowObject:(WebScriptObject *)windowObject
        forFrame:(WebFrame *)frame
{
   // only set the Desktop object for the top level frame
   if (frame == [webView mainFrame])
   {
      // register desktop object
      [self registerDesktopObject];
      
      // register main menu callback
      WebScriptObject* win = [webView_ windowScriptObject];
      [win setValue: menu_ forKey:@"desktopMenuCallback"];
   }
}

- (void) windowDidBecomeMain: (NSNotification *) notification
{
   if ([self hasDesktopObject])
      [self invokeCommand: @"vcsRefreshNoError"];
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

- (BOOL) performKeyEquivalent: (NSEvent *) theEvent
{
   NSString* chr = [theEvent charactersIgnoringModifiers];
   NSUInteger mod = [theEvent modifierFlags] & NSDeviceIndependentModifierFlagsMask;
   
   if ([chr isEqualToString: @"w"] && mod == NSCommandKeyMask)
   {
      if ([self isCommandEnabled: @"closeSourceDoc"])
      {
         [self invokeCommand: @"closeSourceDoc"];
      }
      else
      {
         [[webView_ window] performClose: self];
      }
      return YES;
   }
   else if (([chr isEqualToString: @"0"] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @"="] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @"-"] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @","] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @"h"] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @"h"] && mod == (NSCommandKeyMask | NSAlternateKeyMask)) ||
            ([chr isEqualToString: @"m"] && mod == NSCommandKeyMask) ||
            ([chr isEqualToString: @"m"] && mod == (NSCommandKeyMask | NSAlternateKeyMask)))
   {
      // These are shortcuts that only exist on the menu, so they must be invoked there rather
      // than letting the in-page shortcut manager handle it.
      // It's possible we could let all key-equivs get dispatched through the menu, but our Qt
      // desktop code works pretty hard to avoid that and I can't remember why (quite likely
      // to do with copy/paste I think). Safer to just keep the behavior the same except for
      // these few cases that are different.
      if ([[NSApp mainMenu] performKeyEquivalent: theEvent])
         return YES;
   }
   
   return [super performKeyEquivalent: theEvent];
}


@end
