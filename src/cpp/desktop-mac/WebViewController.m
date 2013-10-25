

#import <Cocoa/Cocoa.h>

#import <Webkit/WebUIDelegate.h>

#import "WebViewController.h"

#import "GwtCallbacks.h"

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



