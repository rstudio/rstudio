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

#include <map>
#import <JavaScriptCore/JavaScriptCore.h>
#import <WebKit/WebKit.h>
#import "BrowserChannel.h"
#import "Debug.h"
#import "GTMSystemVersion.h"
#import "NSMutableString+HtmlReplacement.h"
#import "LoadModuleMessage.h"
#import "OophmWebScriptObject.h"
#import "SessionHandler.h"
#import "AllowedConnections.h"

/*
 * This is a helper shim to bridge crash events from the core cpp code to the
 * objc plugin and UI layer.
 */
class PluginCrashHandler : public CrashHandler {
public:
  PluginCrashHandler(OophmWebScriptObject* obj) : obj(obj) {
  }
  
  virtual void crash(const char* functionName, const char* message) {
    Debug::log(Debug::Error) << "Crashing with message: "<< message << Debug::flush;
    NSString* str = [NSString stringWithFormat:@"%s\n\n%s", message, functionName];
    [obj crashWithMessage:str];
  }
  virtual bool hasCrashed();
private:
  OophmWebScriptObject* const obj;
};

@interface OophmWebScriptObject (Private)
+ (void)logAndThrowString: (NSString*)message;
- (void)addAllowedHost: (NSString*)host;
- (BOOL)hasCrashed;
- (void)connectAlertDidEnd: (NSAlert*)alert
                returnCode: (int)returnCode
               contextInfo: (void*)contextInfo;
- (BOOL)doConnectWithUrl: (NSString*) url
          withSessionKey: (NSString*) sessionKey
                withHost: (NSString*) host
              withModule: (NSString*) moduleName
   withHostedHtmlVersion: (NSString*) hostedHtmlVersion;
	
@end

// This is declared here so that we can access the category method
bool PluginCrashHandler::hasCrashed() {
  return [obj hasCrashed] ? true : false;
}

@implementation OophmWebScriptObject
+ (void)initialize {
  // Add the plugin's bundle name to the user defaults search path
  NSBundle* pluginBundle = [NSBundle bundleForClass:[OophmWebScriptObject class]];
  NSString* bundleIdentifier = [pluginBundle bundleIdentifier];
  NSUserDefaults* shared = [NSUserDefaults standardUserDefaults];
  [shared addSuiteNamed:bundleIdentifier];
}

+ (BOOL)isSelectorExcludedFromWebScript:(SEL)selector {
  if (selector == @selector(initForWebScriptWithJsniContext:)) {
    return NO;
  } else if (selector == @selector(connectWithUrl:withSessionKey:withHost:withModuleName:withHostedHtmlVersion:)) {
    return NO;
  } else if (selector == @selector(crashWithMessage:)) {
    return NO;
  }

  return YES;
}

+ (OophmWebScriptObject*)scriptObjectWithContext: (JSGlobalContextRef) context
                                     withWebView: (WebView*) webView {
  JSGlobalContextRetain(context);
  OophmWebScriptObject* obj = [[[OophmWebScriptObject alloc] init] autorelease];
  obj->_contextRef = context;
  obj->_webView = [webView retain];
  return obj;
}

+ (NSString*)webScriptNameForSelector: (SEL)selector {
  if (selector == @selector(initForWebScriptWithJsniContext:)) {
    return @"init";
  } else if (selector == @selector(connectWithUrl:withSessionKey:withHost:withModuleName:withHostedHtmlVersion:)) {
    return @"connect";
  } else if (selector == @selector(crashWithMessage:)) {
    return @"crash";
  }
  return nil;
}

// Simply return true to indicate the plugin was successfully loaded and
// reachable.
- (BOOL)initForWebScriptWithJsniContext: (WebScriptObject*) jsniContext {
  return YES;
}

