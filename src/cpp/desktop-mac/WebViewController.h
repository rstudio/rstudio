

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>

#import "WebViewWithKeyEquiv.h"

@interface WebViewController : NSWindowController<NSWindowDelegate> {
   WebViewWithKeyEquiv* webView_;
   NSString* name_;
   NSURL* baseUrl_;
   NSString* viewerUrl_;
}

+ (WebViewController*) windowNamed: (NSString*) name;

+ (void) activateSatelliteWindow: (NSString*) name;

+ (void) prepareForSatelliteWindow: (NSString*) name
                             width: (int) width
                            height: (int) height;


// The designated initializer
- (id)initWithURLRequest: (NSURLRequest*) request
                    name: (NSString*) name;

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
@end

