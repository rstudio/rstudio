

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import <Cocoa/Cocoa.h>

#import "Utils.hpp"
#import "WebViewController.hpp"


@interface AppDelegate : NSObject <NSApplicationDelegate> {
}
@end

@implementation AppDelegate

- (void) applicationDidFinishLaunching: (NSNotification *) aNotification
{

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

using namespace core;
using namespace desktop;

int main(int argc, char* argv[])
{
   // initialize language environment variables
   utils::initializeLang();
 
   // initialize log
   core::system::initializeLog("rdesktop",
                               core::system::kLogLevelWarning,
                               desktop::utils::userLogPath());
   
   // ignore SIGPIPE
   Error error = core::system::ignoreSignal(core::system::SigPipe);
   if (error)
      LOG_ERROR(error);
   
   
   // initialize autorelease pool and application instance
   NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
   [NSApplication sharedApplication];
   
   // create our app delegate
   AppDelegate* appDelegate = [[[AppDelegate alloc] init] autorelease];
   [NSApp setDelegate: appDelegate];
   
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
  
   // run the event loop
   [NSApp run];

   // free the autorelease pool
   [pool drain];

   return EXIT_SUCCESS;
}



