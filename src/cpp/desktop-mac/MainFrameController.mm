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
#include <core/RegexUtils.hpp>
#include <core/SafeConvert.hpp>
#include "Options.hpp"

#import "GwtCallbacks.h"
#import "MainFrameMenu.h"
#import "Utils.hpp"

#include "SessionLauncher.hpp"


using namespace rstudio;

@implementation MainFrameController

static MainFrameController* instance_;

// context for tracking all running applications
const static NSString *kRunningApplicationsContext = @"RunningAppsContext";

namespace
{

bool setWindowGeometry(NSWindow* window, NSString* geometry)
{
   // split geometry into individual values
   NSArray* geom = [geometry componentsSeparatedByString:@","];
   if ([geom count] < 4)
      return false;
   
   // parse into individual values
   NSRect frame;
   frame.origin.x    = [[geom objectAtIndex: 0] intValue];
   frame.origin.y    = [[geom objectAtIndex: 1] intValue];
   frame.size.height = [[geom objectAtIndex: 2] intValue];
   frame.size.width  = [[geom objectAtIndex: 3] intValue];
   
   // apply parsed geometry
   [window setFrame: frame display: TRUE];
   return true;
}

} // anonymous namespace

+ (MainFrameController*) instance
{
   return instance_;
}

- (id) initWithURL: (NSURL*) url openFile: (NSString*) openFile
{
   if (self = [super initWithURLRequest: [NSURLRequest requestWithURL: url]
                                   name: nil
                             clientName: nil
                  allowExternalNavigate: false])
   {
      // initialize the global instance
      instance_ = self;
      
      // initialize flags
      quitConfirmed_ = NO;
      firstWorkbenchInitialized_ = NO;
      pendingProject_ = nil;
      
      // retain openFile request
      if (openFile)
         openFile_ = [openFile retain];
      
      // create the main menu
      menu_ = [[MainFrameMenu alloc] init];
      
      bool hasInitialGeometry = false;
      NSArray* args = [[NSProcessInfo processInfo] arguments];
      for (NSUInteger i = 0; i < [args count]; i++)
      {
         NSString* str = [args objectAtIndex: i];
         if ([str isEqualToString: kInitialGeometryArg] &&
             (i + 1) < [args count])
         {
            hasInitialGeometry = setWindowGeometry(
               [self window], [args objectAtIndex: i+1]);
         }
      }
      
      // auto-save window position unless we're using manually supplied geometry
      if (!hasInitialGeometry)
      {
         [self setWindowFrameAutosaveName: @"RStudio"];
      }
      
      // set title
      [[self window] setTitle: @"RStudio"];
      
      // set proxy icon (commented out until we can actually implement the icon's behavior)
      /*
      [[self window] setRepresentedURL: url];
      [[[self window] standardWindowButton: NSWindowDocumentIconButton] setImage: [NSImage imageNamed: @"rstudio"]];
      */
      
      // set dock tile for application
      dockTile_ = [[DockTileView alloc] init];
      [[NSApp dockTile] setContentView: dockTile_];
      [[NSApp dockTile] display];
      
      // set primary fullscreen mode
      desktop::utils::enableFullscreenMode([self window], true);
      
      // webkit version check
      NSString* userAgent = [webView_
               stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];
      [self checkWebkitVersion: userAgent];
      
      // signup for changes in the list of running applications
      [[NSWorkspace sharedWorkspace] addObserver:self
                                      forKeyPath:@"runningApplications"
                                         options:NSKeyValueObservingOptionNew
                                         context:&kRunningApplicationsContext];
   }
   
   return self;
}

- (void) dealloc
{
   // unsubscribe to changes in the list of running applications
   [[NSWorkspace sharedWorkspace] removeObserver:self
                                      forKeyPath:@"runningApplications"
                                         context:&kRunningApplicationsContext];
   
   instance_ = nil;
   [dockTile_ release];
   [menu_ release];
   [openFile_ release];
   [super dealloc];
}

