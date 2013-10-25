

#import <Cocoa/Cocoa.h>
#import <Webkit/WebKit.h>

@interface WebViewController : NSWindowController<NSWindowDelegate> {
   WebView* webView_;
}
- (id)initWithURLRequest: (NSURLRequest*) request;
- (WebView*) webView;
@end

