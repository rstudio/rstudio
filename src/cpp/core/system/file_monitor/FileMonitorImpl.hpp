/*
 * FileMonitorImpl.hpp
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

#ifndef CORE_SYSTEM_FILE_MONITOR_IMPL_HPP
#define CORE_SYSTEM_FILE_MONITOR_IMPL_HPP


#include <core/FilePath.hpp>
#include <core/Thread.hpp>

#include <core/system/file_monitor/FileMonitor.hpp>

namespace core {   
namespace system {
namespace file_monitor {
namespace impl {

class RegistrationCommand
{
public:
   enum Type { None, Register, Unregister };

public:
   RegistrationCommand()
      : type_(None)
   {
   }

   RegistrationCommand(const core::FilePath& filePath, const Callbacks& callbacks)
      : type_(Register), filePath_(filePath), callbacks_(callbacks)
   {
   }

   explicit RegistrationCommand(const RegistrationHandle& registrationHandle)
      : type_(Unregister), registrationHandle_(registrationHandle)
   {
   }

   Type type() const { return type_; }

   const core::FilePath& filePath() const { return filePath_; }
   const Callbacks& callbacks() const { return callbacks_; }

   const RegistrationHandle& registrationHandle() const
   {
      return registrationHandle_;
   }

private:
   // command type
   Type type_;

   // register command data
   core::FilePath filePath_;
   Callbacks callbacks_;

   // unregister command data
   RegistrationHandle registrationHandle_;
};

// registration command queue (input to file monitor)
core::thread::ThreadsafeQueue<RegistrationCommand>& registrationCommandQueue();

// callback queue (output from file monitor)
core::thread::ThreadsafeQueue<boost::function<void()> >& callbackQueue();

// platform specific run method
void run();

} // namespace impl
} // namespace file_monitor
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_MONITOR_IMPL_HPP


