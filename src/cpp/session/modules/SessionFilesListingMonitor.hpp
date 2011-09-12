/*
 * SessionFilesListingMonitor.hpp
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

#ifndef SESSION_SESSION_FILES_LISTING_MONITOR_HPP
#define SESSION_SESSION_FILES_LISTING_MONITOR_HPP

#include <string>
#include <set>
#include <vector>

#include <boost/utility.hpp>

#include <core/collection/Tree.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/system/FileMonitor.hpp>

namespace core {
   class Error;
   class FilePath;
   class FileInfo;
   namespace system {
      class FileChangeEvent;
   }
}

namespace session {
namespace modules {

   namespace source_control {
      class StatusResult;
   }

namespace files {

class FilesListingMonitor : boost::noncopyable
{
public:
   // kickoff monitoring and call the continuation (if specifed) once it's initialized
   void startMonitoring(const std::string& path,
                        core::json::JsonRpcFunctionContinuation cont =
                                          core::json::JsonRpcFunctionContinuation());

   // what path are we currently monitoring?
   const std::string& currentMonitoredPath() const;

   // convenience method which is also called by listFiles for requests that
   // don't specify monitoring (e.g. file dialog listing)
   static void fileListingResponse(const core::FilePath& rootPath,
                                   core::json::JsonRpcFunctionContinuation cont);

private:
   // stateful callbacks for registration and unregistration
   void onRegistered(core::system::file_monitor::Handle handle,
                     const std::string& path,
                     const tree<core::FileInfo>& files,
                     core::json::JsonRpcFunctionContinuation cont);

   void onUnregistered(core::system::file_monitor::Handle handle);

   // error during registration
   static void onRegistrationError(const core::Error& error,
                                   const std::string& path,
                                   core::json::JsonRpcFunctionContinuation cont);

   // file change notification
   static void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events);

   // more helpers
   static void enqueFileChangeEvent(const source_control::StatusResult& statusResult,
                                    const core::system::FileChangeEvent& event);

   static void fileListingResponse(const core::FilePath& rootPath,
                                   const std::vector<core::FilePath>& children,
                                   core::json::JsonRpcFunctionContinuation cont);

   static bool fileEventFilter(const core::FileInfo& fileInfo);

private:
   std::string currentPath_;
   std::set<core::system::file_monitor::Handle> activeHandles_;
};



   
} // namespace files
} // namepace handlers
} // namesapce session

#endif // SESSION_SESSION_FILES_LISTING_MONITOR_HPP
