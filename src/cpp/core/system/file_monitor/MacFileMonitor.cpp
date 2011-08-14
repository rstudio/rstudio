/*
 * MacFileMonitor.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/file_monitor/FileMonitor.hpp>

#include <CoreServices/CoreServices.h>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/system/System.hpp>
#include <core/Thread.hpp>

namespace core {
namespace system {
namespace file_monitor {

namespace {

void myCallback(ConstFSEventStreamRef streamRef,
                void *clientCallBackInfo,
                size_t numEvents,
                void *eventPaths,
                const FSEventStreamEventFlags eventFlags[],
                const FSEventStreamEventId eventIds[])
{
   char **paths = (char**)eventPaths;

   std::cerr << "Callback called" << std::endl;
   for (std::size_t i=0; i<numEvents; i++)
   {
      printf("Change %llu in %s, flags %u\n",
             eventIds[i],
             paths[i],
             eventFlags[i]);
   }

}

void listenerThread()
{
   try
   {
      CFStringRef myPath CFSTR("/Users/jjallaire/Projects");
      CFArrayRef pathsToWatch = CFArrayCreate(NULL, (const void **)&myPath, 1,
                                              NULL);
      FSEventStreamContext* callbackInfo = NULL;
      FSEventStreamRef stream;
      CFAbsoluteTime latency = 1;

      stream = ::FSEventStreamCreate(NULL,
                                     &myCallback,
                                     callbackInfo,
                                     pathsToWatch,
                                     kFSEventStreamEventIdSinceNow,
                                     latency,
                                     kFSEventStreamCreateFlagNone);


      ::FSEventStreamScheduleWithRunLoop(stream,
                                         ::CFRunLoopGetCurrent(),
                                         kCFRunLoopDefaultMode);

      ::FSEventStreamStart(stream);

      while (true)
      {
         SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 10, false);

         std::cerr << "Ran run loop: " << reason << std::endl;
      }





   }
   CATCH_UNEXPECTED_EXCEPTION
}



} // anonymous namespace

struct RegistrationHandle::Impl
{
};

RegistrationHandle::RegistrationHandle()
   : pImpl_(new Impl())
{
}

RegistrationHandle::~RegistrationHandle()
{
}

namespace detail {


// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks)
{

}

// unregister a file monitor
void unregisterMonitor(const RegistrationHandle& handle)
{

}

void run(const boost::function<void()>& checkForInput)
{
   // ensure we have a run loop for this thread
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // process the run loop for 1 second
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1, false);

      // if we were stopped then break
      if (reason == kCFRunLoopRunStopped)
         break;

      // if there is nothing the run loop then sleep for 250ms (so we don't spin)
      if (reason == kCFRunLoopRunFinished)
         boost::this_thread::sleep(boost::posix_time::milliseconds(250));

      // check for input
      checkForInput();
   }

}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



