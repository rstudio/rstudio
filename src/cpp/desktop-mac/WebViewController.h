

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>

#import "WebViewWithKeyEquiv.h"
#import "GwtCallbacks.h"

#define SOURCE_WINDOW_PREFIX @"_rstudio_satellite_source_window_"

@interface WebViewController :
          NSWindowController<NSWindowDelegate,GwtCallbacksUIDelegate> {
   WebViewWithKeyEquiv* webView_;
   NSString* name_;
   NSString* clientName_;
   NSURL* baseUrl_;
   NSString* viewerUrl_;
   NSString* shinyDialogUrl_;
   bool allowExternalNav_;
   GwtCallbacks* gwtCallbacks_;
}

+ (WebViewController*) windowNamed: (NSString*) name;

+ (WebViewController*) activeDesktopController;

+ (void) activateNamedWindow: (NSString*) name;

+ (void) prepareForSatelliteWindow: (NSString*) name
                                 x: (int) x
                                 y: (int) y
                             width: (int) width
                            height: (int) height;

+ (void) prepareForNamedWindow: (NSString*) name
         allowExternalNavigate: (bool) allowExternalNavigate;

+ (void) closeNamedWindow: (NSString*) name;

// The designated initializer
- (id)initWithURLRequest: (NSURLRequest*) request
                    name: (NSString*) name
              clientName: (NSString*) clientName
   allowExternalNavigate: (bool) allowExternalNavigate;

// load a new url
- (void) loadURL: (NSURL*) url;

// set the current viewer url
- (void) setViewerURL: (NSString*) url;

// set the current shiny dialog url
- (void) setShinyDialogURL: (NSString*) url;

// Get the embedded WebView
- (WebView*) webView;

// sync the web view's zoom level
- (void) syncZoomLevel;

// adjust the zoom level (up or down; 0 to reset)
- (void) adjustZoomLevel: (int) zoomLevel;

// print
- (void) printFrameView: (WebFrameView*) frameView;

// subclass methods for registering javascript callbacks
- (void) registerDesktopObject;

// evaluate javascript
- (id) evaluateJavaScript: (NSString*) js;

// indicate whether the window has desktop hooks
- (BOOL) hasDesktopObject;

// invoke a command (only for windows with a desktop object)
- (id) invokeCommand: (NSString*) command;

// check to see whether a command is enabled (only for windows with a desktop object)
- (BOOL) isCommandEnabled: (NSString*) command;


@end

