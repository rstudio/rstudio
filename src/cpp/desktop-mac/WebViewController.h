

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>

@interface WebViewController : NSWindowController<NSWindowDelegate> {
   WebView* webView_;
   NSString* name_;
}

+ (WebViewController*) windowNamed: (NSString*) name;

+ (void) activateSatelliteWindow: (NSString*) name;

+ (void) prepareForSatelliteWindow: (NSString*) name
                             width: (int) width
                            height: (int) height;


// The designated initializer
- (id)initWithURLRequest: (NSURLRequest*) request
                    name: (NSString*) name;

// Get the embedded WebView
- (WebView*) webView;

// subclass methods for registering javascript callbacks
- (void) registerDesktopObject;
@end

