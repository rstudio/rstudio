

#import <Cocoa/Cocoa.h>

#import "WebViewController.h"


@interface AppDelegate : NSObject <NSApplicationDelegate> {
}
@end

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{

}

- (void)applicationWillTerminate:(NSNotification *)notification
{

}

- (BOOL) canBecomeKeyWindow
{
    return YES;
}

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication*)inSender
{
   return YES;
}



@end

// TODO: figure out how to do this locally
// defaults write NSGlobalDomain WebKitDeveloperExtras -bool true


int main(int argc, char* argv[])
{
   // Autorelease Pool:
   // Objects declared in this scope will be automatically
   // released at the end of it, when the pool is "drained".
   NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];

   // Create a shared app instance.
   // This will initialize the global variable
   // 'NSApp' with the application instance.
   [NSApplication sharedApplication];
   
   // App Delegate
   AppDelegate* appDelegate = [[[AppDelegate alloc] init] autorelease];
   [NSApp setDelegate: appDelegate];
   
   
   // create menubar
   id menubar = [[NSMenu new] autorelease];
   id appMenuItem = [[NSMenuItem new] autorelease];
   [menubar addItem:appMenuItem];
   [NSApp setMainMenu:menubar];
   id appMenu = [[NSMenu new] autorelease];
   id appName = [[NSProcessInfo processInfo] processName];
   id quitTitle = [@"Quit " stringByAppendingString:appName];
   id quitMenuItem = [[[NSMenuItem alloc] initWithTitle:quitTitle
                                                 action:@selector(terminate:) keyEquivalent:@"q"] autorelease];
   [appMenu addItem:quitMenuItem];
   [appMenuItem setSubmenu:appMenu];

   
   // Load content view
   NSString *urlAddress = @"http://localhost:8787/webkit.nocache.html";
   //NSString *urlAddress = @"http://www.google.com";
   NSURL *url = [NSURL URLWithString:urlAddress];
   NSURLRequest *requestObj = [NSURLRequest requestWithURL:url];
  
   WebViewController * windowController = [[WebViewController alloc] initWithURLRequest: requestObj];
   
   [windowController setWindowFrameAutosaveName: @"RStudio"];
   

   [NSApp activateIgnoringOtherApps: YES];
  
   [NSApp run];

   [pool drain];

   return (0);
}



