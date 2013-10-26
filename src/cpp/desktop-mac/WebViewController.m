

#import <Cocoa/Cocoa.h>

#import <Webkit/WebUIDelegate.h>

#import "WebViewController.h"

#import "GwtCallbacks.h"

// TODO: enable javascript alerts

@implementation WebViewController

- (WebView*) webView
{
   return webView_;
}

- (void) dealloc
{
   [webView_ release];
   [super dealloc];
}

- (id) init
{
   @throw [NSException exceptionWithName: @"WebViewControllerInitialization"
                       reason: @"Use initWithURLRequest"
                       userInfo: nil];
}

- (id)initWithURLRequest: (NSURLRequest*) request
{
   // create window and become it's delegate
   NSRect frameRect =  NSMakeRect(20, 20, 1024, 768);
   NSWindow * window = [[[NSWindow alloc] initWithContentRect: frameRect
                                          styleMask: NSTitledWindowMask |
                                                     NSClosableWindowMask |
                                                     NSResizableWindowMask
                                          backing: NSBackingStoreBuffered
                                          defer: NO] autorelease];   
   [window setDelegate: self];
   [window setTitle: @"RStudio"];
   
   // initialize superclass then continue
   if (self = [super initWithWindow: window])
   {
      // create web view, save it as a member, and register as it's delegate,
      webView_ = [[WebView alloc] initWithFrame: frameRect];
      [webView_ setUIDelegate: self];
      [webView_ setFrameLoadDelegate: self];
      
      // load the request
      [[webView_ mainFrame] loadRequest: request];
      
      // add the webview to the window
      [window setContentView: webView_];
      
      // bring the window to the front
      [window makeKeyAndOrderFront: nil];
   }
   
   return self;
}

// WebViewController is a self-freeing object so free it when the window closes
- (void)windowWillClose:(NSNotification *) notification
{
   [self autorelease];
}

// Handle new window request by creating another controller
- (WebView *) webView: (WebView *) sender
              createWebViewWithRequest:(NSURLRequest *)request
{
   // self-freeing so don't auto-release
   WebViewController * webViewController =
            [[WebViewController alloc] initWithURLRequest: request];
   return [webViewController webView];
}


// Inject our script ojbect when the window object becomes available
- (void) webView: (WebView*) webView
         didClearWindowObject:(WebScriptObject *)windowObject
         forFrame:(WebFrame *)frame
{
   // only set the Desktop object for the top level frame
   if ([frame parentFrame] == nil) {
      
      // register objective c with webkit
      id win = [webView windowScriptObject];
      GwtCallbacks* gwtCallbacks = [[[GwtCallbacks alloc] init ] autorelease];
      [win setValue: gwtCallbacks forKey:@"Desktop"];
      
      // execute some js
      NSString *href = [win evaluateWebScript:@"window.location.href"];
      NSLog(@"href: %@",href);
   }
}


@end



