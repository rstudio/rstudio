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

#import "SlowScriptProxy.h"

@implementation SlowScriptProxy
- (id)initWithWebView:(WebView*)webView {
  _target = [[webView UIDelegate] retain];
  _webView = [webView retain];
  [_webView setUIDelegate:self];
  return self;
}

- (void)dealloc {
  // Restore the original delegate
  [_webView setUIDelegate:_target];
  [_webView release];
  [_target release];
  [super dealloc];
}

- (void)forwardInvocation:(NSInvocation *)anInvocation {
  [anInvocation setTarget:_target];
  [anInvocation invoke];
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector {
  return [_target methodSignatureForSelector:aSelector];
}

- (WebView*)webView {
  return _webView;
}

- (BOOL)webViewShouldInterruptJavaScript:(WebView *)sender {
  // TODO: (robertvawter) What do we want to do with repeated invocations?
  return NO;
}

@end
