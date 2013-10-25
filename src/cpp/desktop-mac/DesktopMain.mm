
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

// https://developer.apple.com/library/mac/documentation/AppleApplications/Conceptual/SafariJSProgTopics/Tasks/ObjCFromJavaScript.html
// https://developer.apple.com/library/mac/samplecode/CallJS/Introduction/Intro.html#//apple_ref/doc/uid/DTS10004241
@interface GwtCallbacks : NSObject {
   int theValue;
}
- (void) setTheValue:(int)value;
- (int)getTheValue;
@end

@implementation GwtCallbacks

- (id)init
{
   if (self = [super init])
   {
      theValue = 14;
      return self;
   }
   else
   {
      return nil;
   }
}

- (void) setTheValue:(int)value
{
   theValue = value;
}

- (int)getTheValue
{
   return theValue;
}

+ (NSString *) webScriptNameForSelector:(SEL)sel
{
   if (sel == @selector(setTheValue:))
      return @"setTheValue";
   
   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript:(SEL)sel
{
   return NO;
}

@end


@interface WebViewController : NSWindowController<NSWindowDelegate> {
   WebView* webView_;
}
-(WebView*) webView;
@end

@implementation WebViewController

- (WebView*) webView
{
   return webView_;
}

- (id)initWithURLRequest: (NSURLRequest*) request
{
   
   // Style flags
   NSUInteger windowStyle =
     (NSTitledWindowMask | NSClosableWindowMask | NSResizableWindowMask);
   
   // Window bounds (x, y, width, height)
   NSRect windowRect = NSMakeRect(100, 100, 1024, 768);
   
   NSWindow * window = [[[NSWindow alloc] initWithContentRect:windowRect
                                                   styleMask:windowStyle
                                                     backing:NSBackingStoreBuffered
                                                       defer:NO] autorelease];   
   [window setDelegate: self];

   
   webView_ = [[[WebView alloc] initWithFrame:NSMakeRect(100,100,1024,768)] autorelease];
   [webView_ setUIDelegate: self];
   [webView_ setFrameLoadDelegate: self];
   
   
   [[webView_ mainFrame] loadRequest:request];
   [window setContentView:webView_];
   
   [window setTitle: @"RStudio"];
   [window makeKeyAndOrderFront: nil];
   
   return [super initWithWindow: window];
}

- (void)windowWillClose:(NSNotification *)notification
{
   [self autorelease];
}

- (WebView *)webView:(WebView *)sender createWebViewWithRequest:(NSURLRequest *)request
{
   WebViewController * windowController = [[WebViewController alloc] initWithURLRequest: request];   
   return [windowController webView];
}


- (void)webView:(WebView *)webView windowScriptObjectAvailable:(WebScriptObject *)windowScriptObject {
   NSLog(@"%@ received %@", self, NSStringFromSelector(_cmd));
   
   // register objective c with webkit
   id win = [webView windowScriptObject];
   GwtCallbacks* gwtCallbacks = [[[GwtCallbacks alloc] init ] autorelease];
   [win setValue: gwtCallbacks forKey:@"Desktop"];
   
   // now call it
   NSString *href = [[webView windowScriptObject] evaluateWebScript:@"Desktop.getTheValue()"];
   NSLog(@"href: %@",href);
   
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
   //NSString *urlAddress = @"http://localhost:8787/webkit.nocache.html";
   NSString *urlAddress = @"http://www.google.com";
   NSURL *url = [NSURL URLWithString:urlAddress];
   NSURLRequest *requestObj = [NSURLRequest requestWithURL:url];
  
   WebViewController * windowController = [[WebViewController alloc] initWithURLRequest: requestObj];
   
   [windowController setWindowFrameAutosaveName: @"RStudio"];
   

   [NSApp activateIgnoringOtherApps: YES];
  
   [NSApp run];

   [pool drain];

   return (0);
}