- (BOOL)connectWithUrl: (NSString*) url
        withSessionKey: (NSString*) sessionKey
              withHost: (NSString*) host
        withModuleName: (NSString*) moduleName
 withHostedHtmlVersion: (NSString*) hostedHtmlVersion {

  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];

  // See if authentication has been bypassed
  if ([defaults boolForKey:@"allowAll"]) {
    return [self doConnectWithUrl:url withSessionKey:sessionKey withHost:host
        withModule:moduleName withHostedHtmlVersion:hostedHtmlVersion];
  }
  
  // TODO(jat): do this only once, refactor to a separate method
  NSArray* allowedHosts = [defaults arrayForKey:@"allowedHosts"];
  if (allowedHosts != nil) {
    AllowedConnections::clearRules();
    int n = [allowedHosts count];
    for (int i = 0; i < n; ++i) {
      NSString* entry = [allowedHosts objectAtIndex:i];
      std::string hostName = [entry UTF8String];
      std::string codeServer = "localhost";
      int len = hostName.length();
      bool exclude = false;
      if (len > 0) {
        if (len > 1 && hostName[0] == '!') {
          exclude = true;
          hostName = hostName.substr(1);
        }
        int slash = hostName.find("/");
        if( slash > 0 && slash != std::string::npos ) {
            codeServer = hostName.substr(slash+1);
            hostName = hostName.substr(0,slash);
        }
        AllowedConnections::addRule(hostName, codeServer, exclude);
      }
    }
  }

  const std::string urlStr = [url UTF8String];
  bool allowed = false;

  if (AllowedConnections::matchesRule(AllowedConnections::getHostFromUrl(urlStr), 
                                      AllowedConnections::getCodeServerFromUrl(urlStr),
                                      &allowed) )
  {
    if (allowed) {
      return [self doConnectWithUrl:url withSessionKey:sessionKey withHost:host
          withModule:moduleName withHostedHtmlVersion:hostedHtmlVersion];
    } else {
      return YES;
    }
  }

  // Otherwise, bring up an alert dialog
  // TODO(jat): add an include/exclude option, currently treat as only include
  NSAlert* alert = [NSAlert alertWithMessageText:@"Initiate development mode session"
                                   defaultButton:@"Deny"
                                 alternateButton:nil
                                     otherButton:@"Allow"
                       informativeTextWithFormat:@"The current web-page would like to initiate a development-mode connection to %@", host];
  
  if ([alert respondsToSelector:@selector(setShowsSuppressionButton:)]) {
    [alert setShowsSuppressionButton:YES];
    [[alert suppressionButton] setTitle:@"Remember this decision for this server"];
  } else {
    [[alert addButtonWithTitle:@"Always allow"] setTag:NSAlertAlternateReturn];
  }
  
  NSBundle* bundle = [NSBundle bundleForClass:[OophmWebScriptObject class]];
	NSArray* contextArray = [[NSArray arrayWithObjects:[url retain],
      [sessionKey retain], [host retain], [moduleName retain],
      [hostedHtmlVersion retain], nil] retain];
  NSString* imagePath = [bundle pathForImageResource:@"gwtlogo"];
  if (imagePath != nil) {
    NSImage* img = [[[NSImage alloc] initByReferencingFile:imagePath] autorelease];
    [alert setIcon:img];
  }
  
  [alert beginSheetModalForWindow:[_webView hostWindow]
                    modalDelegate:self
                   didEndSelector:@selector(connectAlertDidEnd:returnCode:contextInfo:)
                      contextInfo:contextArray];
  return YES;
}

