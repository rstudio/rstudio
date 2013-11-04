

#import <Cocoa/Cocoa.h>

#import <Webkit/WebUIDelegate.h>


#import "Options.hpp"
#import "WebViewController.h"
#import "GwtCallbacks.h"
#import "MainFrameMenu.h"
#import "SatelliteController.h"

// TODO: enable javascript alerts

struct PendingSatelliteWindow
{
   PendingSatelliteWindow()
      : name(), width(-1), height(-1)
   {
   }
   
   PendingSatelliteWindow(std::string name, int width, int height)
      : name(name), width(width), height(height)
   {
   }
   
   bool empty() const { return name.empty(); }
   
   std::string name;
   int width;
   int height;
};


@implementation WebViewController

static NSMutableDictionary* namedWindows_;
static PendingSatelliteWindow pendingWindow_;

+ (void) initialize
{
   namedWindows_ = [[NSMutableDictionary alloc] init];
   pendingWindow_ = PendingSatelliteWindow();
}

+ (void) activateSatelliteWindow: (NSString*) name
{
   WebViewController* controller = [namedWindows_ objectForKey: name];
   if (controller)
      [[controller window] makeKeyAndOrderFront: self];
}

+ (void) prepareForSatelliteWindow: (NSString*) name
                             width: (int) width
                            height: (int) height
{
   pendingWindow_ = PendingSatelliteWindow([name UTF8String], width, height);
}



- (WebView*) webView
{
   return webView_;
}

- (void) dealloc
{
   [name_ release];
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
                    name: (NSString*) name
{
   // create window and become it's delegate
   NSRect frameRect =  NSMakeRect(20, 20, 1024, 768);
   NSWindow* window = [[[NSWindow alloc] initWithContentRect: frameRect
                                          styleMask: NSTitledWindowMask |
                                                     NSClosableWindowMask |
                                                     NSResizableWindowMask |
                                                     NSMiniaturizableWindowMask
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
      [webView_ setResourceLoadDelegate: self];
      
      // load the request
      [[webView_ mainFrame] loadRequest: request];
      
      // add the webview to the window
      [window setContentView: webView_];
      
      // bring the window to the front
      [window makeKeyAndOrderFront: self];
      
      // speciql treatment for named windows
      if (name)
      {
         // track it (for reactivation)
         name_ = [name copy];
         [namedWindows_ setValue: self forKey: name_];
         
         // auto save positiom
         [self setWindowFrameAutosaveName: name_];
      }
   }
   
   return self;
}

- (void) windowDidLoad
{
   [super windowDidLoad];
   
   // more post-load initialization
}


// WebViewController is a self-freeing object so free it when the window closes
- (void)windowWillClose:(NSNotification *) notification
{
   // if we were named then remove from the tracker
   if (name_)
      [namedWindows_ removeObjectForKey: name_];
   
   [self autorelease];
}

// Handle new window request by creating another controller
- (WebView *) webView: (WebView *) sender
              createWebViewWithRequest:(NSURLRequest *)request
{
   // check for a pending satellite request
   if (!pendingWindow_.empty())
   {
      // capture and then clear the pending window
      PendingSatelliteWindow pendingWindow = pendingWindow_;
      pendingWindow_ = PendingSatelliteWindow();
      
      // get the name
      NSString* name =
        [NSString stringWithUTF8String: pendingWindow.name.c_str()];
     
      // check for an existing window
      WebViewController* controller = [namedWindows_ objectForKey: name];
      if (controller)
      {
         [[controller window] makeKeyAndOrderFront: self];
         return nil;
      }
      else
      {
         // self-freeing so don't auto-release
         SatelliteController* satelliteController =
         [[SatelliteController alloc] initWithURLRequest: request
                                                    name: name];
         
         [[satelliteController window]
                  cascadeTopLeftFromPoint: NSMakePoint(10, 5)];
         
         return [satelliteController webView];
      }
   }
   else
   {
      // self-freeing so don't auto-release
      WebViewController * webViewController =
               [[WebViewController alloc] initWithURLRequest: request
                                                        name: nil];
      return [webViewController webView];
   }
}

- (NSURLRequest*) webView:(WebView *) sender
                  resource:(id) identifier
                  willSendRequest:(NSURLRequest *)request
                  redirectResponse:(NSURLResponse *) redirectResponse
                  fromDataSource:(WebDataSource *) dataSource
{
   NSMutableURLRequest *mutableRequest = [[request mutableCopy] autorelease];
   std::string secret = desktop::options().sharedSecret();
   [mutableRequest setValue: [NSString stringWithUTF8String: secret.c_str()]
                   forHTTPHeaderField:@"X-Shared-Secret"];
   return mutableRequest;
}

- (void) registerDesktopObject
{
   id win = [webView_ windowScriptObject];
   GwtCallbacks* gwtCallbacks = [[[GwtCallbacks alloc] init] autorelease];
   [win setValue: gwtCallbacks forKey:@"desktop"];
}

@end



