
#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif

#import <Cocoa/Cocoa.h>

#import <WebKit/WebFrame.h>
#import <Webkit/WebUIDelegate.h>

#import "Options.hpp"
#import "WebViewController.h"
#import "GwtCallbacks.h"
#import "MainFrameMenu.h"
#import "SatelliteController.h"
#import "SecondaryWindowController.h"
#import "Utils.hpp"
#import "WebViewWithKeyEquiv.h"
#import "FileDownloader.h"
#import "MainFrameController.h"

using namespace rstudio;

struct PendingWindow
{
   PendingWindow()
      : name(), x(-1), y(-1), width(-1), height(-1)
   {
   }
   
   PendingWindow(std::string name, int x, int y, int width, int height)
      : name(name), x(x), y(y), width(width), height(height), isSatellite(true)
   {
   }
   
   PendingWindow(std::string name, bool allowExternalNavigation)
      : name(name), allowExternalNavigate(allowExternalNavigation),
        isSatellite(false)
   {
   }
   
   bool empty() const { return name.empty(); }
   
   std::string name;
   int x;
   int y;
   int width;
   int height;
   bool allowExternalNavigate;
   bool isSatellite;
};

NSString* authorityFromUrl (NSString* url)
{
   // extract the authority (domain and port) from the URL:
   // e.g. for http://foo:8402/bar/baz.html, extract http://foo:8402/
   NSURL* authorityUrl = [NSURL URLWithString: 
        [url stringByAddingPercentEscapesUsingEncoding: NSUTF8StringEncoding]];
   NSString* port = @"";
   if ([authorityUrl port] != nil) {
      port = [NSString stringWithFormat: @":%@", [authorityUrl port]];
   }
   NSString* prefix = [NSString stringWithFormat: @"%@://%@%@/",
                       [authorityUrl scheme], [authorityUrl host], port];
   return prefix;
}

// get access to private webview zoom apis
@interface WebView (Zoom)
- (IBAction)zoomPageIn:(id)sender;
- (IBAction)zoomPageOut:(id)sender;
- (IBAction)resetPageZoom:(id)sender;
@end

@implementation WebViewController

static NSMutableDictionary* namedWindows_;
static PendingWindow pendingWindow_;

+ (void) initialize
{
   namedWindows_ = [[NSMutableDictionary alloc] init];
   pendingWindow_ = PendingWindow();
}

+ (void) prepareForSatelliteWindow: (NSString*) name
                                 x: (int) x
                                 y: (int) y
                             width: (int) width
                            height: (int) height
{
   pendingWindow_ = PendingWindow([name UTF8String], x, y, width, height);
}

+ (void) prepareForNamedWindow: (NSString *) name
         allowExternalNavigate: (bool) allow
{
   pendingWindow_ = PendingWindow([name UTF8String], allow);
}

+ (WebViewController*) windowNamed: (NSString*) name
{
   return [namedWindows_ objectForKey: name];
}

+ (WebViewController*) activeDesktopController
{
   // get the current key window; some satellites (right now, just the source
   // window) have a desktop object and can handle commands themselves
   NSWindow* keyWindow = [NSApp keyWindow];
   NSWindowController* keyController = [keyWindow windowController];
   if ([keyController isMemberOfClass: [SatelliteController class]])
   {
      SatelliteController* controller = (SatelliteController*)keyController;
      if ([controller hasDesktopObject])
      {
         return controller;
      }
   }
   
   // current key window isn't a webview or doesn't have desktop hooks; use the
   // main window
   return [MainFrameController instance];
}

+ (void) activateNamedWindow: (NSString*) name
{
   WebViewController* controller = [self windowNamed: name];
   if (controller)
      [[controller window] makeKeyAndOrderFront: self];
}

+ (void) closeNamedWindow: (NSString *)name
{
   WebViewController* controller = [self windowNamed: name];
   if (controller)
      [[controller window] close];
}

- (WebView*) webView
{
   return webView_;
}

