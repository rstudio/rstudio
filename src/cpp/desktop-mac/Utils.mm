
#include <string>
#include <vector>
#include <set>

#include <core/StringUtils.hpp>
#include <core/FilePath.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <crt_externs.h>

#import <Cocoa/Cocoa.h>

#import "Utils.hpp"

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace desktop {
namespace utils {
      
namespace {

NSString* readSystemLocale()
{
   using namespace core;
   using namespace core::system;
   Error error;

   // First, read all available locales so we can validate whether we've received
   // a valid locale.
   ProcessResult localeResult;
   error = runCommand("/usr/bin/locale -a", ProcessOptions(), &localeResult);
   if (error)
      LOG_ERROR(error);

   std::string allLocales = localeResult.stdOut;

   // Now, try looking for the active locale using NSLocale.
   std::string localeIdentifier = [[[NSLocale currentLocale] localeIdentifier] UTF8String];

   // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
   std::string::size_type idx = localeIdentifier.find('@');
   if (idx != std::string::npos)
      localeIdentifier = localeIdentifier.substr(0, idx);

   // Enforce a UTF-8 locale.
   localeIdentifier += ".UTF-8";

   if (allLocales.find(localeIdentifier) != std::string::npos)
      return [NSString stringWithCString: localeIdentifier.c_str()];

   // If that failed, fall back to reading the defaults value. Note that Mojave
   // (at least with 10.14) reports the wrong locale above and so we rely on this
   // as a fallback.
   ProcessResult defaultsResult;
   error = runCommand("defaults read NSGlobalDomain AppleLocale", ProcessOptions(), &defaultsResult);
   if (error)
      LOG_ERROR(error);

   std::string defaultsLocale = string_utils::trimWhitespace(defaultsResult.stdOut);

   // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
   idx = defaultsLocale.find('@');
   if (idx != std::string::npos)
      defaultsLocale = defaultsLocale.substr(0, idx);

   // Enforce a UTF-8 locale.
   defaultsLocale += ".UTF-8";

   if (allLocales.find(defaultsLocale) != std::string::npos)
      return [NSString stringWithUTF8String: defaultsLocale.c_str()];

   return nullptr;
}

} // end anonymous namespace

void initializeLang()
{
   // Not sure what the memory management rules are here, i.e. whether an
   // autorelease pool is active. Just let it leak, since we're only calling
   // this once (at the time of this writing).

   // We try to simulate the behavior of R.app.

   NSString* lang = nil;

   // Highest precedence: force.LANG. If it has a value, use it.
   NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
   [defaults addSuiteNamed:@"org.R-project.R"];
   lang = [defaults stringForKey:@"force.LANG"];
   if (lang && ![lang length])
   {
      // If force.LANG is present but empty, don't touch LANG at all.
      return;
   }
   
   // Next highest precedence: ignore.system.locale. If it has a value,
   // hardcode to en_US.UTF-8.
   if (!lang && [defaults boolForKey:@"ignore.system.locale"])
   {
      lang = @"en_US.UTF-8";
   }

   // Next highest precedence: LANG environment variable.
   if (!lang)
   {
      std::string envLang = core::system::getenv("LANG");
      if (!envLang.empty())
      {
         lang = [NSString stringWithCString:envLang.c_str()
                          encoding:NSASCIIStringEncoding];
      }
   }

   // Next highest precedence: Try to figure out language from the current
   // locale.
   if (!lang)
   {
      lang = readSystemLocale();
   }

   // None of the above worked. Just hard code it.
   if (!lang)
   {
      lang = @"en_US.UTF-8";
   }

   const char* clang = [lang cStringUsingEncoding:NSASCIIStringEncoding];
   core::system::setenv("LANG", clang);
   core::system::setenv("LC_CTYPE", clang);

   initializeSystemPrefs();
}
   
void initializeSystemPrefs()
{
   NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
   [defaults setBool:YES forKey: @"WebKitWebGLEnabled"];
   [defaults setBool:YES forKey: @"WebKitDeveloperExtras"];
   [defaults setBool:YES forKey: @"WebKitJavaScriptCanAccessClipboard"];
   [defaults setBool:NO forKey: @"NSFunctionBarAPIEnabled"];
}
 
// PORT: from DesktopUtils.cpp
FilePath userLogPath()
{
   FilePath userHomePath = core::system::userHomePath("R_USER|HOME");
   FilePath logPath = core::system::userSettingsPath(
                                    userHomePath,
                                    "RStudio-Desktop").childPath("log");
   return logPath;
}

void showMessageBox(NSAlertStyle style, NSString* title, NSString* message)
{
   NSAlert *alert = [[[NSAlert alloc] init] autorelease];
   [alert setAlertStyle: style];
   [alert setMessageText: title];
   [alert setInformativeText: message];
   [alert runModal];
}
   
void browseURL(NSURL* nsurl)
{
   // check for a pdf and force use of preview (prevent crash that can
   // occur with certain versions of acrobat reader)
   if ([nsurl isFileURL] && [[nsurl absoluteString] hasSuffix: @".pdf"])
   {
      [[NSWorkspace sharedWorkspace] openFile: nsurl.path
                              withApplication: @"Preview"];
   }
   else
   {
      [[NSWorkspace sharedWorkspace] openURL: nsurl];
   }
}
   
core::system::ProcessSupervisor& processSupervisor()
{
   static core::system::ProcessSupervisor instance;
   return instance;
}
   
bool supportsFullscreenMode(NSWindow* window)
{
   return [window respondsToSelector:@selector(toggleFullScreen:)];
}
   

// see: https://developer.apple.com/library/mac/#documentation/General/Conceptual/MOSXAppProgrammingGuide/FullScreenApp/FullScreenApp.html
void enableFullscreenMode(NSWindow* window, bool primary)
{
   if (supportsFullscreenMode(window))
   {
      NSWindowCollectionBehavior behavior = [window collectionBehavior];
      behavior = behavior | (primary ?
                             NSWindowCollectionBehaviorFullScreenPrimary :
                             NSWindowCollectionBehaviorFullScreenAuxiliary);
      [window setCollectionBehavior:behavior];
   }
}

void toggleFullscreenMode(NSWindow* window)
{
   if (supportsFullscreenMode(window))
      [window toggleFullScreen:nil];
}

   
float titleBarHeight()
{
   NSRect frame = NSMakeRect (0, 0, 100, 100);
   
   NSRect contentRect;
   contentRect = [NSWindow contentRectForFrameRect: frame
                                         styleMask: NSTitledWindowMask];
   
   return (frame.size.height - contentRect.size.height);
   
}

namespace {
int charToB64Val(unsigned char c)
{
   if (c >= 'A' && c <= 'Z')
      return c - 'A';
   if (c >= 'a' && c <= 'z')
      return 26 + (c - 'a');
   if (c >= '0' && c <= '9')
      return 52 + (c - '0');
   if (c == '+')
      return 62;
   if (c == '/')
      return 63;
   if (c == '=')
      return 0;
   return -1;
}
}

NSData *base64Decode(NSString *input)
{
   int paddingChars = 0;
   std::vector<unsigned char> output;
   output.reserve(static_cast<int>([input length] * 0.75 + 2));
   std::vector<unsigned int> buffer;
   buffer.reserve(4);
   NSUInteger pos = 0;
   NSUInteger len = [input length];
   while (pos < len)
   {
      unichar c = [input characterAtIndex: pos++];
      // ignore whitespace
      if (c == ' ' || c == '\r' || c == '\n' || c == '\t')
         continue;

      if (c == '=')
      {
         paddingChars++;
         if (paddingChars > 2)
            return nil;
      }
      else if (paddingChars > 0)
         return nil; // padding chars must only appear at the end
      
      int decodedVal = charToB64Val(c);
      if (decodedVal < 0)
         return nil; // invalid data
      buffer.push_back(decodedVal);
      if (buffer.size() == 4)
      {
         // We have a quartet; align and flush to output
         output.push_back( (buffer[0]<<2) | buffer[1]>>4);
         output.push_back( ((buffer[1]&0xF)<<4) | (buffer[2]>>2) );
         output.push_back( ((buffer[2]&0x3)<<6) | (buffer[3]) );
         buffer.clear();
      }
   }
   while (paddingChars-- > 0)
      output.pop_back();
   return [NSData dataWithBytes: &output[0] length: output.size()];
}


namespace
{
// Unexposed--extract an environment variable name from its NAME=VALUE
// representation
std::string varname(const char* line)
{
   size_t nameLen = strcspn(line, "=");
   if (nameLen == strlen(line)) {
      return std::string();
   } else {
      return std::string(line, nameLen);
   }
}
}
   
void cleanDuplicateEnvVars()
{
   std::set<std::string> seen;
   std::set<std::string> dupes;
   
   // Create a list of the environment variables that appear more than once
   for (char **read = *_NSGetEnviron(); *read; read++) {
      std::string name = varname(*read);
      if (name.size() == 0) {
         continue;
      }
      if (seen.find(name) != seen.end()) {
         dupes.insert(name);
      } else {
         seen.insert(name);
      }
   }
   
   // Loop over the list of duplicated variables
   for (std::set<std::string>::iterator dupe = dupes.begin(); dupe != dupes.end(); dupe++) {
      const char *name = (*dupe).c_str();
      char *val = getenv(name);
      if (val != NULL) {
         // unsetenv removes *all* instances of the variable from the environment
         unsetenv(name);
         
         // replace with the value from getenv (in practice appears to be the
         // first value in the list)
         setenv(name, val, 0);
      }
   }
}
   
} // namespace utils
} // namespace desktop
} // namespace rstudio