- (void)crashWithMessage: (NSString*)message {
  if (self->_hasCrashed) {
    return;
  }
  self->_hasCrashed = YES;
  
#ifdef GWT_DEBUGDISABLE
  // We'll call out to the JS support function
  JSGlobalContextRef contextRef = self->_contextRef;
  JSStringRef disconnectedName = JSStringCreateWithUTF8CString("__gwt_disconnected");
  JSValueRef disconnected = JSObjectGetProperty(contextRef, JSContextGetGlobalObject(contextRef), disconnectedName, NULL);
  JSStringRelease(disconnectedName);
  
  if (JSValueIsObject(contextRef, disconnected)) {
    // Found hosted.html's crash support
    JSObjectRef disconnectedFunction = JSValueToObject(contextRef, disconnected, NULL);
    JSValueRef exception = NULL;
    JSObjectCallAsFunction(contextRef, disconnectedFunction, JSContextGetGlobalObject(contextRef), 0, NULL, &exception);
    if (!exception) {
      // Couldn't invoke the crash handler.
      return;
    }
  }
#endif //GWT_DEBUGDISABLE

  // Use a simple crash page built into the bundle
  NSBundle* oophmBundle = [NSBundle bundleForClass:[self class]];
  NSString* path = [oophmBundle pathForResource:@"crash" ofType:@"html"];
  NSMutableString* crashPage = [NSMutableString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:nil];
  [crashPage replacePattern:@"__MESSAGE__" withStringLiteral:message];
  
  long major, minor, bugFix;
  [GTMSystemVersion getMajor:&major minor:&minor bugFix:&bugFix];
  NSString* systemVersion = [NSString stringWithFormat:@"%i.%i.%i", major, minor, bugFix];
  [crashPage replacePattern:@"__SYSTEM_VERSION__" withStringLiteral:systemVersion];
  
  NSString* ua = [_webView userAgentForURL:[NSURL URLWithString:@"about:blank"]];
  [crashPage replacePattern:@"__USER_AGENT__" withStringLiteral:ua];
  
  [crashPage replacePattern:@"__DATE__"
          withStringLiteral:[NSString stringWithUTF8String:__DATE__]];
  [crashPage replacePattern:@"__TIME__"
          withStringLiteral:[NSString stringWithUTF8String:__TIME__]];  

  NSURL* currentUrl = [[[[_webView mainFrame] dataSource] response] URL];

  [[_webView mainFrame] loadAlternateHTMLString:crashPage
                                        baseURL:[NSURL fileURLWithPath:path]
                              forUnreachableURL:currentUrl];
}

- (void)dealloc {
  [_webView release];  
  delete _crashHandler;
  [super dealloc];
}

- (void)finalizeForWebScript {
  Debug::log(Debug::Info) << "Finalizing OophmWebScriptObject" << Debug::flush;

  // Disable any lingering JS proxy objects
  _hasCrashed = true;

  // Free memory
  delete _sessionHandler;
  
  if (_hostChannel) {
    _hostChannel->disconnectFromHost();
    delete _hostChannel;
    _hostChannel = NULL;
  }

  if (_contextRef) {
    JSGlobalContextRelease(_contextRef);
    _contextRef = NULL;
  }
}
@end

@implementation OophmWebScriptObject (Private)
+ (void)logAndThrowString:(NSString*)message {
  Debug::log(Debug::Info) << "Throwing exception from WSO: " << message << Debug::flush;
  [WebScriptObject throwException:message];
}

- (void)addAllowedHost:(NSString*)host {
  /*
   * This is more complicated than usual because we're not using the
   * application's default persestent domain.  Instead, we use a plugin-specific
   * domain.
   */
  NSBundle* pluginBundle = [NSBundle bundleForClass:[OophmWebScriptObject class]];
  NSString* bundleIdentifier = [pluginBundle bundleIdentifier];
  
  NSUserDefaults* shared = [NSUserDefaults standardUserDefaults];
  NSDictionary* pluginDict = [shared persistentDomainForName:bundleIdentifier];
  NSArray* allowedHosts = [pluginDict objectForKey:@"allowedHosts"];
  
  //TODO(codefu): don't add duplicates

  NSMutableArray* mutableHosts = [NSMutableArray arrayWithArray:allowedHosts];
  NSMutableDictionary* mutableDict = [NSMutableDictionary dictionaryWithDictionary:pluginDict];
  [mutableHosts addObject:host];
  [mutableDict setObject:mutableHosts forKey:@"allowedHosts"];
  [shared setPersistentDomain:mutableDict forName:bundleIdentifier];
  [shared synchronize];
}

- (BOOL)hasCrashed{
  return self->_hasCrashed;
}

