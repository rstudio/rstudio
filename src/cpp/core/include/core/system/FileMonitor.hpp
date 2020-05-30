/*
 * FileMonitor.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_SYSTEM_FILE_MONITOR_HPP
#define CORE_SYSTEM_FILE_MONITOR_HPP

#include <string>
#include <set>
#include <vector>

#include <boost/function.hpp>

#include <shared_core/FilePath.hpp>
#include <core/ScheduledCommand.hpp>
#include <core/collection/Tree.hpp>

#include <core/system/System.hpp>
#include <core/system/FileChangeEvent.hpp>

// cross-platform recursive file monitoring service. note that the
// implementation specifically avoids following soft-links. only directories
// physically contained in the root will be monitored (this is to prevent
// both extremely large trees and or self-referential (and thus infinitely
// recursive) trees.

namespace rstudio {
namespace core {   
namespace system {
namespace file_monitor {

// initialize the file monitoring service (creates a background thread
// which performs the monitoring)
void initialize();

// stop the file monitoring service (automatically unregisters all
// active file monitoring handles)
void stop();


// opaque handle to a registration (used to unregister). the id field
// is included so that handles have additional uniqueness beyond the
// value of the pData pointer (which could be duplicated accross monitors
// depending upon the order of allocations/deallocations)
struct Handle
{
   Handle()
      : pData(nullptr)
   {
   }

   explicit Handle(void* pData)
      : id(core::system::generateUuid()),
        pData(pData)
   {
   }

   bool empty() const { return id.empty(); }

   bool operator==(const Handle& other) const
   {
      return id == other.id &&
             pData == other.pData;
   }

   bool operator < (const Handle& other) const
   {
      return id < other.id;
   }

   std::string id;
   void* pData;
};

// file monitoring callbacks (all callbacks are optional)
struct Callbacks
{
   // callback which occurs after a successful registration (includes an initial
   // listing of all of the files in the directory)
   boost::function<void(Handle, const tree<FileInfo>&)> onRegistered;

   // callback which occurs if a registration error occurs
   boost::function<void(const core::Error&)> onRegistrationError;

   // callback which occurs if an error occurs during monitoring (the
   // monitor is automatically unregistered if a monitoring error occurs)
   boost::function<void(const core::Error&)> onMonitoringError;

   // callback which occurs when files change
   boost::function<void(const std::vector<FileChangeEvent>&)> onFilesChanged;

   // callback which occurs when the monitor is fully unregistered. note that
   // this callback can occur as a result of:
   //    - an explicit call to unregisterMonitor;
   //    - a monitoring error which caused an automatic unregistration; or
   //    - a call to the global file_monitor::stop function
   boost::function<void(Handle)> onUnregistered;
};

// register a new file monitor. the result of this call will be an
// aynchronous call to either onRegistered or onRegistrationError. onRegistered
// will provide an opaque Handle which can used for a subsequent call
// to unregisterMonitor. if you want to bind a c++ object to the lifetime
// of this file monitor simply create a shared_ptr and bind its members
// to the file monitor callbacks. note that if you also would like to
// guarantee that the deletion of your shared_ptr object is invoked on the same
// thread that called registerMonitor you should also bind a function to
// onUnregistered (otherwise the delete will occur on the file monitoring thread)
void registerMonitor(const core::FilePath& filePath,
                     bool recursive,
                     const boost::function<bool(const FileInfo&)>& filter,
                     const Callbacks& callbacks);

// unregister a file monitor. note that file monitors can be automatically
// unregistered in the case of errors or a call to global file_monitor::stop,
// as a result multiple calls to unregisterMonitor are permitted (and no-op
// if the handle has already been unregistered)
void unregisterMonitor(Handle handle);


// check for changes (will cause onRegistered, onRegistrationError,
// onMonitoringError, onFilesChanged, and onUnregistered calls to occur
// on the same thread that calls checkForChanges)
void checkForChanges();


// create a ScheduledCommand that periodically checks for changes
boost::shared_ptr<ScheduledCommand> checkForChangesCommand(
                       const boost::posix_time::time_duration& interval);

// convenience functions for creating filters that are useful in
// file monitoring scenarios

// filter out any directory (and its children) with the specified name
// (no matter where it is located within the tree). useful for directories
// like .git, .svn, .RProj.user, etc.
boost::function<bool(const FileInfo&)> excludeDirectoryFilter(
                                                     const std::string& name);

// aggregate version of above
boost::function<bool(const FileInfo&)> excludeDirectoriesFilter(
                                    const std::vector<std::string>& names);

// exclude hidden files
boost::function<bool(const FileInfo&)> excludeHiddenFilter();


} // namespace file_monitor
} // namespace system
} // namespace core 
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_MONITOR_HPP


