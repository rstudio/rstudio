

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import <AppKit/NSApplication.h>
#import <Foundation/NSAutoreleasePool.h>
#import <set>
#import <string>

#import "AppDelegate.h"
#import "Utils.hpp"

#include <crt_externs.h>

using namespace core;

std::string varname(const char* line) {
   size_t nameLen = strcspn(line, "=");
   if (nameLen == strlen(line)) {
      return std::string();
   } else {
      return std::string(line, nameLen);
   }
}

int main(int argc, char* argv[])
{
   // initialize autorelease pool
   NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
   
   std::set<std::string> seen;
   char **read = *_NSGetEnviron(), **write = *_NSGetEnviron();
   
   for (; *read; read++) {
      std::string name = varname(*read);
      if (name.size() == 0 || seen.find(name) == seen.end()) {
         // Not a dupe
         *write = *read;
         write++;
         if (name.size() > 0) {
            seen.insert(name);
         }
      } else {
         NSLog(@"Ignoring duplicate environment variable: %s", *read);
      }
   }
   *write = NULL;
   
   // initialize language environment variables
   desktop::utils::initializeLang();
 
   // initialize log
   core::system::initializeLog("rdesktop",
                               core::system::kLogLevelWarning,
                               desktop::utils::userLogPath());
   
   // ignore SIGPIPE
   Error error = core::system::ignoreSignal(core::system::SigPipe);
   if (error)
      LOG_ERROR(error);
   
   // initialize application instance
   [NSApplication sharedApplication];
   
   // create our app delegate
   AppDelegate* appDelegate = [[[AppDelegate alloc] init] autorelease];
   [NSApp setDelegate: appDelegate];
   
   // run the event loop
   [NSApp run];

   // free the autorelease pool
   [pool drain];

   return EXIT_SUCCESS;
}