- (void)connectAlertDidEnd:(NSAlert *)alert
                returnCode:(int)returnCode
               contextInfo:(void *)contextInfo {
  NSArray* contextArray = (NSArray*) contextInfo;
  NSString* url = [[contextArray objectAtIndex:0] autorelease];
  NSString* sessionKey = [[contextArray objectAtIndex:1] autorelease];
  NSString* host = [[contextArray objectAtIndex:2] autorelease];
  NSString* moduleName = [[contextArray objectAtIndex:3] autorelease];
  NSString* hostedHtmlVersion = [[contextArray objectAtIndex:4] autorelease];
  [contextArray release];
  
  if (returnCode == NSAlertDefaultReturn) {
    //TODO(codefu): save the host/codesvr as excluded ("!host")
    //              should this require a check to verify that one is
    //              not already whitelisted?
    // currently: ignore exclude, re-show the modal popup on webpage reload
    return;
  } else if (returnCode == NSAlertAlternateReturn ||
      [alert respondsToSelector:@selector(suppressionButton)] &&
      [[alert suppressionButton] state] == NSOnState) {
    // TODO(jat): simplify, handle errors
    // Get the host part of the URL and store that
    NSString* host = [NSString stringWithFormat: @"%@/%@",
                      [[[[[[url componentsSeparatedByString:@"://"]
                           objectAtIndex:1] componentsSeparatedByString:@"/"] objectAtIndex:0]
                           componentsSeparatedByString:@":"] objectAtIndex:0],
                      [[[[[[url componentsSeparatedByString:@"gwt.codesvr="]
                           objectAtIndex:1] componentsSeparatedByString:@"&"] objectAtIndex:0]
                        componentsSeparatedByString:@":"] objectAtIndex:0]];
    [self addAllowedHost:host];
  }

  [self doConnectWithUrl:url withSessionKey:sessionKey withHost:host
      withModule:moduleName withHostedHtmlVersion:hostedHtmlVersion];
}

- (BOOL)doConnectWithUrl: (NSString*) url
          withSessionKey: (NSString*) sessionKey
                withHost: (NSString*) host
              withModule: (NSString*) moduleName
   withHostedHtmlVersion: (NSString*) hostedHtmlVersion {
  Debug::log(Debug::Debugging) << "connect : " << [host UTF8String] << " " <<
      [moduleName UTF8String] << Debug::flush;
  
  if (_hostChannel != NULL) {
    [OophmWebScriptObject logAndThrowString:@"Already connected"];
    return NO;
  }
  
  NSArray *parts = [host componentsSeparatedByString:@":"];
  if ([parts count] != 2) {
    [OophmWebScriptObject logAndThrowString:
     [NSString stringWithFormat:@"Incorrect format for host string %i",
      [parts count]]];
    return NO;
  }
  
  NSString *hostPart = [parts objectAtIndex:0];
  NSString *portPart = [parts objectAtIndex:1];
  
  Debug::log(Debug::Debugging) << "Extracted host: " << [hostPart UTF8String] <<
      " and port: " << [portPart UTF8String] << Debug::flush;
  
  char *hostAsChars = const_cast<char*>([hostPart UTF8String]);
  unsigned portAsInt = [portPart intValue];
  
  _hostChannel = new HostChannel();
  if (!_hostChannel->connectToHost(hostAsChars, portAsInt)) {
    [OophmWebScriptObject logAndThrowString:@"HostChannel failed to connect"];
    delete _hostChannel;
    _hostChannel = NULL;
    return NO;
  }
  
  _crashHandler = new PluginCrashHandler(self);
  _sessionHandler = new WebScriptSessionHandler(_hostChannel, _contextRef, _crashHandler);

  std::string hostedHtmlVersionStr([hostedHtmlVersion UTF8String]);
  // TODO: add support for a range of protocol versions when more are added.
  if (!_hostChannel->init(_sessionHandler, BROWSERCHANNEL_PROTOCOL_VERSION,
      BROWSERCHANNEL_PROTOCOL_VERSION, hostedHtmlVersionStr)) {
    [OophmWebScriptObject logAndThrowString:@"HostChannel failed to initialize"];
    _hostChannel->disconnectFromHost();
    delete _hostChannel;
    _hostChannel = NULL;
    return NO;
  }

  const std::string urlStr = [url UTF8String];
  // TODO(jat): add support for tab identity
  const std::string tabKeyStr = "";
  const std::string sessionKeyStr = [sessionKey UTF8String];
  const std::string moduleNameStr = [moduleName UTF8String];
    
  if (!LoadModuleMessage::send(*_hostChannel, urlStr, tabKeyStr, 
                               sessionKeyStr, moduleNameStr,
                               "Safari DMP", _sessionHandler)) {
    _hostChannel->disconnectFromHost();
    delete _hostChannel;
    _hostChannel = NULL;
    [OophmWebScriptObject logAndThrowString:@"Unable to load module"];
    return NO;
  }  
  
  return YES;
}
@end