- (void) dealloc
{
   [name_ release];
   [webView_ release];
   [baseUrl_ release];
   [viewerUrl_ release];
   [shinyDialogUrl_ release];
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
              clientName: (NSString*) clientName
   allowExternalNavigate: (bool) allowExternalNavigate
{
   // record base url
   baseUrl_ = [[request URL] retain];
   
   // indicate whether this window is permitted to load external (non-local)
   // URLs
   allowExternalNav_ = allowExternalNavigate;
   
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
      // set autosave name if this is a named window
      if (name)
         [self setWindowFrameAutosaveName: name];
      
      // create web view, save it as a member, and register as it's delegate,
      
      webView_ = [[WebViewWithKeyEquiv alloc] initWithFrame: frameRect];
      [webView_ setUIDelegate: (id) self];
      [webView_ setFrameLoadDelegate: (id) self];
      [webView_ setResourceLoadDelegate: (id) self];
      [webView_ setPolicyDelegate: (id) self];
      [webView_ setKeyEquivDelegate: (id) self];
      
      // respect the current zoom level
      [self syncZoomLevel];
      
      // load the request
      [[webView_ mainFrame] loadRequest: request];
      
      // add the webview to the window
      [window setContentView: webView_];
      
      // bring the window to the front
      [window makeKeyAndOrderFront: self];
      
      // track named windows for reactivation
      if (name)
      {
         // track it (for reactivation)
         name_ = [name copy];
         [namedWindows_ setValue: self forKey: name_];
      }
      
      if (clientName)
      {
         // keep track of the name requested by the client (used to report the
         // name of the created window back to the client; see
         // handler for didClearWindowObject)
         clientName_ = [clientName copy];
      }
      
      // set fullscreen mode (default to non-primary)
      desktop::utils::enableFullscreenMode(window, false);
      
   }
   
   return self;
}

- (void) loadURL: (NSURL*) url
{
   // record base url
   if(url != baseUrl_)
   {
      [url retain];
      [baseUrl_ release];
      baseUrl_ = url;
   }
   
   // load the webview
   NSURLRequest* request = [NSURLRequest requestWithURL: baseUrl_];
   [[[self webView] mainFrame] loadRequest: request];
}

- (void) syncZoomLevel
{
   // reset to a known baseline
   [self adjustZoomLevel: 0];
   
   // get the zoom level
   int zoomLevel = desktop::options().zoomLevel();
  
   // apply it to the view
   [self adjustZoomLevel: zoomLevel];
}

- (void) adjustZoomLevel: (int) zoomLevel
{
   // reset
   if (zoomLevel == 0)
   {
      [webView_ resetPageZoom: self];
   }
   // zoom in
   else if (zoomLevel > 0)
   {
      for (int i=0; i < zoomLevel; i++)
         [webView_ zoomPageIn: self];
   }
   // zoom out
   else if (zoomLevel < 0)
   {
      zoomLevel = std::abs(zoomLevel);
      for (int i = 0; i < zoomLevel; i++)
         [webView_ zoomPageOut: self];
   }
}

// set the current viewer url
- (void) setViewerURL: (NSString*) url
{
   // record viewer url
   if(url != viewerUrl_)
   {
      [viewerUrl_ release];
      
      // record about:blank literally
      if ([url isEqual: @"about:blank"]) {
         viewerUrl_ = [url retain];
         return;
      }
      
      viewerUrl_ = [authorityFromUrl(url) retain];
   }
}

- (void) setShinyDialogURL:(NSString *) url
{
   if (url != shinyDialogUrl_)
   {
      [shinyDialogUrl_ release];
      shinyDialogUrl_ = [authorityFromUrl(url) retain];
   }
}

- (void) windowDidLoad
{
   [super windowDidLoad];
   
   // more post-load initialization
}
 
- (void) webView:(WebView *) sender
 didReceiveTitle:(NSString *) title
        forFrame:(WebFrame *) frame
{
   // set window title when main frame title is available
   if (frame == [webView_ mainFrame])
      [[self window] setTitle: title];
}

- (BOOL)                     webView: (WebView*) sender
runJavaScriptConfirmPanelWithMessage: (NSString*) message
                    initiatedByFrame: (WebFrame *) frame
{
   NSAlert *alert = [[[NSAlert alloc] init] autorelease];
   [alert setMessageText:message];
   [alert setAlertStyle:NSWarningAlertStyle];
   [alert addButtonWithTitle:@"Yes"];
   [alert addButtonWithTitle:@"No"];
   if ([alert runModal] == NSAlertFirstButtonReturn)
      return YES;
   else
      return NO;
}

