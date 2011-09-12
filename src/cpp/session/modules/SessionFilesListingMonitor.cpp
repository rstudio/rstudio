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

#include <core/system/FileMonitor.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionSourceControl.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace files {

void FilesListingMonitor::start(const FilePath& filePath,
                                core::json::JsonRpcFunctionContinuation cont)
{
   // always stop existing
   stop();

   // kickoff new monitor
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(&FilesListingMonitor::onRegistered,
                                    this, _1, filePath, _2, cont);
   cb.onRegistrationError = boost::bind(onRegistrationError, _1, filePath, cont);
   cb.onFilesChanged = boost::bind(module_context::enqueFileChangedEvents, filePath, _1);
   cb.onMonitoringError = boost::bind(core::log::logError, _1, ERROR_LOCATION);
   cb.onUnregistered = boost::bind(&FilesListingMonitor::onUnregistered, this, _1);
   core::system::file_monitor::registerMonitor(filePath,
                                               false,
                                               module_context::fileListingFilter,
                                               cb);
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

// convenience method which is also called by listFiles for requests that
// don't specify monitoring (e.g. file dialog listing)
void FilesListingMonitor::fileListingResponse(
                                          const core::FilePath& rootPath,
                                          core::json::JsonRpcFunctionContinuation cont)
{
   std::vector<core::FilePath> files ;
   core::Error error = rootPath.children(&files) ;
   if (error)
   {
      cont(error, NULL);
      return;
   }

   fileListingResponse(rootPath, files, cont);
}

void FilesListingMonitor::onRegistered(core::system::file_monitor::Handle handle,
                                       const FilePath& filePath,
                                       const tree<core::FileInfo>& files,
                                       core::json::JsonRpcFunctionContinuation cont)
{
   // set path and current handle
   currentPath_ = filePath;
   currentHandle_ = handle;

   // if there is a continuation then satisfy it
   if (cont)
   {
      // convert file tree into flat vector of FilePath
      std::vector<core::FilePath> children;
      std::transform(files.begin(files.begin()),
                     files.end(files.begin()),
                     std::back_inserter(children),
                     core::toFilePath);

      // satisfy the continuation
      fileListingResponse(filePath, children, cont);
   }
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


void FilesListingMonitor::onRegistrationError(const Error& error,
                                              const FilePath& filePath,
                                              json::JsonRpcFunctionContinuation cont)
{
   // always log the error
   LOG_ERROR(error);

   // if there is a continuation then we still need to satisfy the file listing
   // request using a standard scan of the fileystem
   if (cont)
   {
      // retreive list of files
      fileListingResponse(filePath, cont);
   }
}


void FilesListingMonitor::fileListingResponse(
                                       const core::FilePath& rootPath,
                                       const std::vector<core::FilePath>& children,
                                       core::json::JsonRpcFunctionContinuation cont)
{
   source_control::StatusResult vcsStatus;
   core::Error error = source_control::status(rootPath, &vcsStatus);
   if (error)
   {
      cont(error, NULL);
      return;
   }

   // sort the files by name (first make a copy)
   std::vector<core::FilePath> files;
   std::copy(children.begin(), children.end(), std::back_inserter(files));
   std::sort(files.begin(), files.end(), core::compareAbsolutePathNoCase);

   // produce json listing
   core::json::Array jsonFiles ;
   BOOST_FOREACH( core::FilePath& filePath, files )
   {
      // files which may have been deleted after the listing or which
      // are not end-user visible
      if (filePath.exists() && module_context::fileListingFilter(core::FileInfo(filePath)))
      {
         source_control::VCSStatus status = vcsStatus.getStatus(filePath);
         core::json::Object fileObject = module_context::createFileSystemItem(filePath);
         fileObject["vcs_status"] = status.status();
         jsonFiles.push_back(fileObject) ;
      }
   }

   // return listing
   core::json::JsonRpcResponse response;
   response.setResult(jsonFiles) ;
   cont(core::Success(), &response);
}


} // namepsace files
} // namespace modules
} // namesapce session

