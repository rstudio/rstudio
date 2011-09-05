/*
 * LinuxFileMonitor.cpp
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

#include <core/system/FileMonitor.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileInfo.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

// TODO: consider calling terminateAll on module context supervisor as well
//       retaining interrupable child supervisor so we can do the same

// TODO: investigate parallel package (multicore) interactions with file monitor

// TODO: checkout inotifywait (http://linux.die.net/man/1/inotifywait)

// TODO: should we be using lstat64?

// TODO: investigate use of IN_DONT_FOLLOW to prevent following links
//       (also add documentation to the API and comment in generic
//        FileScanner.cpp)

namespace core {
namespace system {
namespace file_monitor {

namespace {



} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const Callbacks& callbacks)
{
   return Handle();
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{

}

void run(const boost::function<void()>& checkForInput)
{

}

void stop()
{

}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



