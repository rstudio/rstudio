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

// stop the file monitoring service (automatically unregisters all
// active file monitoring handles)
void stop();

// opaque handle to a registration (used to unregister)
typedef void* Handle;

struct Callbacks
{
   // callback which occurs after a successful registration (includes an initial
   // listing of all of the files in the directory)
   boost::function<void(Handle, const tree<FileInfo>&)> onRegistered;

   // callback which occurs if a registration error occurs
   boost::function<void(const core::Error&)> onRegistrationError;

   // callback which occurs if an error occurs during monitoring. depending
   // upon the nature of the error this may (and likely will) result in
   // no more file chagne events being fired for this context. therefore,
   // after receiving this callback unregisterMonitor should then be called
   boost::function<void(const core::Error&)> onMonitoringError;

   // callback which occurs when files change
   boost::function<void(const std::vector<FileChangeEvent>&)> onFilesChanged;

   // callback which occurs when the monitor is fully unregistered. only
   // after this callback is received is it safe to tear down the
   // context (e.g. c++ object) setup for the other callbacks. note that this
   // callback can be received as a result of a call to file_monitor::stop
   // (as opposed to an explicit unregistration) -- in this case the callback
   // context needs to ensure that it doesn't subsequently call
   // unregisterMonitor (as that would result in a double-free of the Handle)
   boost::function<void()> onUnregistered;
};

// register a new file monitor. the result of this call will be an
// aynchronous call to either onRegistered or onRegistrationError. onRegistered
// will provide an opaque Handle which can used for a subsequent call
// to unregisterMonitor
void registerMonitor(const core::FilePath& filePath,
                     bool recursive,
                     const boost::function<bool(const FileInfo&)>& filter,
                     const Callbacks& callbacks);

// unregister a file monitor. this function can only be called once per file
// monitor registration (it is equivilant to calling delete on the Handle)
void unregisterMonitor(Handle handle);



// check for changes (will cause onRegistered, onRegistrationError,
// onMonitoringError, and onFilesChanged calls to occur on the same
// thread that calls checkForChanges)
void checkForChanges();



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

#endif // CORE_SYSTEM_FILE_MONITOR_HPP