- (void)                   webView: (WebView *) sender
runJavaScriptAlertPanelWithMessage: (NSString *) message
                  initiatedByFrame: (WebFrame *) frame
{
   NSAlert *alert = [[[NSAlert alloc] init] autorelease];
   [alert setMessageText: message];
   [alert addButtonWithTitle:@"OK"];
   [alert runModal];
}

- (void) webView: (WebView *)sender
  printFrameView: (WebFrameView *) frameView
{
   if ([frameView documentViewShouldHandlePrint])
   {
      [frameView printDocumentView];
   }
   else
   {
      [self printFrameView: frameView];
   }
}

- (void) printFrameView: (WebFrameView*) frameView
{
   NSPrintOperation* printOperation =
   [frameView printOperationWithPrintInfo: [NSPrintInfo sharedPrintInfo]];
   [printOperation runOperation];
}

// WebViewController is a self-freeing object so free it when the window closes
- (void)windowWillClose:(NSNotification *) notification
{
   // if we were named then remove from the tracker
   if (name_)
      [namedWindows_ removeObjectForKey: name_];
   
   // if we have a client-supplied name, let the client know the window is gone
   if (clientName_)
   {
      NSString* windowName = clientName_;
      NSArray* args = [NSArray arrayWithObjects: windowName, nil];
      [[[[MainFrameController instance] webView] windowScriptObject]
       callWebScriptMethod: @"unregisterDesktopChildWindow" withArguments: args];
   }

   // unsubscribe observers
   [[self window] setDelegate: nil];
   [webView_ setUIDelegate: nil];
   [webView_ setFrameLoadDelegate: nil];
   [webView_ setResourceLoadDelegate: nil];
   [webView_ setPolicyDelegate: nil];
   [webView_ setKeyEquivDelegate: nil];
   
   [self autorelease];
}

- (BOOL) isSupportedScheme: (NSString*) scheme
{
   return ([scheme isEqualTo: @"http"] ||
           [scheme isEqualTo: @"https"] ||
           [scheme isEqualTo: @"mailto"] ||
           [scheme isEqualTo: @"data"]);
}

- (BOOL) isApplicationURL: (NSURL*) url
{
   if (([[url scheme] isEqualTo: [baseUrl_ scheme]] &&
        [[url host] isEqualTo: [baseUrl_ host]] &&
        [[url port] isEqualTo: [baseUrl_ port]]))
   {
      return YES;
   }
   else
   {
      return NO;
   }
}

-(void) decidePolicyFor: (WebView *) webView
      actionInformation: (NSDictionary *) actionInformation
                request: (NSURLRequest *) request
                 iframe: (BOOL) iframe
       decisionListener: (id <WebPolicyDecisionListener>) listener
{
   // get the url for comparison to the base url
   NSURL* url = [request URL];
   if ([[url absoluteString] isEqualTo: @"about:blank"])
   {
      [listener use];
      return;
   }
   
   // ensure this is a supported scheme
   NSString* scheme = [url scheme];
   if (![self isSupportedScheme: scheme])
   {
      [[NSWorkspace sharedWorkspace] openURL: url];
      [listener ignore];
      return;
   }
   
   // open PDFs externally
   if ([[url pathExtension] isEqual: @"pdf"])
   {
      desktop::downloadAndShowFile([self rsessionRequest: request]);
      [listener ignore];
      return;
   }
   
   NSString* host = [url host];
   BOOL isLocal = [host isEqualTo: @"localhost"] ||
   [host isEqualTo: @"127.0.0.1"];
   
   if ((!baseUrl_ && isLocal) || [self isApplicationURL: url])
   {
      [listener use];
   }
   else if (isLocal && (viewerUrl_ != nil) &&
            [[url absoluteString] hasPrefix: viewerUrl_])
   {
      [listener use];
   }
   else if (isLocal && (shinyDialogUrl_ != nil) &&
            [[url absoluteString] hasPrefix: shinyDialogUrl_])
   {
      [listener use];
   }
   else
   {
      // get the host name
      NSString* hostName =
         [webView stringByEvaluatingJavaScriptFromString: @"window.location.host"];
      BOOL isSameDomain = false;
      if (hostName)
         isSameDomain = [[url host] hasPrefix: hostName];
      
      // perform a base64 download if necessary
      WebNavigationType navType = (WebNavigationType)[[actionInformation
                        objectForKey:WebActionNavigationTypeKey] intValue];
      if ([scheme isEqualToString: @"data"] &&
           (navType == WebNavigationTypeLinkClicked ||
            navType == WebNavigationTypeFormSubmitted))
      {
         [self handleBase64Download: url
                         forElement: [actionInformation
                                      objectForKey:WebActionElementKey]];
         [listener ignore];
      }
      // show external links to a new domain in a new window
      else if (navType == WebNavigationTypeLinkClicked && !isSameDomain)
      {
         desktop::utils::browseURL(url);
         [listener ignore];
      }
      // allow embedding videos from whitelisted sources
      else if ([host hasSuffix: @".youtube.com"] ||
               [host hasSuffix: @".vimeo.com"]   ||
               [host hasSuffix: @".ch9.ms"])
      {
         [listener use];
      }
      else
      {
         if (allowExternalNav_)
            [listener use];
         else
            [listener ignore];
      }
   }
}

