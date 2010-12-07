/*
 * DirectoryMonitor.cpp
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

#include <algorithm>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
    
namespace core {
namespace system {

namespace {   

Error fileListing(const FilePath& filePath, std::vector<FileInfo>* pFiles)
{
   // get the children
   std::vector<FilePath> children ;
   Error error = filePath.children(&children);
   if (error)
      return error;
  
   // add to listing
   pFiles->clear();
   for (std::vector<FilePath>::const_iterator it = children.begin();
        it != children.end();
        ++it)
   {
      pFiles->push_back(FileInfo(*it));
   }
   
   // sort the listing
   std::sort(pFiles->begin(), pFiles->end(), compareFileInfoPaths);
   
   return Success();
}
   
}
   
struct DirectoryMonitor::Impl
{
   FilePath directory;
   std::vector<FileInfo> previousListing;
   DirectoryMonitor::Filter filter;
   
   void clear()
   {
      directory = FilePath();
      previousListing.clear();
      filter = DirectoryMonitor::Filter();
   }
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
   
   // set path
   pImpl_->directory = filePath;
   
   // set filter
   pImpl_->filter = filter;
   
   // update the listing
   error = fileListing(pImpl_->directory, &pImpl_->previousListing);
   if (error)
   {
      pImpl_->clear();
      return error;
   }
   else
   {
      return Success();
   }
}
   
Error DirectoryMonitor::checkForEvents(std::vector<FileChangeEvent>* pEvents)
{
   // clear existing events
   pEvents->clear();
   
   // if we have not been started then return no events
   if (pImpl_->directory.empty())
      return Success();
   
   // if the directory has been deleted then stop
   if (!pImpl_->directory.exists())
      return stop();
   
   // do a file listing
   std::vector<FileInfo> currentListing ;
   Error error = fileListing(pImpl_->directory, &currentListing);
   if (error)
      return error ;
   
   // find removed files
   std::vector<FileInfo> removedFiles ;
   std::set_difference(pImpl_->previousListing.begin(),
                       pImpl_->previousListing.end(),
                       currentListing.begin(),
                       currentListing.end(),
                       std::back_inserter(removedFiles),
                       compareFileInfoPaths);
   // enque removed events
   for (std::vector<FileInfo>::const_iterator it = removedFiles.begin();
        it != removedFiles.end();
        ++it)
   {
      if (shouldFireEventForFile(*it))
         pEvents->push_back(FileChangeEvent(FileChangeEvent::FileRemoved, *it));
   }
   
   // find updated files (could be added or modified)
   std::vector<FileInfo> updatedFiles;
   std::set_difference(currentListing.begin(),
                       currentListing.end(),
                       pImpl_->previousListing.begin(),
                       pImpl_->previousListing.end(),
                       std::back_inserter(updatedFiles));
   
   // determine whether each update is an add or a modify
   for (std::vector<FileInfo>::const_iterator it = updatedFiles.begin();
        it != updatedFiles.end();
        ++it)
   {
      if (shouldFireEventForFile(*it))
      {
         FileChangeEvent::Type eventType ;
         if (std::binary_search(pImpl_->previousListing.begin(),
                                pImpl_->previousListing.end(),
                                *it,
                                compareFileInfoPaths))
         {
            // was in the previous listing -- modified
            eventType = FileChangeEvent::FileModified;
         }
         else
         {
            // was not in the previous listing -- added
            eventType = FileChangeEvent::FileAdded;
         }
         
         // add the event
         pEvents->push_back(FileChangeEvent(eventType, *it));
      }
   }
   
   // update previous listing
   pImpl_->previousListing = currentListing;
   
   
   return Success();
}
      
Error DirectoryMonitor::stop()
{
   // reset impl state
   pImpl_->clear();
   
   return Success();
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

   



