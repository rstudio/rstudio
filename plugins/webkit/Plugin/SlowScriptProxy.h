/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>

/*
 * This NSProxy is used to prevent slow-script warnings from surfacing in the
 * web browser.  This is something of a hack since the message that we want
 * to override isn't public, but there's no way to directly reset the slow-scipt
 * timer either.
 */
@interface SlowScriptProxy : NSProxy {
@private
  id _target;
  WebView* _webView;
}

/*
 * This will restore the original UIDelegate.
 */
- (void)dealloc;

/*
 * The proxy object will install itself as the UIDelegate on the given webView.
 */
- (id)initWithWebView: (WebView*)webView;

/*
 * Just delegates the invocation to the original UIDelegate.
 */
- (void)forwardInvocation:(NSInvocation *)anInvocation;

/*
 * Just delegates the invocation to the original UIDelegate.
 */
- (NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector;

/*
 * The WebView to which the proxy object has attached itself.
 */
- (WebView*)webView;

/*
 * This is the message that we want to intercept.
 */
- (BOOL)webViewShouldInterruptJavaScript:(WebView *)sender;
@end
