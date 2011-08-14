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

struct RegistrationHandle::Impl
{
   FSEventStreamRef streamRef;
   Callbacks::FilesChanged onFilesChanged;
};

RegistrationHandle::RegistrationHandle()
   : pImpl_(new Impl())
{
}

RegistrationHandle::~RegistrationHandle()
{
}


namespace {

void fileEventCallback(ConstFSEventStreamRef streamRef,
                       void *pCallbackInfo,
                       size_t numEvents,
                       void *eventPaths,
                       const FSEventStreamEventFlags eventFlags[],
                       const FSEventStreamEventId eventIds[])
{
   // build path of file changes
   std::vector<FileChange> fileChanges;
   char **paths = (char**)eventPaths;
   for (std::size_t i=0; i<numEvents; i++)
   {
      fileChanges.push_back(FileChange(FileChange::Modified,
                                       FileEntry(paths[i])));
   }

   // fire callback
   ((RegistrationHandle*)pCallbackInfo)->pImpl_->onFilesChanged(fileChanges);
}

class CFRefScope : boost::noncopyable
{
public:
   explicit CFRefScope(CFTypeRef ref)
      : ref_(ref)
   {
   }
   virtual ~CFRefScope()
   {
      try
      {
         ::CFRelease(ref_);
      }
      catch(...)
      {
      }
   }
private:
   CFTypeRef ref_;
};

} // anonymous namespace

namespace detail {

// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks)
{
   // allocate file path
   CFStringRef filePathRef = ::CFStringCreateWithFileSystemRepresentation(
                                       kCFAllocatorDefault,
                                       filePath.absolutePath().c_str());
   if (filePathRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return;
   }
   CFRefScope filePathRefScope(filePathRef);

   // allocate paths array
   CFArrayRef pathsArrayRef = ::CFArrayCreate(kCFAllocatorDefault,
                                              (const void **)&filePathRef,
                                              1,
                                              NULL);
   if (pathsArrayRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return;
   }
   CFRefScope pathsArrayRefScope(pathsArrayRef);


   // create a new RegistrationHandle
   boost::shared_ptr<RegistrationHandle> pRegHandle(new RegistrationHandle());
   pRegHandle->pImpl_->onFilesChanged = callbacks.onFilesChanged;

   // FSEventStreamContext
   FSEventStreamContext context;
   context.version = 0;
   context.info = (void*) pRegHandle.get();
   context.retain = NULL;
   context.release = NULL;
   context.copyDescription = NULL;

   // create the stream and save a reference to it
   FSEventStreamRef streamRef = ::FSEventStreamCreate(
                  kCFAllocatorDefault,
                  &fileEventCallback,
                  &context,
                  pathsArrayRef,
                  kFSEventStreamEventIdSinceNow,
                  1,
                  kFSEventStreamCreateFlagNone);
   if (streamRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return;
   }

   // schedule with the run loop
   ::FSEventStreamScheduleWithRunLoop(streamRef,
                                      ::CFRunLoopGetCurrent(),
                                      kCFRunLoopDefaultMode);

   // start the event stream (check for errors and release if necessary
   if (!::FSEventStreamStart(streamRef))
   {
      ::FSEventStreamInvalidate(streamRef);
      ::FSEventStreamRelease(streamRef);

      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return;

   }

   // perform file listing
   FileListing fileListing(filePath);

   // set the stream on the RegistrationHandle and notify the caller
   // that we are now registered
   pRegHandle->pImpl_->streamRef = streamRef;
   callbacks.onRegistered(pRegHandle, fileListing);
}

// unregister a file monitor
void unregisterMonitor(boost::shared_ptr<RegistrationHandle> pHandle)
{
   ::FSEventStreamStop(pHandle->pImpl_->streamRef);
   ::FSEventStreamInvalidate(pHandle->pImpl_->streamRef);
   ::FSEventStreamRelease(pHandle->pImpl_->streamRef);
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

   



