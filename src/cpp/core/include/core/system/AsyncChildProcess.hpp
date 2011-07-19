/*
 * AsyncChildProcess.hpp
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


#ifndef CORE_SYSTEM_CHILD_PROCESS_HPP
#define CORE_SYSTEM_CHILD_PROCESS_HPP

#include <vector>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/signal.hpp>

namespace core {

class Error;

namespace system {

// TODO: could this object keep a shared_from_this refernece around
// until it exits?

class AsyncChildProcess : boost::noncopyable
{
public:
   typedef boost::signal<void(boost::shared_ptr<AsyncChildProcess>)> Signal;
   typedef boost::signal<void(boost::shared_ptr<AsyncChildProcess>,
                              const std::string&)> OutputSignal;

public:
   static boost::shared_ptr<AsyncChildProcess> create(
                                          const std::string& cmd,
                                          const std::vector<std::string>& args);

private:
   AsyncChildProcess(const std::string& cmd,
                     const std::vector<std::string>& args);

public:
   virtual ~AsyncChildProcess() ;

   // slots (connect before calling run)
   void connectOnStdout(const OutputSignal::slot_type& slot);
   void connectOnStderr(const OutputSignal::slot_type& slot);
   void connectOnExit(const Signal::slot_type& slot);

   // run the process (call poll to periodically to check for output/exit)
   Error run();

   // kill the process
   Error kill(int signal);

   // write (synchronously) to std input
   Error writeToStdin(const std::string& input);

   // check whethere the processs has exited and get it's status
   bool exited() const;
   int exitStatus() const;

   // call this periodically to poll for output/exit
   void poll();

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_CHILD_PROCESS_HPP