- (void)               webView: (WebView *) webView
decidePolicyForNewWindowAction: (NSDictionary *) actionInformation
                       request: (NSURLRequest *) request
                  newFrameName: (NSString *)frameName
              decisionListener: (id < WebPolicyDecisionListener >)listener
{
   [self decidePolicyFor: webView
       actionInformation: actionInformation
                 request: request
                  iframe: FALSE
        decisionListener: listener];
}

- (void)                webView: (WebView *) webView
decidePolicyForNavigationAction: (NSDictionary *) actionInformation
                        request: (NSURLRequest *) request
                          frame: (WebFrame *) frame
               decisionListener: (id < WebPolicyDecisionListener >)listener
{
   [self decidePolicyFor: webView
       actionInformation: actionInformation
                 request: request
                  iframe: frame != [webView_ mainFrame]
        decisionListener: listener];
}

- (void)        webView: (WebView *) webView
decidePolicyForMIMEType: (NSDictionary *) actionInformation
                request: (NSURLRequest *) request
                  frame: (WebFrame *) frame
       decisionListener: (id < WebPolicyDecisionListener >)listener
{
   
   // get the response; if it isn't a NSHTTPURLResponse, don't process it here
   // (we need access to the headers below)
   NSHTTPURLResponse* response = (NSHTTPURLResponse*)
                                 [[frame provisionalDataSource] response];
   if (![response isKindOfClass: [NSHTTPURLResponse class]])
   {
      [listener use];
      return;
   }

   // get the Content-Disposition header to see if this file is intended to be
   // downloaded
   NSDictionary* headers = [response allHeaderFields];
   NSString* disposition =
      (NSString*)[headers objectForKey:@"Content-Disposition"];
   NSString* filename = [response suggestedFilename];
   if ([[disposition lowercaseString] hasPrefix: @"attachment"])
   {
      NSSavePanel* attSavePanel = [NSSavePanel savePanel];
      if ([self runSavePanelForFilename: attSavePanel filename: filename])
      {
         // save the file ourselves and don't handle it in the webview
         desktop::downloadAndSaveFile(request,
                                      [[attSavePanel URL] path]);
         [listener ignore];
         return;
      }
   }
   
   // for other types, proceed normally
   [listener use];
}

- (NSWindow*) uiWindow
{
   return [self window];
}

// Handle new window request by creating another controller
- (WebView *) webView: (WebView *) sender
              createWebViewWithRequest:(NSURLRequest *)request
{
   NSString* name = nil;
   bool isSatellite = false;
   bool allowExternalNavigate = false;

   // check to see if this window is one we're expecting
   if (!pendingWindow_.empty())
   {
      PendingWindow pendingWindow = pendingWindow_;
      pendingWindow_ = PendingWindow();
      name = [NSString stringWithUTF8String: pendingWindow.name.c_str()];
      isSatellite = pendingWindow.isSatellite;
      allowExternalNavigate = pendingWindow.allowExternalNavigate;

      // check for an existing window, and activate it
      WebViewController* controller = [namedWindows_ objectForKey: name];
      if (controller)
      {
         [[controller window] makeKeyAndOrderFront: self];
         return nil;
      }
   }
   
   // create the appropriate controller type; these are self-freeing so don't
   // auto-release
   if (isSatellite)
   {
      SatelliteController* satelliteController =
      [[SatelliteController alloc] initWithURLRequest: request
                                                 name: name
                                           clientName: name
                                allowExternalNavigate: allowExternalNavigate];
      
      return [satelliteController webView];
   }
   else
   {
      SecondaryWindowController * controller =
         [[SecondaryWindowController alloc] initWithURLRequest: request
                                                          name: name
                                                    clientName: name
                                         allowExternalNavigate: allowExternalNavigate];
      return [controller webView];
   }
}

