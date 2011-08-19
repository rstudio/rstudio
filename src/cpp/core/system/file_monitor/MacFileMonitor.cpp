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

#include <core/system/FileMonitor.hpp>

#include <CoreServices/CoreServices.h>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/classification.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/Thread.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

namespace core {
namespace system {
namespace file_monitor {

namespace {

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext() : streamRef(NULL) {}
   virtual ~FileEventContext() {}
   FSEventStreamRef streamRef;
   tree<FileInfo> fileTree;
   Callbacks::FilesChanged onFilesChanged;
};

void fileEventCallback(ConstFSEventStreamRef streamRef,
                       void *pCallbackInfo,
                       size_t numEvents,
                       void *eventPaths,
                       const FSEventStreamEventFlags eventFlags[],
                       const FSEventStreamEventId eventIds[])
{
   // get context
   FileEventContext* pContext = (FileEventContext*)pCallbackInfo;

   // bail if we don't have onFilesChanged (we wouldn't if a callback snuck
   // through to us even after we failed to fully initialize the file monitor
   // (e.g. if there was an error during file listing)
   if (!pContext->onFilesChanged)
      return;

   char **paths = (char**)eventPaths;
   for (std::size_t i=0; i<numEvents; i++)
   {
      // check for root changed (unregister)
      if (eventFlags[i] & kFSEventStreamEventFlagRootChanged)
      {
         unregisterMonitor((Handle)pContext);
         return;
      }

      // make a copy of the path and strip off trailing / if necessary
      std::string path(paths[i]);
      boost::algorithm::trim_right_if(path, boost::algorithm::is_any_of("/"));

      // get FileInfo for this directory
      FileInfo fileInfo(path, true);

      // check for need to do recursive scan
      bool recursive = eventFlags[i] & kFSEventStreamEventFlagMustScanSubDirs;

      // process changes
      Error error = impl::discoverAndProcessFileChanges(fileInfo,
                                                        recursive,
                                                        &(pContext->fileTree),
                                                        pContext->onFilesChanged);
      if (error)
         LOG_ERROR(error);
   }
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

void invalidateAndReleaseEventStream(FSEventStreamRef streamRef)
{
   ::FSEventStreamInvalidate(streamRef);
   ::FSEventStreamRelease(streamRef);
}

void stopInvalidateAndReleaseEventStream(FSEventStreamRef streamRef)
{
   ::FSEventStreamStop(streamRef);
   invalidateAndReleaseEventStream(streamRef);
}

} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const Callbacks& callbacks)
{
   // allocate file path
   std::string path = filePath.absolutePath();
   CFStringRef filePathRef = ::CFStringCreateWithCString(
                                       kCFAllocatorDefault,
                                       filePath.absolutePath().c_str(),
                                       kCFStringEncodingUTF8);
   if (filePathRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return NULL;
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
      return NULL;
   }
   CFRefScope pathsArrayRefScope(pathsArrayRef);


   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext();
   std::auto_ptr<FileEventContext> autoPtrContext(pContext);
   FSEventStreamContext context;
   context.version = 0;
   context.info = (void*) pContext;
   context.retain = NULL;
   context.release = NULL;
   context.copyDescription = NULL;

   // create the stream and save a reference to it
   pContext->streamRef = ::FSEventStreamCreate(
                  kCFAllocatorDefault,
                  &fileEventCallback,
                  &context,
                  pathsArrayRef,
                  kFSEventStreamEventIdSinceNow,
                  1,
                  kFSEventStreamCreateFlagNoDefer |
                  kFSEventStreamCreateFlagWatchRoot);
   if (pContext->streamRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return NULL;
   }

   // schedule with the run loop
   ::FSEventStreamScheduleWithRunLoop(pContext->streamRef,
                                      ::CFRunLoopGetCurrent(),
                                      kCFRunLoopDefaultMode);

   // start the event stream (check for errors and release if necessary
   if (!::FSEventStreamStart(pContext->streamRef))
   {
      invalidateAndReleaseEventStream(pContext->streamRef);

      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return NULL;

   }

   // scan the files
   Error error = scanFiles(FileInfo(filePath), true, &pContext->fileTree);
   if (error)
   {
       // stop, invalidate, release
       stopInvalidateAndReleaseEventStream(pContext->streamRef);

       // return error
       callbacks.onRegistrationError(error);
       return NULL;
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the onFilesChanged callback so that the
   // client can receive events
   pContext->onFilesChanged = callbacks.onFilesChanged;

   // we are going to pass the context pointer to the client (as the Handle)
   // so we release it here to relinquish ownership
   autoPtrContext.release();

   // notify the caller that we have successfully registered
   callbacks.onRegistered((Handle)pContext, pContext->fileTree);

   // return the handle
   return (Handle)pContext;
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // cast to context
   FileEventContext* pContext = (FileEventContext*)handle;

   // stop, invalidate, release
   stopInvalidateAndReleaseEventStream(pContext->streamRef);

   // delete context
   delete pContext;
}

void run(const boost::function<void()>& checkForInput)
{
   // ensure we have a run loop for this thread (not sure if this is
   // strictly necessary but it is not harmful)
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // process the run loop for 1 second
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1, false);

      if (reason == kCFRunLoopRunStopped)
      {
         LOG_WARNING_MESSAGE("Unexpected stop of file monitor run loop");
         break;
      }

      // check for input
      checkForInput();
   }
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



