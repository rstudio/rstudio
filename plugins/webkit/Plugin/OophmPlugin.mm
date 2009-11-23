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

#import "OophmPlugin.h"
#import "OophmWebScriptObject.h"
#import "SlowScriptProxy.h"

@implementation OophmPlugin
+ (NSView *)plugInViewWithArguments:(NSDictionary *)arguments {
  return [[[OophmPlugin alloc] initWithArguments: arguments] autorelease];
}

- (void)dealloc {
  Debug::log(Debug::Info) << "Plugin deallocated" << Debug::flush;
  [super dealloc];
}

- (id)initWithArguments:(NSDictionary *)arguments {
  Debug::log(Debug::Info) << "Plugin starting" << Debug::flush;
  self = [super initWithFrame: NSZeroRect];
  if (!self) {
    return nil;
  }

  id container = [[arguments objectForKey:WebPlugInContainerKey] retain];
  WebFrame* frame = [container webFrame];
  JSGlobalContextRef contextRef = [frame globalContext];
  _scriptObject = [[OophmWebScriptObject scriptObjectWithContext:contextRef withWebView:[frame webView]] retain];

  /*
   * Install a proxy to prevent slow script warnings from being shown by hijacking
   * the message sent to the original UIDelegate.  We could also use this to prevent
   * window.alert and window.prompt from blocking test code.
   */
  WebView* view = [frame webView];
  _slowScriptProxy = [[SlowScriptProxy alloc] initWithWebView: view];
  if ([_slowScriptProxy respondsToSelector:@selector(webView:setStatusText:)]) {
    [_slowScriptProxy webView:view setStatusText:@"GWT Developer Plugin Active"];
  }
  
  return self;
}

- (id)objectForWebScript {
  return _scriptObject;
}

- (void)webPlugInDestroy {
  Debug::log(Debug::Info) << "Destroying plugin" << Debug::flush;
  [_scriptObject release];
  _scriptObject = nil;
  
  if ([_slowScriptProxy respondsToSelector:@selector(webView:setStatusText:)]) {
    [_slowScriptProxy webView:[_slowScriptProxy webView] 
                      setStatusText:@"GWT OOPHM Session Ended"];
  }
  [_slowScriptProxy release];
  _slowScriptProxy = nil;
}
@end
