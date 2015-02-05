

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>

#import "WebViewWithKeyEquiv.h"
#import "GwtCallbacks.h"

@interface WebViewController :
          NSWindowController<NSWindowDelegate,GwtCallbacksUIDelegate> {
   WebViewWithKeyEquiv* webView_;
   NSString* name_;
   NSString* clientName_;
   NSURL* baseUrl_;
   NSString* viewerUrl_;
   bool allowExternalNav_;
}

+ (WebViewController*) windowNamed: (NSString*) name;

+ (void) activateNamedWindow: (NSString*) name;

+ (void) prepareForSatelliteWindow: (NSString*) name
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

// Get the embedded WebView
- (WebView*) webView;

// sync the web view's zoom level
- (void) syncZoomLevel;

// print
- (void) printFrameView: (WebFrameView*) frameView;

// subclass methods for registering javascript callbacks
- (void) registerDesktopObject;

// evaluate javascript
- (id) evaluateJavaScript: (NSString*) js;

@end

