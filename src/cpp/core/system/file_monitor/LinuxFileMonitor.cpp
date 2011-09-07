/*
 * LinuxFileMonitor.cpp
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

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/inotify.h>

#include <boost/utility.hpp>
#include <boost/foreach.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileInfo.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

// TODO: what happens if a symlink is the root entry

// TODO: investigate parallel package (multicore) interactions with file monitor

namespace core {
namespace system {
namespace file_monitor {

namespace {

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext()
      : fd(-1), recursive(false)
   {
      handle = Handle((void*)this);
   }
   virtual ~FileEventContext() {}
   Handle handle;
   int fd;
   FilePath rootPath;
   bool recursive;
   boost::function<bool(const FileInfo&)> filter;
   tree<FileInfo> fileTree;
   Callbacks callbacks;
};

void closeWatch(FileEventContext* pContext)
{
   // TODO: remove all watches



   // close the file descriptor
   if (pContext->fd >= 0)
   {
      // close the descriptor
      safePosixCall<int>(boost::bind(::close, pContext->fd), ERROR_LOCATION);

      // reset file descriptor
      pContext->fd = -1;
   }
}


void terminateWithMonitoringError(FileEventContext* pContext,
                                  const Error& error)
{
   pContext->callbacks.onMonitoringError(error);

   // unregister this monitor (this is done via postback from the
   // main file_monitor loop so that the monitor Handle can be tracked)
   file_monitor::unregisterMonitor(pContext->handle);
}


} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks)
{
   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext();
   pContext->rootPath = filePath;
   pContext->recursive = recursive;
   pContext->filter = filter;
   std::auto_ptr<FileEventContext> autoPtrContext(pContext);

   // init file descriptor
   pContext->fd = ::inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
   if (pContext->fd < 0 )
   {
      callbacks.onRegistrationError(systemError(errno, ERROR_LOCATION));
      return Handle();
   }

   // scan the files (use callback to setup watches)
   // TODO: add callback interface for watch setup
   Error error = scanFiles(FileInfo(filePath),
                           recursive,
                           filter,
                           &pContext->fileTree);
   if (error)
   {
       // close watch
       closeWatch(pContext);

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

   // close context
   closeWatch(pContext);

   // let the client know we are unregistered (note this call should always
   // be prior to delete pContext below!)
   pContext->callbacks.onUnregistered();

   // delete the context
   delete pContext;
}

void run(const boost::function<void()>& checkForInput)
{
   // create event buffer (enough to hold 5000 events)
   const int kEventSize = sizeof(struct inotify_event);
   const int kFilenameSizeEstimate = 20;
   const int kEventBufferLength = 5000 * (kEventSize+kFilenameSizeEstimate);
   char eventBuffer[kEventBufferLength];

   while(true)
   {
      std::list<void*> contexts = impl::activeEventContexts();
      BOOST_FOREACH(void* ctx, contexts)
      {
         // cast to context and get fd
         FileEventContext* pContext = (FileEventContext*)ctx;

         // loop reading from this context's fd until EAGAIN or EWOULDBLOCK
         while (true)
         {
            // read
            int len = posixCall<int>(boost::bind(::read,
                                                 pContext->fd,
                                                 eventBuffer,
                                                 kEventBufferLength));
            if (len < 0)
            {
               // don't terminate for errors indicating no events available
               if (errno == EAGAIN || errno == EWOULDBLOCK)
                  break;

               // otherwise terminate this watch (notify user and break
               // out of the read loop for this context)
               terminateWithMonitoringError(pContext,
                                            systemError(errno, ERROR_LOCATION));
               break;
            }


            // iterate through the events
            int i = 0;
            while (i < len)
            {
               // get the event
               struct inotify_event* pEvent =
                                       (struct inotify_event*)&eventBuffer[i];


               // TODO: process events



               // next event (note: this code must execute to advance within
               // the event buffer so don't ever "continue" from the code above
               // (always let execution fall through to here)
               i += kEventSize + pEvent->len;
            }
         }
      }

      // check for input (register/unregister of monitors)
      checkForInput();
   }
}

void stop()
{
   // nothing to do here
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