- (NSURLRequest*) webView:(WebView *) sender
                  resource:(id) identifier
                  willSendRequest:(NSURLRequest *)request
                  redirectResponse:(NSURLResponse *) redirectResponse
                  fromDataSource:(WebDataSource *) dataSource
{
   return [self rsessionRequest: request];
}

- (NSURLRequest*) rsessionRequest: (NSURLRequest*) request
{
   NSMutableURLRequest *mutableRequest = [[request mutableCopy] autorelease];
   std::string secret = desktop::options().sharedSecret();
   [mutableRequest setValue: [NSString stringWithUTF8String: secret.c_str()]
                   forHTTPHeaderField:@"X-Shared-Secret"];
   [mutableRequest setTimeoutInterval: 3600];
   return mutableRequest;
}

- (void) registerDesktopObject
{
   id win = [webView_ windowScriptObject];
   gwtCallbacks_ =
            [[[GwtCallbacks alloc] initWithUIDelegate: self] autorelease];
   [win setValue: gwtCallbacks_ forKey:@"desktop"];
}

- (BOOL) hasDesktopObject
{
   WebScriptObject* script = [webView_ windowScriptObject];
   if (script == nil)
      return NO;
   
   return [[script evaluateWebScript: @"!!window.desktopHooks"] boolValue];
}

- (id) invokeCommand: (NSString*) command
{
   static NSArray* noRefocusCommands = [[NSArray alloc] initWithObjects:
                                        @"undoDummy", @"redoDummy",
                                        @"cutDummy", @"copyDummy", @"pasteDummy",
                                        nil];
   
   if (![noRefocusCommands containsObject: command])
      [[self window] makeKeyAndOrderFront: self];
   
   return [self evaluateJavaScript: [NSString stringWithFormat: @"window.desktopHooks.invokeCommand(\"%@\");",
                                     command]];
}

- (BOOL) isCommandEnabled: (NSString*) command
{
   return [[self evaluateJavaScript: [NSString stringWithFormat: @"window.desktopHooks.isCommandEnabled(\"%@\");",
                                      command]] boolValue];
}

- (BOOL) performKeyEquivalent: (NSEvent *)theEvent
{
   NSString* chr = [theEvent charactersIgnoringModifiers];
   // Get all the modifier flags except caps lock, which we don't care about for
   // the sake of these comparisons
   NSUInteger mod = [theEvent modifierFlags] &
         (NSDeviceIndependentModifierFlagsMask ^ NSAlphaShiftKeyMask);
   if ([chr isEqualToString: @"w"] && mod == NSCommandKeyMask)
   {
      if ([clientName_ hasPrefix: SOURCE_WINDOW_PREFIX])
      {
         // in the source window, cmd+w should close the current tab
         [self invokeCommand: @"closeSourceDoc"];
      }
      else
      {
         // cmd+w closes other satellites
         [[webView_ window] performClose: self];
      }
      return YES;
   }
   
   // check for empty selection (we don't want to blast the clipboard when
   // cutting or copying and there's no text)
   BOOL emptySelection = [[webView_ stringByEvaluatingJavaScriptFromString: @"window.desktopHooks.isSelectionEmpty()"] boolValue];

   // Without these, secondary/satellite windows don't respond to clipboard shortcuts
   if ([chr isEqualToString: @"x"] && mod == NSCommandKeyMask)
   {
      if (!emptySelection)
         [webView_ cut: self];
      return YES;
   }
   if ([chr isEqualToString: @"c"] && mod == NSCommandKeyMask)
   {
      if (!emptySelection)
         [webView_ copy: self];
      return YES;
   }
   if ([chr isEqualToString: @"v"] && mod == NSCommandKeyMask)
   {
      [webView_ paste: self];
      return YES;
   }
   if ([chr isEqualToString: @"a"] && mod == NSCommandKeyMask)
   {
      if ([webView_ respondsToSelector: @selector(selectAll:)])
         [webView_ selectAll: self];
      
      // ACE needs to handle this event to do custom logic for Select All, so
      // continue to process the event as though it were unhandled here.
   }
   
   return NO;
}

