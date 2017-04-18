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

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   class FileInfo;
   namespace system {
      class FileChangeEvent;
   }
}
}

namespace rstudio {
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
   core::Error start(const core::FilePath& filePath, core::json::Array* pJsonFiles);

   void stop();

   // what path are we currently monitoring?
   const core::FilePath& currentMonitoredPath() const;

   // convenience method which is also called by listFiles for requests that
   // don't specify monitoring (e.g. file dialog listing)
   static core::Error listFiles(const core::FilePath& rootPath,
                                core::json::Array* pJsonFiles)
   {
      std::vector<core::FilePath> files;
      return listFiles(rootPath, &files, pJsonFiles);
   }

private:
   // stateful handlers for registration and unregistration
   void onRegistered(core::system::file_monitor::Handle handle,
                     const core::FilePath& filePath,
                     const std::vector<core::FileInfo>& prevFiles,
                     const tree<core::FileInfo>& files);

   void onUnregistered(core::system::file_monitor::Handle handle);

   // helpers
   static core::Error listFiles(const core::FilePath& rootPath,
                                std::vector<core::FilePath>* pFiles,
                                core::json::Array* pJsonFiles);

private:
   core::FilePath currentPath_;
   core::system::file_monitor::Handle currentHandle_;
};



   
} // namespace files
} // namepace handlers
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_FILES_LISTING_MONITOR_HPP
