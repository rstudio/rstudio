/*
 * MacFileMonitor.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/FileMonitor.hpp>

#include <CoreServices/CoreServices.h>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/bind.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/Thread.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

namespace rstudio {
namespace core {
namespace system {
namespace file_monitor {

namespace {

class DirectoryHandle : boost::noncopyable
{
public:
   DirectoryHandle(const std::string& path)
      : fd_(-1)
   {
      const char* cpath = path.c_str();
      auto f = [&]() { return ::open(cpath, O_DIRECTORY); };
      Error error = posix::posixCall<int>(f, ERROR_LOCATION, &fd_);
      if (error)
         LOG_ERROR(error);
   }

   ~DirectoryHandle()
   {
      if (fd_ != -1)
      {
         auto f = [&]() { return ::close(fd_); };
         safePosixCall<int>(f, ERROR_LOCATION);
      }
   }

   FilePath currentPath()
   {
      // read the path associated with the descriptor
      char path[PATH_MAX];
      auto f = [&]() { return ::fcntl(fd_, F_GETPATH, path); };
      Error error = posix::posixCall<int>(f, ERROR_LOCATION);
      if (error)
         return FilePath();

      // validate the directory still exists
      FilePath currPath(path);
      if (!currPath.isDirectory())
         return FilePath();

      // okay, return the path
      return currPath;
   }

private:
   int fd_;
};

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext(const FilePath& rootPath)
      : rootPath(rootPath),
        rootHandle(rootPath.getAbsolutePathNative()),
        streamRef(nullptr),
        recursive(false)
   {
      handle = Handle((void*)this);
   }

   virtual ~FileEventContext()
   {
   }

   Handle handle;
   FilePath rootPath;
   DirectoryHandle rootHandle;
   FSEventStreamRef streamRef;
   bool recursive;
   boost::function<bool(const FileInfo&)> filter;
   tree<FileInfo> fileTree;
   Callbacks callbacks;
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

   // bail if we don't have callbacks (we wouldn't if a callback snuck
   // through to us even after we failed to fully initialize the file monitor
   // (e.g. if there was an error during file listing)
   if (!pContext->callbacks.onFilesChanged)
      return;

   // de-register the file monitor if the root path has changed or been
   // removed
   //
   // NOTE: on macOS Catalina, we observed spurious 'RootChanged' events
   // delivered causing the file monitor to erroneously detach. protect
   // against this by also double-checking whether the original path
   // monitored and the path reported by the file handle match up
   //
   // We check for filesystem equivalence (not path equivalence) since macOS 
   // Catalina can issue a RootChanged event for conversion to/from a 
   // canonicalized /System/Volumes/Data path.
   //
   // https://github.com/rstudio/rstudio/issues/4755
   if (!pContext->rootPath.isEquivalentTo(pContext->rootHandle.currentPath()))
   {
      // propagate error to client
      Error error = fileNotFoundError(pContext->rootPath.getAbsolutePath(),
                                      ERROR_LOCATION);
      pContext->callbacks.onMonitoringError(error);

      // unregister this monitor (this is done via postback from the
      // main file_monitor loop so that the monitor Handle can be tracked)
      file_monitor::unregisterMonitor(pContext->handle);

      return;
   }

   char **paths = (char**) eventPaths;
   for (std::size_t i = 0; i < numEvents; i++)
   {
      // make a copy of the path and strip off trailing / if necessary
      std::string path(paths[i]);
      boost::algorithm::trim_right_if(path, boost::algorithm::is_any_of("/"));

      // if we aren't in recursive mode then ignore this if it isn't for
      // the root directory
      if (!pContext->recursive && (path != pContext->rootPath.getAbsolutePath()))
         continue;

      // get FileInfo for this directory
      FileInfo fileInfo(path, true);

      // apply the filter (if any)
      if (!pContext->filter || pContext->filter(fileInfo))
      {
         // check for need to do recursive scan
         bool recursive = pContext->recursive &&
                          (eventFlags[i] & kFSEventStreamEventFlagMustScanSubDirs);

         // process changes
         Error error = impl::discoverAndProcessFileChanges(
                                             fileInfo,
                                             recursive,
                                             pContext->filter,
                                             &(pContext->fileTree),
                                             pContext->callbacks.onFilesChanged);
         if (error &&
            (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation())))
         {
            LOG_ERROR(error);
         }
      }
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
Handle registerMonitor(const FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks)
{
   // allocate file path
   CFStringRef filePathRef = ::CFStringCreateWithCString(
                                       kCFAllocatorDefault,
                                       filePath.getAbsolutePath().c_str(),
                                       kCFStringEncodingUTF8);
   if (filePathRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return Handle();
   }
   CFRefScope filePathRefScope(filePathRef);

   // allocate paths array
   CFArrayRef pathsArrayRef = ::CFArrayCreate(kCFAllocatorDefault,
                                              (const void **)&filePathRef,
                                              1,
                                              nullptr);
   if (pathsArrayRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return Handle();
   }
   CFRefScope pathsArrayRefScope(pathsArrayRef);

   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext(filePath);
   pContext->recursive = recursive;
   pContext->filter = filter;
   std::unique_ptr<FileEventContext> autoPtrContext(pContext);
   FSEventStreamContext context;
   context.version = 0;
   context.info = (void*) pContext;
   context.retain = nullptr;
   context.release = nullptr;
   context.copyDescription = nullptr;

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
   if (pContext->streamRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return Handle();
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
      return Handle();
   }

   // scan the files
   core::system::FileScannerOptions options;
   options.recursive = recursive;
   options.yield = true;
   options.filter = filter;
   Error error = scanFiles(FileInfo(filePath), options, &pContext->fileTree);
   if (error)
   {
       // stop, invalidate, release
       stopInvalidateAndReleaseEventStream(pContext->streamRef);

       // return error
       callbacks.onRegistrationError(error);
       return Handle();
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the callbacks
   pContext->callbacks = callbacks;

   // we are going to pass the context pointer to the client (as the Handle)
   // so we release it here to relinquish ownership
   autoPtrContext.release();

   // notify the caller that we have successfully registered
   callbacks.onRegistered(pContext->handle, pContext->fileTree);

   // return the handle
   return pContext->handle;
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // cast to context
   FileEventContext* pContext = (FileEventContext*)(handle.pData);

   // stop, invalidate, release
   stopInvalidateAndReleaseEventStream(pContext->streamRef);

   // let the client know we are unregistered (note this call should always
   // be prior to delete pContext below!)
   pContext->callbacks.onUnregistered(handle);

   // delete the context
   delete pContext;
}

void run(const boost::function<void()>& checkForInput)
{
   // ensure we have a run loop for this thread (not sure if this is
   // strictly necessary but it is not harmful)
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // check the run loop
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0, true);

      // if we handled a source then run again
      if (reason == kCFRunLoopRunHandledSource)
      {
         continue;
      }

      else if (reason == kCFRunLoopRunStopped)
      {
         LOG_WARNING_MESSAGE("Unexpected stop of file monitor run loop");
         break;
      }

      // check for input
      checkForInput();
   }
}

void stop()
{
   // no need to call CFRunLoopStop(CFRunLoopGetCurrent()) because control
   // is already outside of the run loop logic (above).
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 
} // namespace rstudio

   



