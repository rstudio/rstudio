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

#import "AppController.h"
#import "BrowserWindow.h"

@implementation AppController
- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  NSLog(@"%s", __PRETTY_FUNCTION__);
  
  if (![WebView canShowMIMEType:@"application/x-gwt-hosted-mode"]) {
    NSLog(@"plugin not available");
    exit(-1);
  }
  NSString* str = @"http://localhost:8888/com.google.gwt.dev.jjs.CompilerSuite.JUnit/junit.html?gwt.hosted=localhost:9997";
//  NSString* str = @"http://localhost:8888/com.google.gwt.user.RPCSuite.JUnit/junit.html?gwt.hosted=localhost:9997";
//  NSString* str = @"http://localhost:8888/com.google.gwt.user.User.JUnit/junit.html?gwt.hosted=localhost:9997";
  [webview setUIDelegate:self];
  [webview setShouldCloseWithWindow: YES];
  [[webview mainFrame] loadRequest: [NSURLRequest requestWithURL:[NSURL URLWithString:str]]];
}

- (IBAction)newWindow:(id)sender {
  NSLog(@"Action received");
  NSRect r;
  r.origin.x = 100;
  r.origin.y = 100;
  r.size.height = 500;
  r.size.width = 500;
  
  WebView* webView = [[WebView alloc] initWithFrame:r];
  NSString* str = @"http://localhost:8888/com.google.gwt.sample.kitchensink.KitchenSink/KitchenSink.html?gwt.hosted=localhost:9997";
  [webView setUIDelegate:self];
  [webView setShouldCloseWithWindow: YES];
  [[webView mainFrame] loadRequest: [NSURLRequest requestWithURL:[NSURL URLWithString:str]]];
  
  NSWindow* wnd = [[BrowserWindow alloc] initWithContentRect:r styleMask:NSResizableWindowMask|NSClosableWindowMask backing:NSBackingStoreBuffered defer:NO];
  [wnd setContentView:webView];
  [wnd makeKeyAndOrderFront:self];
}

- (void)webView:(WebView *)sender windowScriptObjectAvailable:(WebScriptObject *)windowScriptObject {
  NSLog(@"%s", __PRETTY_FUNCTION__);
}

@end
