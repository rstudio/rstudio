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
   
   // find added files
   std::vector<FileInfo> addedFiles;
   std::set_difference(currentListing.begin(),
                       currentListing.end(),
                       pImpl_->previousListing.begin(),
                       pImpl_->previousListing.end(),
                       std::back_inserter(addedFiles),
                       compareFileInfoPaths);
   
   // enque add events
   for (std::vector<FileInfo>::const_iterator it = addedFiles.begin();
        it != addedFiles.end();
        ++it)
   {
      if (shouldFireEventForFile(*it))
         pEvents->push_back(FileChangeEvent(FileChangeEvent::FileAdded, *it));
   }

   // get the subset of files in both lists and then compare for modification
   std::vector<FileInfo> commonCurrentFiles, commonPrevFiles;
   std::set_intersection(currentListing.begin(),
                         currentListing.end(),
                         pImpl_->previousListing.begin(),
                         pImpl_->previousListing.end(),
                         std::back_inserter(commonCurrentFiles),
                         compareFileInfoPaths);
   std::set_intersection(pImpl_->previousListing.begin(),
                         pImpl_->previousListing.end(),
                         currentListing.begin(),
                         currentListing.end(),
                         std::back_inserter(commonPrevFiles),
                         compareFileInfoPaths);
   // enque modified events
   for (std::size_t i=0; i<commonCurrentFiles.size(); i++)
   {
      if (commonCurrentFiles[i].lastWriteTime() !=
          commonPrevFiles[i].lastWriteTime())
      {
         if (shouldFireEventForFile(commonCurrentFiles[i]))
         {
            pEvents->push_back(FileChangeEvent(FileChangeEvent::FileModified,
                                               commonCurrentFiles[i]));
         }
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

   



