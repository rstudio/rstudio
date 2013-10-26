

#import <Cocoa/Cocoa.h>

#import "WebViewController.h"

// TODO: figure out how to do this locally
// defaults write NSGlobalDomain WebKitDeveloperExtras -bool true



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

int main(int argc, char* argv[])
{
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



