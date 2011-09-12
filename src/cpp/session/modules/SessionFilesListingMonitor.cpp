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

void FilesListingMonitor::startMonitoring(const std::string& path,
                                          core::json::JsonRpcFunctionContinuation cont)
{
   // reset monitored path and unregister any existing handle
   currentPath_.clear();
   if (!currentHandle_.empty())
   {
      core::system::file_monitor::unregisterMonitor(currentHandle_);
      currentHandle_ = core::system::file_monitor::Handle();
   }

   // kickoff monitor
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(&FilesListingMonitor::onRegistered,
                                    this, _1, path, _2, cont);
   cb.onRegistrationError = boost::bind(onRegistrationError, _1, path, cont);
   cb.onFilesChanged = onFilesChanged;
   cb.onMonitoringError = boost::bind(core::log::logError, _1, ERROR_LOCATION);
   cb.onUnregistered = boost::bind(&FilesListingMonitor::onUnregistered, this, _1);
   core::system::file_monitor::registerMonitor(core::FilePath(path),
                                               false,
                                               fileEventFilter,
                                               cb);
}

const std::string& FilesListingMonitor::currentMonitoredPath() const
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
      cont(error, boost::optional<core::json::JsonRpcResponse>());
      return;
   }

   fileListingResponse(rootPath, files, cont);
}

void FilesListingMonitor::onRegistered(core::system::file_monitor::Handle handle,
                                       const std::string& path,
                                       const tree<core::FileInfo>& files,
                                       core::json::JsonRpcFunctionContinuation cont)
{
   // set path and current handle
   currentPath_ = path;
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
      fileListingResponse(core::FilePath(path), children, cont);
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
      currentPath_.clear();
      currentHandle_ = core::system::file_monitor::Handle();
   }
}


void FilesListingMonitor::onRegistrationError(
                                            const core::Error& error,
                                            const std::string& path,
                                            core::json::JsonRpcFunctionContinuation cont)
{
   // always log the error
   LOG_ERROR(error);

   // if there is a continuation then we still need to satisfy the file listing
   // request using a standard scan of the fileystem
   if (cont)
   {
      // retreive list of files
      fileListingResponse(core::FilePath(path), cont);
   }
}

void FilesListingMonitor::onFilesChanged(
                              const std::vector<core::system::FileChangeEvent>& events)
{
   if (events.empty())
      return;

   source_control::StatusResult statusResult;
   core::Error error = source_control::status(
         core::FilePath(events.front().fileInfo().absolutePath()).parent(),
         &statusResult);
   if (error)
      LOG_ERROR(error);

   // fire client events as necessary
   std::for_each(events.begin(), events.end(), boost::bind(enqueFileChangeEvent,
                                                           statusResult,
                                                           _1));
}

void FilesListingMonitor::enqueFileChangeEvent(
                                 const source_control::StatusResult& statusResult,
                                 const core::system::FileChangeEvent& event)
{
   using namespace source_control;
   core::FilePath filePath(event.fileInfo().absolutePath());
   std::string vcsStatus = statusResult.getStatus(filePath).status();
   module_context::enqueFileChangedEvent(event, vcsStatus);
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
      cont(error, boost::optional<core::json::JsonRpcResponse>());
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
      if (filePath.exists() && fileEventFilter(core::FileInfo(filePath)))
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
   cont(core::Success(), response);
}


bool FilesListingMonitor::fileEventFilter(const core::FileInfo& fileInfo)
{
   // check extension for special file types which are always visible
   core::FilePath filePath(fileInfo.absolutePath());
   std::string ext = filePath.extensionLowerCase();
   if (ext == ".rprofile" ||
       ext == ".rdata"    ||
       ext == ".rhistory" ||
       ext == ".renviron" )
   {
      return true;
   }
   else
   {
      return !filePath.isHidden();
   }
}






} // namepsace files
} // namespace modules
} // namesapce session

