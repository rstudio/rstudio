/*
 * SessionFilesListingMonitor.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SESSION_SESSION_FILES_LISTING_MONITOR_HPP
#define SESSION_SESSION_FILES_LISTING_MONITOR_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>

#include <core/collection/Tree.hpp>

#include <core/json/Json.hpp>
#include <core/system/FileMonitor.hpp>

namespace rscore {
   class Error;
   class FilePath;
   class FileInfo;
   namespace system {
      class FileChangeEvent;
   }
}

namespace session {
namespace modules {

   namespace git {
      class StatusResult;
   }

namespace files {

class FilesListingMonitor : boost::noncopyable
{
public:
   // kickoff monitoring
   rscore::Error start(const rscore::FilePath& filePath, rscore::json::Array* pJsonFiles);

   void stop();

   // what path are we currently monitoring?
   const rscore::FilePath& currentMonitoredPath() const;

   // convenience method which is also called by listFiles for requests that
   // don't specify monitoring (e.g. file dialog listing)
   static rscore::Error listFiles(const rscore::FilePath& rootPath,
                                rscore::json::Array* pJsonFiles)
   {
      std::vector<rscore::FilePath> files;
      return listFiles(rootPath, &files, pJsonFiles);
   }

private:
   // stateful handlers for registration and unregistration
   void onRegistered(rscore::system::file_monitor::Handle handle,
                     const rscore::FilePath& filePath,
                     const std::vector<rscore::FileInfo>& prevFiles,
                     const tree<rscore::FileInfo>& files);

   void onUnregistered(rscore::system::file_monitor::Handle handle);

   // helpers
   static rscore::Error listFiles(const rscore::FilePath& rootPath,
                                std::vector<rscore::FilePath>* pFiles,
                                rscore::json::Array* pJsonFiles);

private:
   rscore::FilePath currentPath_;
   rscore::system::file_monitor::Handle currentHandle_;
};



   
} // namespace files
} // namepace handlers
} // namesapce session

#endif // SESSION_SESSION_FILES_LISTING_MONITOR_HPP
