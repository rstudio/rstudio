/*
 * MacFileMonitor.cpp
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

#include <core/system/file_monitor/FileMonitor.hpp>

#include <CoreServices/CoreServices.h>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/system/System.hpp>
#include <core/Thread.hpp>

#include "FileMonitorImpl.hpp"

namespace core {
namespace system {
namespace file_monitor {

namespace {


} // anonymous namespace

struct RegistrationHandle::Impl
{
};

RegistrationHandle::RegistrationHandle()
   : pImpl_(new Impl())
{
}

RegistrationHandle::~RegistrationHandle()
{
}

namespace impl {

void run()
{
   // ensure we have a run loop for this thread
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // process the run loop for 1 second
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1, false);

      // if we were stopped then break
      if (reason == kCFRunLoopRunStopped)
         break;

      // if there is nothing the run loop then sleep for 250ms (so we don't spin)
      if (reason == kCFRunLoopRunFinished)
         boost::this_thread::sleep(boost::posix_time::milliseconds(250));

      // check our command queue for new registrations or de-registrations
      RegistrationCommand command;
      while (registrationCommandQueue().deque(&command))
      {

      }
   }
}

} // namespace impl

} // namespace file_monitor
} // namespace system
} // namespace core 

   



