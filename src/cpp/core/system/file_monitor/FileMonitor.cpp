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

// TODO: reduce thread priority for main thread

// TODO: can we just allow the file monitor to die with the process or is
//       there some residual? (almost certainly not but should investigate)

#include <core/system/file_monitor/FileMonitor.hpp>

#include <boost/bind/protect.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/Thread.hpp>

namespace core {
namespace system {
namespace file_monitor {

// these are implemented per-platform
namespace detail {

// run the monitor, calling back checkForInput periodically to see if there are
// new registrations or unregistrations
void run(const boost::function<void()>& checkForInput);

// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(Handle handle);

}


namespace {

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

   explicit RegistrationCommand(Handle handle)
      : type_(Unregister), handle_(handle)
   {
   }

   Type type() const { return type_; }

   const core::FilePath& filePath() const { return filePath_; }
   const Callbacks& callbacks() const { return callbacks_; }

   Handle handle() const
   {
      return handle_;
   }

private:
   // command type
   Type type_;

   // register command data
   core::FilePath filePath_;
   Callbacks callbacks_;

   // unregister command data
   Handle handle_;
};

typedef core::thread::ThreadsafeQueue<RegistrationCommand>
                                                      RegistrationCommandQueue;
RegistrationCommandQueue& registrationCommandQueue()
{
   static core::thread::ThreadsafeQueue<RegistrationCommand> instance;
   return instance;
}

typedef core::thread::ThreadsafeQueue<boost::function<void()> > CallbackQueue;
CallbackQueue& callbackQueue()
{
   static core::thread::ThreadsafeQueue<boost::function<void()> > instance;
   return instance;
}

void checkForInput()
{
   RegistrationCommand command;
   while (registrationCommandQueue().deque(&command))
   {
      switch(command.type())
      {
      case RegistrationCommand::Register:
         detail::registerMonitor(command.filePath(), command.callbacks());
         break;
      case RegistrationCommand::Unregister:
         detail::unregisterMonitor(command.handle());
         break;
      case RegistrationCommand::None:
         break;
      }
   }
}

void fileMonitorThreadMain()
{
   try
   {
      file_monitor::detail::run(boost::bind(checkForInput));
   }
   CATCH_UNEXPECTED_EXCEPTION
}

void enqueOnRegistered(const Callbacks& callbacks,
                       Handle handle,
                       const FileEntry& fileEntry)
{
   callbackQueue().enque(boost::bind(callbacks.onRegistered,
                                     handle,
                                     fileEntry));
}

void enqueOnRegistrationError(const Callbacks& callbacks, const Error& error)
{
   callbackQueue().enque(boost::bind(callbacks.onRegistrationError, error));
}

void enqueOnFilesChanged(const Callbacks& callbacks,
                         const std::vector<FileChange>& fileChanges)
{
   callbackQueue().enque(boost::bind(callbacks.onFilesChanged, fileChanges));
}

} // anonymous namespace


void initialize()
{
   core::thread::safeLaunchThread(fileMonitorThreadMain);
}

void registerMonitor(const FilePath& filePath, const Callbacks& callbacks)
{
   // bind a new version of the callbacks that puts them on the callback queue
   Callbacks qCallbacks;
   qCallbacks.onRegistered = boost::bind(enqueOnRegistered, callbacks, _1, _2);
   qCallbacks.onRegistrationError = boost::bind(enqueOnRegistrationError,
                                                callbacks,
                                                _1);
   qCallbacks.onFilesChanged = boost::bind(enqueOnFilesChanged, callbacks, _1);

   // enque the registration
   registrationCommandQueue().enque(RegistrationCommand(filePath, qCallbacks));
}

void unregisterMonitor(Handle handle)
{
   registrationCommandQueue().enque(RegistrationCommand(handle));
}

void checkForChanges()
{
   boost::function<void()> callback;
   while (callbackQueue().deque(&callback))
      callback();
}

} // namespace file_monitor
} // namespace system
} // namespace core 

   



