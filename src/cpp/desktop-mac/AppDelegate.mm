

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import <AppKit/AppKit.h>

#import <Foundation/NSTask.h>

#import "AppDelegate.h"
#import "WebViewController.h"
#import "Utils.hpp"

using namespace core;

NSString* openFileCommandLineArgument()
{
   NSArray *arguments = [[NSProcessInfo processInfo] arguments];
   int count = [arguments count];
   if (count > 1) // executable name doesn't count as an argument
   {
      for (int i=(count-1); i>0; --i)
      {
         NSString* arg = [arguments objectAtIndex: i];
         if (![arg hasPrefix: @"-psn"]) // avoid process serial number arg
            return arg;
      }
   }
   
   return nil;
}

// PORT: From DesktopMain.cpp
NSString* verifyAndNormalizeFilename(NSString* filename)
{
   if (filename)
   {
      // resolve relative path
      std::string path([filename UTF8String]);
      if (!FilePath::isRootPath(path))
      {
         path = FilePath::safeCurrentPath(
                           FilePath("/")).childPath(path).absolutePath();
      }
      
      if (FilePath(path).exists())
         return [NSString stringWithUTF8String: path.c_str()];
      else
         return nil;
   }
   else
   {
      return nil;
   }
}

BOOL isProjectFilename(NSString* filename)
{
   if (!filename)
      return NO;
   
   FilePath filePath([filename UTF8String]);
   return filePath.exists() && filePath.extensionLowerCase() == ".rproj";
}

NSString* executablePath()
{
   FilePath exePath;
   Error error = core::system::executablePath(NULL, &exePath);
   if (error)
      LOG_ERROR(error);
   return [NSString stringWithUTF8String: exePath.absolutePath().c_str()];
}



@implementation AppDelegate

- (void)dealloc
{
   [openFile_ release];
   [super dealloc];
}

- (BOOL) application: (NSApplication *) theApplication
            openFile:(NSString *) filename
{
   // open file and application together
   if (!openFile_)
   {
      openFile_ = [filename copy];
   }
   // attemping to open a project in an existing instance, force a new instance
   else if (isProjectFilename(filename))
   {
      NSArray* args = [NSArray arrayWithObject: filename];
      [NSTask launchedTaskWithLaunchPath: executablePath()
                               arguments: args];
      
   }
   // attempt to open a file in an existing instance
   else
   {
      // TODO: open file in existing instance
      
      NSAlert *alert = [[[NSAlert alloc] init] autorelease];
      [alert setMessageText: [@"Open Existing:" stringByAppendingString: filename]];
      [alert runModal];
   }
   
   return YES;   
}

- (void) applicationDidFinishLaunching: (NSNotification *) aNotification
{
   // check for open file request (either apple event or command line)
   NSString* openFile = verifyAndNormalizeFilename(openFile_);
   if (!openFile)
      openFile = verifyAndNormalizeFilename(openFileCommandLineArgument());
   
   if (openFile)
   {
      
      NSAlert *alert = [[[NSAlert alloc] init] autorelease];
      [alert setMessageText: [@"Open New:" stringByAppendingString: openFile]];
      [alert runModal];
   }
   
   
   // create menubar
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
   
   
   // load the main window
   NSString *urlAddress = @"http://localhost:8787/webkit.nocache.html";
   NSURL *url = [NSURL URLWithString: urlAddress];
   NSURLRequest *request = [NSURLRequest requestWithURL: url];
   WebViewController * windowController =
   [[WebViewController alloc] initWithURLRequest: request];
   
   // remember size and position accross sessions
   [windowController setWindowFrameAutosaveName: @"RStudio"];
   
   // activate the app
   [NSApp activateIgnoringOtherApps: YES];
}

- (void) applicationWillTerminate: (NSNotification *) notification
{

}

- (BOOL) canBecomeKeyWindow
{
    return YES;
}

- (BOOL) applicationShouldTerminateAfterLastWindowClosed: (NSApplication*) s
{
   return YES;
}

@end