- (void) onWorkbenchInitialized
{
   // reset state (in case this occurred in response to a manual reload
   // or reload for a new project context)
   quitConfirmed_ = NO;

   // determine whether we should show a DockTile label
   [self updateDockTileShowLabel];
   
   // see if there is a project dir to display in the titlebar
   NSString* projectDir = [self evaluateJavaScript:
                                @"window.desktopHooks.getActiveProjectDir()"] ;
   if ([projectDir length] > 0)
   {
      [self setWindowTitle: projectDir];
      [self updateDockTile: projectDir];
   }
   else
   {
      [self setWindowTitle: nil];
      [self updateDockTile: nil];
   }
   
   // open file if requested for first workbench
   if (!firstWorkbenchInitialized_)
   {
      if (openFile_)
         [self openFileInRStudio: openFile_];
      
      firstWorkbenchInitialized_ = YES;
   }
}

- (void) setWindowTitle: (NSString*) title
{
   if (title == nil)
      title = @"RStudio";
   else
      title = [title stringByAppendingString: @" - RStudio"];
   
   [[self window] setTitle: title];
}

- (void) setPendingProject: (NSString*) path
{
   pendingProject_ = [path retain];
}


// whenever the list of running applications changes then check to see
// whether we should show project name labels on our dock tile (do it
// if there is more than one instance of RStudio active)
- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context
{
   if (context == &kRunningApplicationsContext)
      [self updateDockTileShowLabel];
}


- (void) updateDockTile: (NSString*) projectDir
{
   if (projectDir != nil)
      [dockTile_ setLabel: [projectDir lastPathComponent]];
   else
      [dockTile_ setLabel: nil];
   
   [[NSApp dockTile] display];
}

- (void) updateDockTileShowLabel
{
   if ([[NSRunningApplication runningApplicationsWithBundleIdentifier:
         [[NSBundle mainBundle] bundleIdentifier]] count] > 1) {
      [dockTile_ setShowLabel: TRUE];
   }
   else {
      [dockTile_ setShowLabel: FALSE];
   }
   
   [[NSApp dockTile] display];
}

std::string jsEscape(std::string str)
{
   boost::algorithm::replace_all(str, "\\", "\\\\");
   boost::algorithm::replace_all(str, "\"", "\\\"");
   boost::algorithm::replace_all(str, "\n", "\\n");
   return str;
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
   filename = jsEscape(filename);
   
   // execute the openFile command
   std::string js = "window.desktopHooks.openFile(\"" + filename + "\")";
   [self evaluateJavaScript: [NSString stringWithUTF8String: js.c_str()]];
}

// evaluate R command
- (void) evaluateRCommand: (NSString*) cmd
{
   std::string rCmd = [cmd UTF8String];
   rCmd = jsEscape(rCmd);
   std::string js = "window.desktopHooks.evaluateRCmd(\"" + rCmd + "\")";
   [self evaluateJavaScript: [NSString stringWithUTF8String: js.c_str()]];
}

- (void) initiateQuit
{
   [[self window] performClose: self];
}

- (void) quit
{
   quitConfirmed_ = YES;
   if (pendingProject_ != nil)
   {
      [gwtCallbacks_ openProjectInOverlaidNewWindow: pendingProject_];
      [pendingProject_ release];
      pendingProject_ = nil;
      [NSThread sleepForTimeInterval:1.0f];
   }
   [[self window] performClose: self];
}

- (void) windowDidLoad
{
    [super windowDidLoad];
    
    
}

- (void) checkWebkitVersion: (NSString*) userAgent
{
   using namespace rstudio::core;
   
   // parse version info out of user agent string
   boost::regex re("^.*?AppleWebKit/(\\d+).*$");
   boost::smatch match;
   std::string userAgentStr([userAgent UTF8String]);
   if (regex_utils::match(userAgentStr, match, re))
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
             @"In order to ensure that all RStudio features work correctly "
             @"please run System Update to install a more recent version "
             @"of Safari.");
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
