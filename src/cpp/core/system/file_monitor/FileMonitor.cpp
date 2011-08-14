/*
 * FileMonitor.cpp
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

// TODO: implement FileListing applyChange

// TODO: reduce thread priority for main thread

#include <core/system/file_monitor/FileMonitor.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/Thread.hpp>

#include "FileMonitorImpl.hpp"

namespace core {
namespace system {
namespace file_monitor {

namespace {

void fileMonitorThreadMain()
{
   try
   {
      file_monitor::impl::run();
   }
   CATCH_UNEXPECTED_EXCEPTION
}

} // anonymous namespace


Error initialize()
{
   core::thread::safeLaunchThread(fileMonitorThreadMain);
   return Success();
}

namespace impl {

core::thread::ThreadsafeQueue<RegistrationCommand>& registrationCommandQueue()
{
   static core::thread::ThreadsafeQueue<RegistrationCommand> instance;
   return instance;
}

core::thread::ThreadsafeQueue<boost::function<void()> >& callbackQueue()
{
   static core::thread::ThreadsafeQueue<boost::function<void()> > instance;
   return instance;
}

} // namespace impl


void FileListing::applyChange(const FileChange& fileChange)
{
   // seek to the collection containing this change
}

void registerMonitor(const FilePath& filePath, const Callbacks& callbacks)
{
   impl::registrationCommandQueue().enque(
                           impl::RegistrationCommand(filePath, callbacks));
}

void unregisterMonitor(const RegistrationHandle& handle)
{
   impl::registrationCommandQueue().enque(impl::RegistrationCommand(handle));
}

void checkForChanges()
{
   boost::function<void()> callback;
   while (impl::callbackQueue().deque(&callback))
      callback();
}




} // namespace file_monitor
} // namespace system
} // namespace core 

   



