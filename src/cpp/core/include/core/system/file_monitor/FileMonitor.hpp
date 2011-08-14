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
#include <vector>

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/FileInfo.hpp>

#include <core/system/FileChangeEvent.hpp>

namespace core {   
namespace system {
namespace file_monitor {

// initialize the file monitoring service (creates a background thread
// which performs the monitoring)
Error initialize();

// opaque handle to a registration
class RegistrationHandle
{
public:
   RegistrationHandle();
   virtual ~RegistrationHandle();

private:
   struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};

struct Callbacks
{
   // callback which occurs after a successful registration (includes an initial
   // listing of all of the files in the directory)
   boost::function<void(const RegistrationHandle&,
                        const std::vector<core::FileInfo>&)> onRegistered;

   // callback which occurs if a registration error occurs
   boost::function<void(core::Error&)> onRegistrationError;

   // callback which occurs when files change
   boost::function<void(const std::vector<core::system::FileChangeEvent>&)>
                                                                  onFilesChanged;
};

// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(const RegistrationHandle& handle);

// check for changes (will cause onRegistered and/or onFilesChanged calls on
// the same thread that called checkForChanges)
void checkForChanges();


} // namespace file_monitor
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_MONITOR_HPP


