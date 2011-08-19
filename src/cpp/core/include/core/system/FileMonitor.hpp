/*
 * FileMonitor.hpp
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

#ifndef CORE_SYSTEM_FILE_MONITOR_HPP
#define CORE_SYSTEM_FILE_MONITOR_HPP

#include <string>
#include <set>
#include <vector>

#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/FilePath.hpp>
#include <core/collection/Tree.hpp>

#include <core/system/FileChangeEvent.hpp>

namespace core {   
namespace system {
namespace file_monitor {

// initialize the file monitoring service (creates a background thread
// which performs the monitoring)
void initialize();

// stop the file monitoring service
void stop();

// opaque handle to a registration (used to unregister)
typedef void* Handle;

struct Callbacks
{
   // callback which occurs after a successful registration (includes an initial
   // listing of all of the files in the directory)
   boost::function<void(Handle, const tree<FileInfo>&)> onRegistered;

   // callback which occurs if a registration error occurs
   typedef boost::function<void(const core::Error&)> ReportError;
   ReportError onRegistrationError;

   // callback which occurs if an error occurs during monitoring which causes
   // file monitoring to terminate. after this error no more onFilesChanged
   // notifications will be received
   ReportError onMonitoringError;

   // callback which occurs when files change
   typedef boost::function<void(const std::vector<FileChangeEvent>&)> FilesChanged;
   FilesChanged onFilesChanged;
};

// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(Handle handle);

// check for changes (will cause onRegistered and/or onFilesChanged calls on
// the same thread that called checkForChanges)
void checkForChanges();


} // namespace file_monitor
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_MONITOR_HPP


