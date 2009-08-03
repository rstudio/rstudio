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
#import <WebKit/WebPlugin.h>
#import "Debug.h"
#import "OophmWebScriptObject.h"
#import "SlowScriptProxy.h"
#import "WebFrameNonTigerHeaders.h"

/*
 * This provides the entrypoint for the WebKit plugin.  This class performs
 * any necessary environmental configuration and provides the scripting object
 * that represents the plugin in the JavaScript environment.
 */
@interface OophmPlugin : NSView <WebPlugInViewFactory> {
@private
  OophmWebScriptObject* _scriptObject;
  id _slowScriptProxy;
}

/*
 * Defined by the WebPlugInViewFactory protocol to construct an instance of
 * the plugin.
 */
+ (NSView *)plugInViewWithArguments:(NSDictionary *)arguments;
- (void)dealloc;

/*
 * Called by plugInViewWithArguments to initialize the instance of the plugin.
 */
- (id)initWithArguments:(NSDictionary *)arguments;

/*
 * Specified by the WebPlugIn informal protocol to obtain an object whose
 * methods will be exposed to the scripting environment.
 */
- (id)objectForWebScript;

/*
 * Defined by WebPlugIn and called when the plugin should shut down.
 */
- (void)webPlugInDestroy;
@end
