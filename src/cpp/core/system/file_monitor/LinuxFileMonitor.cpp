/*
 * LinuxFileMonitor.cpp
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

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/inotify.h>

#include <set>

#include <boost/utility.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <boost/multi_index_container.hpp>
#include <boost/multi_index/hashed_index.hpp>
#include <boost/multi_index/member.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/FileInfo.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

#include "config.h"

namespace rstudio {
namespace core {
namespace system {
namespace file_monitor {

namespace {

struct Watch
{
   Watch()
      : wd(-1), path()
   {
   }

   Watch(int wd, const std::string& path)
      : wd(wd), path(path)
   {
   }

   bool empty() const { return path.empty(); }

   int wd;
   std::string path;

   bool operator < (const Watch& other) const
   {
      return this->wd < other.wd;
   }
};

// boost::multi_index_container based class for managing a set of watches
class Watches
{
public:

   void insert(const Watch& watch)
   {
      watches_.insert(watch);
   }

   void erase(const Watch& watch)
   {
      watches_.get<wd>().erase(watch.wd);
   }

   Watch find(int wd) const
   {
      WatchesByDescriptor::const_iterator it = descriptorIndex().find(wd);
      if (it != descriptorIndex().end())
         return *it;
      else
         return Watch();
   }

   Watch find(const std::string& path) const
   {
      WatchesByPath::const_iterator it = pathIndex().find(path);
      if (it != pathIndex().end())
         return *it;
      else
         return Watch();
   }

   void forEach(const boost::function<void(const Watch&)> op) const
   {
      std::for_each(descriptorIndex().begin(), descriptorIndex().end(), op);
   }

   void clear()
   {
      watches_ = WatchesContainer();
   }

private:

   struct wd {};
   struct path {};

   typedef boost::multi_index::multi_index_container<

      Watch,

      boost::multi_index::indexed_by<

         boost::multi_index::hashed_unique<
            boost::multi_index::tag<wd>,
            boost::multi_index::member<Watch,
                                       int,
                                       &Watch::wd>
         >,

         boost::multi_index::hashed_unique<
            boost::multi_index::tag<path>,
            boost::multi_index::member<Watch,
                                       std::string,
                                       &Watch::path>
         >
      >
   > WatchesContainer;

   typedef WatchesContainer::index<wd>::type WatchesByDescriptor;
   typedef WatchesContainer::index<path>::type WatchesByPath;

   const WatchesByDescriptor& descriptorIndex() const
   {
      return watches_.get<wd>();
   }

   const WatchesByPath& pathIndex() const
   {
      return watches_.get<path>();
   }

   WatchesContainer watches_;
};


class FileEventContext : boost::noncopyable
{
public:
   FileEventContext()
      : fd(-1),
        recursive(false)
   {
      handle = Handle((void*)this);
   }
   virtual ~FileEventContext() {}
   Handle handle;
   int fd;
   Watches watches;
   FilePath rootPath;
   bool recursive;
   boost::function<bool(const FileInfo&)> filter;
   tree<FileInfo> fileTree;
   Callbacks callbacks;
};

void terminateWithMonitoringError(FileEventContext* pContext,
                                  const Error& error)
{
   pContext->callbacks.onMonitoringError(error);

   // unregister this monitor (this is done via postback from the
   // main file_monitor loop so that the monitor Handle can be tracked)
   file_monitor::unregisterMonitor(pContext->handle);
}

Error addWatch(const FileInfo& fileInfo,
               const FilePath& rootPath,
               bool allowRootSymlink,
               int fd,
               Watches* pWatches)
{
   // NOTE: both inotify_add_watch and std::set::insert gracefully
   // handle duplicate additions, inotify_add_watch by modifying the
   // existing watch and returning the same watch descriptor, and
   // set::set by simply doing nothing. therefore, we don't bother
   // checking to see if the watch exists and don't generally worry
   // about adding duplicate watches

   // define watch mask
   uint32_t mask = 0;
   mask |= IN_CREATE;
   mask |= IN_DELETE;
   mask |= IN_MODIFY;
   mask |= IN_MOVED_TO;
   mask |= IN_MOVED_FROM;
   mask |= IN_Q_OVERFLOW;

   // add IN_DONT_FOLLOW unless we are explicitly allowing root symlinks
   // and this is a watch for the root path
   if (!allowRootSymlink ||
       (fileInfo.absolutePath() != rootPath.getAbsolutePath()))
   {
      mask |= IN_DONT_FOLLOW;
   }

   // initialize watch
   int wd = ::inotify_add_watch(fd, fileInfo.absolutePath().c_str(), mask);
   if (wd < 0)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", fileInfo.absolutePath());
      return error;
   }

   // record it
   pWatches->insert(Watch(wd, fileInfo.absolutePath()));

   // return success
   return Success();
}

boost::function<Error(const FileInfo&)> addWatchFunction(
                                           FileEventContext* pContext,
                                           bool allowRootSymlink = false)
{
   return boost::bind(addWatch,
                        _1,
                        pContext->rootPath,
                        allowRootSymlink,
                        pContext->fd,
                        &pContext->watches);
}

void removeWatch(int fd, const Watch& watch)
{
   // remove the watch
   int result = ::inotify_rm_watch(fd, watch.wd);

   // log error if it isn't EINVAL (which is expected if e.g. the
   // filesystem has been unmounted or the root directory has been deleted)
   if (result < 0 && errno != EINVAL)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", watch.path);
      LOG_ERROR(error);
   }
}

void removeAllWatches(FileEventContext* pContext)
{
   pContext->watches.forEach(boost::bind(removeWatch,
                                          pContext->fd,
                                          _1));
   pContext->watches.clear();
}

void closeContext(FileEventContext* pContext)
{
   // remove all watches
   removeAllWatches(pContext);

   // close the file descriptor
   if (pContext->fd >= 0)
   {
      // close the descriptor
      safePosixCall<int>(boost::bind(::close, pContext->fd), ERROR_LOCATION);

      // reset file descriptor
      pContext->fd = -1;
   }
}

Error processEvent(FileEventContext* pContext,
                   struct inotify_event* pEvent,
                   std::vector<FileChangeEvent>* pFileChanges)
{
   // determine event type
   FileChangeEvent::Type eventType = FileChangeEvent::None;
   if (pEvent->mask & IN_CREATE)
      eventType = FileChangeEvent::FileAdded;
   else if (pEvent->mask & IN_DELETE)
      eventType = FileChangeEvent::FileRemoved;
   else if (pEvent->mask & IN_MODIFY)
      eventType = FileChangeEvent::FileModified;
   else if (pEvent->mask & IN_MOVED_TO)
      eventType = FileChangeEvent::FileAdded;
   else if (pEvent->mask & IN_MOVED_FROM)
      eventType = FileChangeEvent::FileRemoved;

   // return event if we got a valid event type and the event applies to a
   // child of the monitored directory (len == 0 occurs for root element)
   if ((eventType != FileChangeEvent::None) && (pEvent->len > 0))
   {
      // find the FileInfo for this wd (ignore if we can't find one)
      Watch watch = pContext->watches.find(pEvent->wd);
      if (watch.empty())
         return Success();

      // get an iterator to the parent dir
      tree<FileInfo>::iterator parentIt = impl::findFile(
                                                   pContext->fileTree.begin(),
                                                   pContext->fileTree.end(),
                                                   watch.path);

      // if we can't find a parent then return (this directory may have
      // been excluded from scanning due to a filter)
      if (parentIt == pContext->fileTree.end())
         return Success();

      // get file info
      FilePath filePath = FilePath(parentIt->absolutePath()).completePath(
         pEvent->name);


      // if the file exists then collect as many extended attributes
      // as necessary -- otherwise just record path and dir status
      FileInfo fileInfo;
      if (filePath.exists())
      {
         fileInfo = FileInfo(filePath, filePath.isSymlink());
      }
      else
      {
         fileInfo = FileInfo(filePath.getAbsolutePath(), pEvent->mask & IN_ISDIR);
      }

      // if this doesn't meet the filter then ignore
      if (pContext->filter && !pContext->filter(fileInfo))
         return Success();

      // handle the various types of actions
      switch(eventType)
      {
         case FileChangeEvent::FileRemoved:
         {
            // generate events
            FileChangeEvent event(FileChangeEvent::FileRemoved, fileInfo);
            std::vector<FileChangeEvent> removeEvents;
            impl::processFileRemoved(parentIt,
                                     event,
                                     pContext->recursive,
                                     &pContext->fileTree,
                                     &removeEvents);

            // for each directory remove event remove any watches we have for it
            for (const FileChangeEvent& event : removeEvents)
            {
               if (event.fileInfo().isDirectory())
               {
                  Watch watch = pContext->watches.find(
                                             event.fileInfo().absolutePath());
                  if (!watch.empty())
                  {
                     removeWatch(pContext->fd, watch);
                     pContext->watches.erase(watch);
                  }
               }
            }

            // copy to the target events
            std::copy(removeEvents.begin(),
                      removeEvents.end(),
                      std::back_inserter(*pFileChanges));

            break;
         }
         case FileChangeEvent::FileAdded:
         {
            FileChangeEvent event(FileChangeEvent::FileAdded, fileInfo);
            Error error = impl::processFileAdded(parentIt,
                                                 event,
                                                 pContext->recursive,
                                                 pContext->filter,
                                                 addWatchFunction(pContext),
                                                 &pContext->fileTree,
                                                 pFileChanges);
            // log the error if it wasn't no such file/dir (this can happen
            // in the normal course of business if a file is deleted between
            // the time the change is detected and we try to inspect it)
            if (error &&
               (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation())))
            {
               LOG_ERROR(error);
            }
            break;
         }
         case FileChangeEvent::FileModified:
         {
            FileChangeEvent event(FileChangeEvent::FileModified, fileInfo);
            impl::processFileModified(parentIt,
                                      event,
                                      &pContext->fileTree,
                                      pFileChanges);
            break;
         }
         case FileChangeEvent::None:
            break;
      }
   }



   return Success();
}


Handle registrationFailure(int errorNumber,
                           FileEventContext* pContext,
                           const Callbacks& callbacks,
                           const ErrorLocation& location)
{
   closeContext(pContext);
   callbacks.onRegistrationError(systemError(errorNumber, location));
   return Handle();
}


} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks)
{
   // create and allocate FileEventContext
   // (also pack into unique_ptr to auto-delete if we return early;
   // we'll relinquish ownership if we successfully register the monitor)
   FileEventContext* pContext = new FileEventContext();
   pContext->rootPath = filePath;
   pContext->recursive = recursive;
   pContext->filter = filter;
   std::unique_ptr<FileEventContext> contextScope(pContext);

   // init file descriptor
#ifdef HAVE_INOTIFY_INIT1
   pContext->fd = ::inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
   if (pContext->fd < 0)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);
#else
   // init file descriptor
   pContext->fd = ::inotify_init();
   if (pContext->fd < 0)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);

   // set non-blocking
   int flags = ::fcntl(pContext->fd, F_GETFL);
   if (flags == -1)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);
   if (::fcntl(pContext->fd, F_SETFL, flags | O_NONBLOCK) == -1)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);

   // set close on exec
   int fdFlags = ::fcntl(pContext->fd, F_GETFD);
   if (fdFlags == -1)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);
   if (::fcntl(pContext->fd, F_SETFD, fdFlags | FD_CLOEXEC) == -1)
      return registrationFailure(errno, pContext, callbacks, ERROR_LOCATION);
#endif

   // scan the files (use callback to setup watches)
   FileScannerOptions options;
   options.recursive = recursive;
   options.yield = true;
   options.filter = filter;
   options.onBeforeScanDir = addWatchFunction(pContext, true);
   Error error = scanFiles(FileInfo(filePath), options, &pContext->fileTree);
   if (error)
   {
       // close context
       closeContext(pContext);

       // return error
       callbacks.onRegistrationError(error);
       return Handle();
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the callbacks
   pContext->callbacks = callbacks;

   // we are going to pass the context pointer to the client (as the Handle)
   // so we release it here to relinquish ownership
   contextScope.release();

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
   closeContext(pContext);

   // let the client know we are unregistered (note this call should always
   // be prior to delete pContext below!)
   pContext->callbacks.onUnregistered(handle);

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
      for (void* ctx : contexts)
      {
         // cast to context
         FileEventContext* pContext = (FileEventContext*)ctx;

         // bail if we don't have callbacks (we wouldn't if a callback snuck
         // through to us even after we failed to fully initialize the
         // file monitor  (e.g. if there was an error during file listing)
         if (!pContext->callbacks.onFilesChanged)
            continue;

         // check for context root directory deleted
         if (!pContext->rootPath.exists())
         {
            Error error = fileNotFoundError(
               pContext->rootPath.getAbsolutePath(),
                                            ERROR_LOCATION);
            terminateWithMonitoringError(pContext, error);
            continue;
         }

         // loop reading from this context's fd until EAGAIN or EWOULDBLOCK
         std::vector<FileChangeEvent> fileChanges;
         while (true)
         {
            // read
            int len = posix::posixCall<int>(
               boost::bind(
                  ::read,
                  pContext->fd,
                  eventBuffer,
                  kEventBufferLength));
            if (len < 0)
            {
               // don't terminate for errors indicating no events available
               // (silly ifdef here is to silence compiler warnings)
#if EAGAIN == EWOULDBLOCK
               if (errno == EAGAIN)
                  break;
#else
               if (errno == EAGAIN || errno == EWOULDBLOCK)
                  break;
#endif

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
               typedef struct inotify_event* EventPtr;
               EventPtr pEvent = (EventPtr)&eventBuffer[i];

               // buffer overflow is handled specially -- basically
               // we start over because we missed events
               if (pEvent->mask & IN_Q_OVERFLOW)
               {
                  // remove all watches
                  removeAllWatches(pContext);

                  // generate events based on scanning
                  Error error =impl::discoverAndProcessFileChanges(
                        FileInfo(pContext->rootPath),
                        pContext->recursive,
                        pContext->filter,
                        addWatchFunction(pContext, true),
                        &pContext->fileTree,
                        pContext->callbacks.onFilesChanged);
                  if (error)
                     terminateWithMonitoringError(pContext, error);

                  // always break here -- we've generated events based on
                  // a fresh scan so any other events in the queue would
                  // be duplicates
                  break;
               }

               // process the event
               Error error = processEvent(pContext, pEvent, &fileChanges);
               if (error)
               {
                  terminateWithMonitoringError(pContext, error);
                  break;
               }

               // advance to next event
               i += kEventSize + pEvent->len;
            }
         }

         // fire any events we got
         if (!fileChanges.empty())
            pContext->callbacks.onFilesChanged(fileChanges);
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
} // namespace rstudio

   



