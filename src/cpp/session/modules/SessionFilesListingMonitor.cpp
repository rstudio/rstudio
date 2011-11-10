/*
 * SessionFilesListingMonitor.cpp
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

#include "SessionFilesListingMonitor.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileInfo.hpp>
#include <core/FilePath.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionSourceControl.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace files {

Error FilesListingMonitor::start(const FilePath& filePath, json::Array* pJsonFiles)
{
   // always stop existing
   stop();

   // scan the directory (populates pJsonFiles out parameter)
   std::vector<FilePath> files;
   Error error = listFiles(filePath, &files, pJsonFiles);
   if (error)
      return error;

   // copy the file listing into a vector of FileInfo which we will order so that it can
   // be compared with the initial scan of the file montor for changes
   std::vector<FileInfo> prevFiles;
   std::transform(files.begin(),
                  files.end(),
                  std::back_inserter(prevFiles),
                  core::toFileInfo);

   // kickoff new monitor
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(&FilesListingMonitor::onRegistered,
                                    this, _1, filePath, prevFiles, _2);
   cb.onRegistrationError =  boost::bind(core::log::logError, _1, ERROR_LOCATION);
   cb.onFilesChanged = boost::bind(module_context::enqueFileChangedEvents, filePath, _1);
   cb.onMonitoringError = boost::bind(core::log::logError, _1, ERROR_LOCATION);
   cb.onUnregistered = boost::bind(&FilesListingMonitor::onUnregistered, this, _1);
   core::system::file_monitor::registerMonitor(filePath,
                                               false,
                                               module_context::fileListingFilter,
                                               cb);

   return Success();
}

void FilesListingMonitor::stop()
{
   // reset monitored path and unregister any existing handle
   currentPath_ = FilePath();
   if (!currentHandle_.empty())
   {
      core::system::file_monitor::unregisterMonitor(currentHandle_);
      currentHandle_ = core::system::file_monitor::Handle();
   }
}

const FilePath& FilesListingMonitor::currentMonitoredPath() const
{
   return currentPath_;
}

void FilesListingMonitor::onRegistered(core::system::file_monitor::Handle handle,
                                       const FilePath& filePath,
                                       const std::vector<FileInfo>& prevFiles,
                                       const tree<core::FileInfo>& files)
{
   // set path and current handle
   currentPath_ = filePath;
   currentHandle_ = handle;

   // compare the previously returned listing with the initial scan to see if any
   // file changes occurred between listings
   std::vector<core::system::FileChangeEvent> events;
   core::system::collectFileChangeEvents(prevFiles.begin(),
                                         prevFiles.end(),
                                         files.begin(files.begin()),
                                         files.end(files.begin()),
                                         module_context::fileListingFilter,
                                         &events);

   // enque any events we discovered
   if (!events.empty())
      module_context::enqueFileChangedEvents(filePath, events);
}

void FilesListingMonitor::onUnregistered(core::system::file_monitor::Handle handle)
{
   // typically we clear our internal state explicitly when a new registration
   // comes in. however, it is possible that our monitor could be unregistered
   // as a result of an error which occurs during monitoring. in this case
   // we clear our state explicitly here as well
   if (currentHandle_ == handle)
   {
      currentPath_ = FilePath();
      currentHandle_ = core::system::file_monitor::Handle();
   }
}

Error FilesListingMonitor::listFiles(const FilePath& rootPath,
                                     std::vector<FilePath>* pFiles,
                                     json::Array* pJsonFiles)
{
   // enumerate the files
   pFiles->clear();
   core::Error error = rootPath.children(pFiles) ;
   if (error)
      return error;

   // get source control status (merely log errors doing this)
   source_control::StatusResult vcsStatus;
   error = source_control::status(rootPath, &vcsStatus);
   if (error)
      LOG_ERROR(error);

   // sort the files by name
   std::sort(pFiles->begin(), pFiles->end(), core::compareAbsolutePathNoCase);

   // produce json listing
   BOOST_FOREACH( core::FilePath& filePath, *pFiles)
   {
      // files which may have been deleted after the listing or which
      // are not end-user visible
      if (filePath.exists() && module_context::fileListingFilter(core::FileInfo(filePath)))
      {
         source_control::VCSStatus status = vcsStatus.getStatus(filePath);
         core::json::Object fileObject = module_context::createFileSystemItem(filePath);
         json::Object vcsObj;
         error = modules::source_control::statusToJson(filePath, status, &vcsObj);
         if (error)
            LOG_ERROR(error);
         fileObject["git_status"] = vcsObj;
         pJsonFiles->push_back(fileObject) ;
      }
   }

   return Success();
}


} // namepsace files
} // namespace modules
} // namesapce session

