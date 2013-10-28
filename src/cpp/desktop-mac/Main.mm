

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import <AppKit/NSApplication.h>
#import <Foundation/NSAutoreleasePool.h>

#import "AppDelegate.h"
#import "Utils.hpp"

using namespace core;

int main(int argc, char* argv[])
{
   // initialize autorelease pool
   NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
   
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


