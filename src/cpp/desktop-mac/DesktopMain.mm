
#include <iostream>

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>
#import <Webkit/WebUIDelegate.h>


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


@interface WebViewController : NSWindowController<NSWindowDelegate> {
   
}
@end

@implementation WebViewController

- (id)initWithWindow:(NSWindow *)window
{
   return [super initWithWindow: window];
}

- (void)windowWillClose:(NSNotification *)notification
{
   [self autorelease];
}

@end

@interface WebViewDelegate  : NSObject<NSWindowDelegate> {
}
@end

@implementation WebViewDelegate

- (WebView *)webView:(WebView *)sender createWebViewWithRequest:(NSURLRequest *)request
{
   // Style flags
   NSUInteger windowStyle =
   (NSTitledWindowMask |
    NSClosableWindowMask |
    NSResizableWindowMask);
   
   // Window bounds (x, y, width, height)
   NSRect windowRect = NSMakeRect(100, 100, 1024, 768);
   
   NSWindow * window = [[NSWindow alloc] initWithContentRect:windowRect
                                                   styleMask:windowStyle
                                                     backing:NSBackingStoreBuffered
                                                       defer:NO];
   [window autorelease];
   
   // Window controller
   WebViewController * windowController = [[WebViewController alloc] initWithWindow:window];
   [window setDelegate: windowController];
   
   WebView *webView = [[[WebView alloc] initWithFrame:NSMakeRect(100,100,1024,768)] autorelease];
   
   [[webView mainFrame] loadRequest:request];
   
   [webView setUIDelegate: self];
   
   [window setContentView:webView];
   
   [window setTitle: @"RStudio Popup"];
   [window makeKeyAndOrderFront: nil];

   
   
   return webView;
   
}



@end




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

   // Create a window:

   // Style flags
   NSUInteger windowStyle =
   (NSTitledWindowMask |
   NSClosableWindowMask |
   NSResizableWindowMask);

   // Window bounds (x, y, width, height)
   NSRect windowRect = NSMakeRect(100, 100, 1024, 768);

   NSWindow * window = [[NSWindow alloc] initWithContentRect:windowRect
   styleMask:windowStyle
   backing:NSBackingStoreBuffered
   defer:NO];
   [window autorelease];

  
  WebViewController * windowController = [[WebViewController alloc] initWithWindow:window];
   [window setDelegate: windowController];
   [windowController setWindowFrameAutosaveName: @"RStudio"];
   

   // App Delegate
   AppDelegate* appDelegate = [[[AppDelegate alloc] init] autorelease];
   [NSApp setDelegate: appDelegate];
   
   

   // Load content view
   NSString *urlAddress = @"http://www.google.com";
   NSURL *url = [NSURL URLWithString:urlAddress];
   NSURLRequest *requestObj = [NSURLRequest requestWithURL:url];
   WebView *webView = [[[WebView alloc] initWithFrame:NSMakeRect(100,100,1024,768)] autorelease];

   [[webView mainFrame] loadRequest:requestObj];
   [window setContentView:webView];
   
   // WebUIDelegate
   WebViewDelegate* webViewDelegate = [[[WebViewDelegate alloc] init] autorelease];
   [webView setUIDelegate: webViewDelegate];
   
  
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

   // Show window and run event loop
   [window setTitle: @"RStudio"];
   [window makeKeyAndOrderFront: nil];
   [NSApp activateIgnoringOtherApps: YES];

   [NSApp run];

   [pool drain];

   return (0);
}



