/*
 * INotifyDirectoryMonitor.cpp
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

#include <core/system/DirectoryMonitor.hpp>

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/inotify.h>

#include <boost/lexical_cast.hpp>

#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>

// inotify faq: http://inotify.aiken.cz/?section=inotify&page=faq&lang=en

// NOTE: inotify is not recursive. see the following discussions on issues
// associated with making it recursive:

//  http://mail.gnome.org/archives/dashboard-hackers/2004-October/msg00022.html 
//  http://jamiemcc.livejournal.com/10814.html
//

namespace core {
namespace system {

struct DirectoryMonitor::Impl
{
   Impl() : fd(-1), wd(-1) {}
   FilePath directory;
   int fd ;
   int wd ;
   DirectoryMonitor::Filter filter;
};


DirectoryMonitor::DirectoryMonitor()
   : pImpl_(new Impl())
{
}

DirectoryMonitor::~DirectoryMonitor()
{
   try
   {
      Error error = stop();
      if (error)
         LOG_ERROR(error);
   }
   catch(...)
   {
   }
}

Error DirectoryMonitor::start(const std::string& path, const Filter& filter)
{
   // stop existing monitor
   Error error = stop();
   if (error)
      LOG_ERROR(error);   
   
   // validate that it is a directory
   FilePath filePath(path);
   if (!filePath.isDirectory())
      return systemError(boost::system::errc::not_a_directory, ERROR_LOCATION);
   
   // set directory 
   pImpl_->directory = FilePath(path);
   
   // set filter
   pImpl_->filter = filter;
   
   // init file descriptor
   pImpl_->fd = ::inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
   if (pImpl_->fd < 0 ) 
      return systemError(errno, ERROR_LOCATION);

   // define watch mask
   uint32_t mask = 0 ;
   mask |= IN_CREATE;
   mask |= IN_DELETE;
   mask |= IN_MODIFY;
   mask |= IN_MOVED_TO;
   mask |= IN_MOVED_FROM;

   // initialize watch
   pImpl_->wd = ::inotify_add_watch(pImpl_->fd, path.c_str(), mask);
   if (pImpl_->wd < 0)
   {
      // attempt to stop (log any errors which occur)
      Error error = stop();
      if (error)
         LOG_ERROR(error);

      // return original error
      return systemError(errno, ERROR_LOCATION);
   }

   // success
   return Success();
}

// TODO: this implementation of inotify based monitoring will not pickup
// directory removals (because you need to setup a separate watch for 
// sub-directories if you want to see them removed). we didn't add this 
// capability for the time-being because it is an edge case and it would 
// significantly complicate our implementation. it may be that we 
// ultimately need to watch the entire home directory recursively (in a 
// background thread) to accomodate multiple clients (see notes above on
// implementing recursive inotify). as an accomdation to this, we manually
// fire removed events from our 

Error DirectoryMonitor::checkForEvents(std::vector<FileChangeEvent>* pEvents)
{
   // clear inbound events
   pEvents->clear();
   
   // if we have not been started then return no events
   if (pImpl_->fd < 0)
      return Success();
   
   // if the directory has been deleted then stop
   if (!pImpl_->directory.exists())
      return stop();

   // define event size and buffer (enough to hold 10 events)
   const int kEventSize = sizeof(struct inotify_event);
   const int kFilenameSizeEstimate = 20;
   const int kEventBufferLength = 10 * (kEventSize+kFilenameSizeEstimate);
	
   // loop until we hit EAGAIN or EWOULDBLOCK
   while (true)
   {      
      // setup event buffer and read
      char eventBuffer[kEventBufferLength];
      int len = ::read(pImpl_->fd, eventBuffer, kEventBufferLength);
      if (len < 0)
      {
         // break on errors which indicate thre are no events to read
         if (errno == EAGAIN || errno == EWOULDBLOCK)
            break;

         // otherwise return an error
         return systemError(errno, ERROR_LOCATION);
      }
         
      // iterate through the events
      int i = 0;
      while (i < len)
      {
         // get the event
         struct inotify_event* pEvent = (struct inotify_event*)&eventBuffer[i];
         
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
         else
         {
            LOG_WARNING_MESSAGE("Unexpected event type from inotify: " +
                                boost::lexical_cast<std::string>(pEvent->mask));
         }
         
         // return event if we got a valid event type and the event applies to a 
         // child of the monitored directory (len == 0 occurs for root element)
         if ((eventType != FileChangeEvent::None) && (pEvent->len > 0))
         {
            // first the path
            FilePath filePath = pImpl_->directory.complete(pEvent->name);
            
            // get file info (w/ extended attributes if this isn't a remove)
            FileInfo fileInfo;
            if (eventType != FileChangeEvent::FileRemoved)
            {
               // may have been added then deleted -- in this case we 
               // don't want to send any events
               if (filePath.exists())
                  fileInfo = FileInfo(filePath);
            }
            else
            {
               fileInfo = FileInfo(filePath.absolutePath(), 
                                   filePath.isDirectory());
            }
            
            // add the event
            if (!fileInfo.empty() && shouldFireEventForFile(fileInfo))
               pEvents->push_back(FileChangeEvent(eventType, fileInfo));
         }
        

         // next event (note: this code must execute to advance within the
         // event buffer so don't ever "continue" from the code above (always
         // let execution fall through to here)
         i += kEventSize + pEvent->len;
      }
   }
	
   // return events we got
   return Success();
}
    
Error DirectoryMonitor::stop()
{
   // remove watch
   Error removeWatchError;
   if (pImpl_->wd >= 0)
   {
      // remove the watch
      int res = ::inotify_rm_watch(pImpl_->fd, pImpl_->wd);
      
      // note error (but allow processing to continue so we can also close fd)
      if (res < 0)
      {
         // invalid argument is expected if the directory is gone
         if ((errno == EINVAL) && !pImpl_->directory.exists())
         {
            // don't set the error
         }
         else
         {
            removeWatchError = systemError(errno, ERROR_LOCATION);
         }
      }
      
      // reset watch descriptor
      pImpl_->wd = -1;
   }

   // close file descriptor
   Error closeError ;
   if (pImpl_->fd >= 0)
   {
      // close the descriptor
      int res = ::close(pImpl_->fd);

      // note error
      if (res < 0)
         closeError = systemError(errno, ERROR_LOCATION);

      // reset file descriptor
      pImpl_->fd = -1;
   }
   
   // reset directory and filter
   pImpl_->directory = FilePath();
   pImpl_->filter = Filter();
   
   // return error. if there is more than one error than log the 
   // remove watch error and return the close error
   if (closeError)
   {
      if (removeWatchError)
         LOG_ERROR(removeWatchError);

      return closeError;
   }
   else if (removeWatchError)
   {
      return removeWatchError;
   }
   else
   {
      return Success();
   }
}
   
std::string DirectoryMonitor::path() const 
{
   if (!pImpl_->directory.empty())
      return pImpl_->directory.absolutePath();
   else
      return std::string();
}

bool DirectoryMonitor::shouldFireEventForFile(const FileInfo& file)
{
   if (pImpl_->filter)
   {
      return pImpl_->filter(file);
   }
   else
   {
      return true;
   }
} 

} // namespace system
} // namespace core    

   