- (void) handleBase64Download: (NSURL*) url
                   forElement: (NSDictionary*) elementDict
{
   // TODO: handle base64 download
   // (see WebPage::handleBase64Download in Qt version)
   NSString* absUrl = [url absoluteString];
   NSArray* parts = [absUrl
                     componentsSeparatedByCharactersInSet: [NSCharacterSet
                                                            characterSetWithCharactersInString: @":;,"]];
   if ([parts count] != 4
       || ![@"data" isEqualToString: [parts objectAtIndex: 0]]
       || ![@"base64" isEqualToString: [parts objectAtIndex: 2]])
   {
      NSLog(@"Invalid data URL");
      return;
   }

   DOMNode* node = [elementDict objectForKey: @"WebElementDOMNode"];
   // The DOM node that was clicked might be the DOMText or another element that was inside the anchor.
   // Walk the parents until we get to an anchor.
   while (node && ([node nodeType] != 1 || ![[(DOMElement*)node tagName] isEqualToString: @"A"]))
      node = [node parentElement];
   if (!node)
   {
      NSLog(@"Data URI's originating anchor not found");
      return;
   }

   DOMElement* el = (DOMElement*)node;

   DOMNode* downloadAttrib = [[el attributes] getNamedItem: @"download"];
   if (downloadAttrib == nil)
   {
      NSLog(@"'download' attribute not found on anchor");
      return;
   }
   NSString* filename = [downloadAttrib nodeValue];

   NSData* data = desktop::utils::base64Decode([parts objectAtIndex: 3]);
   if (data == nil)
   {
      NSLog(@"Failed to decode data URL");
      return;
   }

   NSSavePanel* dlSavePanel = [NSSavePanel savePanel];
   if ([self runSavePanelForFilename: dlSavePanel filename: filename])
   {
      [data writeToURL: [dlSavePanel URL] atomically: FALSE];
   }
}

- (id) evaluateJavaScript: (NSString*) js
{
   id win = [webView_ windowScriptObject];
   return [win evaluateWebScript: js];
}

- (bool) runSavePanelForFilename: (NSSavePanel*) panel
                        filename: (NSString*) filename
{
   [panel setNameFieldStringValue: filename];
   [panel beginSheetModalForWindow: [self window] completionHandler: ^(NSInteger result) {}];
   long int result = [panel runModal];
   [NSApp endSheet: panel];
   
   return result == NSFileHandlingPanelOKButton;
}

- (void) webView: (WebView *) sender
      didClearWindowObject: (WebScriptObject *) windowObject
                  forFrame: (WebFrame *) frame
{
   // on the desktop, the main frame needs to be notified when a child window
   // is opened in order to communicate with the child window's window object.
   // extract the window object from the child...
   id windowObj = [windowObject evaluateWebScript: @"window;"];
   NSString* windowName = clientName_;
   
   // ... and inject it in the main frame.
   NSArray* args = [NSArray arrayWithObjects: windowName, windowObj, nil];
   [[[[MainFrameController instance] webView] windowScriptObject]
    callWebScriptMethod: @"registerDesktopChildWindow" withArguments: args];
}

- (void)webView: (WebView *) sender
  runOpenPanelForFileButtonWithResultListener:
                 (id<WebOpenPanelResultListener>) resultListener
{
   // WebView doesn't natively launch a file browser for HTML file inputs,
   // so launch one as a modal sheet here and pass the result back to the
   // listener
   NSOpenPanel* panel = [NSOpenPanel openPanel];
   
   [panel setCanChooseFiles: true];
   [panel setCanChooseDirectories: false];
   [panel beginSheetModalForWindow: [self uiWindow]
                 completionHandler: ^(NSInteger result) {}];
   long int result = [panel runModal];
   @try
   {
      if (result == NSOKButton)
      {
         [resultListener chooseFilename: [[panel URL] relativePath]];
      }
   }
   @catch (NSException* e)
   {
      throw e;
   }
   @finally
   {
      [NSApp endSheet: panel];
   }
}

@end

#ifdef __clang__
#pragma clang diagnostic pop
#endif


