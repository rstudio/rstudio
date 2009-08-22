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
#import "GTMStackTrace.h"
#import "GTMSystemVersion.h"
#import "NSMutableString+HtmlReplacement.h"
#import "LoadModuleMessage.h"
#import "OophmWebScriptObject.h"
#import "SessionHandler.h"

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
private:
  OophmWebScriptObject* const obj;
};

@interface OophmWebScriptObject (Private)
+ (void)logAndThrowString: (NSString*)message;
- (void)addAllowedHost: (NSString*)host;
- (void)connectAlertDidEnd: (NSAlert*)alert
                returnCode: (int)returnCode
               contextInfo: (void*)contextInfo;
- (BOOL)doConnectWithHost: (NSString*) host
               withModule: (NSString*) moduleName;
@end

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
  
  // TODO remove this for production builds
  Debug::log(Debug::Warning) << "ACLs are currently disabled" << Debug::flush;
  return [self doConnectWithUrl:url withSessonKey: withHost:host
       withModule:moduleName withHostedHtmlVersion];

  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];

  // See if authentication has been bypassed
  if ([defaults boolForKey:@"allowAll"]) {
    return [self doConnectWithUrl:url withSessonKey: withHost:host
        withModule:moduleName withHostedHtmlVersion];
  }
  
  // Otherwise, check for an explicit entry
  NSArray* allowedHosts = [defaults arrayForKey:@"allowedHosts"];
  if (allowedHosts != nil) {
    NSArray* hostParts = [host componentsSeparatedByString:@":"];
    if ([allowedHosts containsObject:[hostParts objectAtIndex:0]]) {
      return [self doConnectWithHost:host withModule:moduleName];
    }
  }
  
  // Otherwise, bring up an alert dialog
  NSAlert* alert = [NSAlert alertWithMessageText:@"Initiate hosted-mode session"
                                   defaultButton:@"Deny"
                                 alternateButton:nil
                                     otherButton:@"Allow"
                       informativeTextWithFormat:@"The current web-page would like to initiate a hosted-mode connection to %@", host];
  
  if ([alert respondsToSelector:@selector(setShowsSuppressionButton:)]) {
    [alert setShowsSuppressionButton:YES];
    [[alert suppressionButton] setTitle:@"Always allow connections to this host"];
  } else {
    [[alert addButtonWithTitle:@"Always allow"] setTag:NSAlertAlternateReturn];
  }
  
  NSBundle* bundle = [NSBundle bundleForClass:[OophmWebScriptObject class]];
  NSArray* contextArray = [[NSArray arrayWithObjects:[host retain],
                            [moduleName retain], nil] retain];
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
  NSBundle* oophmBundle = [NSBundle bundleForClass:[self class]];
  NSString* path = [oophmBundle pathForResource:@"crash" ofType:@"html"];
  NSMutableString* crashPage = [NSMutableString stringWithContentsOfFile:path];
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
  
  NSString* trace = GTMStackTrace();
  [crashPage replacePattern:@"__BACKTRACE__" withStringLiteral:trace];
  

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
  
  NSMutableArray* mutableHosts = [NSMutableArray arrayWithArray:allowedHosts];
  NSMutableDictionary* mutableDict = [NSMutableDictionary dictionaryWithDictionary:pluginDict];
  [mutableHosts addObject:host];
  [mutableDict setObject:mutableHosts forKey:@"allowedHosts"];
  [shared setPersistentDomain:mutableDict forName:bundleIdentifier];
  [shared synchronize];
}

- (void)connectAlertDidEnd:(NSAlert *)alert
                returnCode:(int)returnCode
               contextInfo:(void *)contextInfo {
  NSArray* contextArray = (NSArray*) contextInfo;
  NSString* host = [[contextArray objectAtIndex:0] autorelease];
  NSString* moduleName = [[contextArray objectAtIndex:1] autorelease];
  [contextArray release];
  
  if (returnCode == NSAlertDefaultReturn) {
    return;
  } else if (returnCode == NSAlertAlternateReturn ||
             [alert respondsToSelector:@selector(suppressionButton)] &&
             [[alert suppressionButton] state] == NSOnState) {
    NSArray* hostParts = [host componentsSeparatedByString:@":"];
    [self addAllowedHost:[hostParts objectAtIndex:0]];
  }

  [self doConnectWithHost:host withModule:moduleName];
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
                               "Safari OOPHM", _sessionHandler)) {
    _hostChannel->disconnectFromHost();
    delete _hostChannel;
    _hostChannel = NULL;
    [OophmWebScriptObject logAndThrowString:@"Unable to load module"];
    return NO;
  }  
  
  return YES;
}
@end
