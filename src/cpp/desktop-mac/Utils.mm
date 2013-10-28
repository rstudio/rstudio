

#import <Cocoa/Cocoa.h>

#include <core/FilePath.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "Utils.hpp"

using namespace core;

namespace desktop {
namespace utils {
      
// PORT: from DesktopUtilsMac.mm
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
      NSString* lcid = [[NSLocale currentLocale] localeIdentifier];
      if (lcid)
      {
         // Eliminate trailing @ components (OS X uses the @ suffix to append
         // locale overrides like alternate currency formats)
         std::string localeId = std::string([lcid UTF8String]);
         std::size_t atLoc = localeId.find('@');
         if (atLoc != std::string::npos)
         {
            localeId = localeId.substr(0, atLoc);
            lcid = [NSString stringWithUTF8String: localeId.c_str()];
         }
         
         lang = [lcid stringByAppendingString:@".UTF-8"];
      }
   }
   
   // None of the above worked. Just hard code it.
   if (!lang)
   {
      lang = @"en_US.UTF-8";
   }
   
   const char* clang = [lang cStringUsingEncoding:NSASCIIStringEncoding];
   core::system::setenv("LANG", clang);
   core::system::setenv("LC_CTYPE", clang);
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

      
} // namespace utils
} // namespace desktop
