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

namespace rstudiocore {
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
   rstudiocore::Error start(const rstudiocore::FilePath& filePath, rstudiocore::json::Array* pJsonFiles);

   void stop();

   // what path are we currently monitoring?
   const rstudiocore::FilePath& currentMonitoredPath() const;

   // convenience method which is also called by listFiles for requests that
   // don't specify monitoring (e.g. file dialog listing)
   static rstudiocore::Error listFiles(const rstudiocore::FilePath& rootPath,
                                rstudiocore::json::Array* pJsonFiles)
   {
      std::vector<rstudiocore::FilePath> files;
      return listFiles(rootPath, &files, pJsonFiles);
   }

private:
   // stateful handlers for registration and unregistration
   void onRegistered(rstudiocore::system::file_monitor::Handle handle,
                     const rstudiocore::FilePath& filePath,
                     const std::vector<rstudiocore::FileInfo>& prevFiles,
                     const tree<rstudiocore::FileInfo>& files);

   void onUnregistered(rstudiocore::system::file_monitor::Handle handle);

   // helpers
   static rstudiocore::Error listFiles(const rstudiocore::FilePath& rootPath,
                                std::vector<rstudiocore::FilePath>* pFiles,
                                rstudiocore::json::Array* pJsonFiles);

private:
   rstudiocore::FilePath currentPath_;
   rstudiocore::system::file_monitor::Handle currentHandle_;
};



   
} // namespace files
} // namepace handlers
} // namesapce session

#endif // SESSION_SESSION_FILES_LISTING_MONITOR_HPP
