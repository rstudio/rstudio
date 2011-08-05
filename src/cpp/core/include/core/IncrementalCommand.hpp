/*
 * IncrementalCommand.hpp
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


#ifndef CORE_INCREMENTAL_COMMAND_HPP
#define CORE_INCREMENTAL_COMMAND_HPP

#include <boost/utility.hpp>
#include <boost/function.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>

namespace core {

// NOTE: execute function should return true if it has more work to do
// or false to indicate all work is completed

class IncrementalCommand : boost::noncopyable
{
public:
   IncrementalCommand(
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute)
      : incrementalDuration_(incrementalDuration),
        execute_(execute),
        finished_(false)
   {
   }

   IncrementalCommand(
         const boost::posix_time::time_duration& initialDuration,
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute)
      : incrementalDuration_(incrementalDuration),
        execute_(execute),
        finished_(false)
   {
      executeUntil(now() + initialDuration);
   }

   virtual ~IncrementalCommand() {}

   // COPYING: boost::noncopyable

public:
   void execute()
   {
      executeUntil(now() + incrementalDuration_);
   }

   bool finished() const { return finished_; }

private:
   void executeUntil(const boost::posix_time::ptime& time)
   {
      while (!finished_ && (now() < time))
         finished_ = !execute_();
   }

   static boost::posix_time::ptime now()
   {
      return boost::posix_time::microsec_clock::universal_time();
   }

private:
   const boost::posix_time::time_duration incrementalDuration_;
   const boost::function<bool()> execute_;
   bool finished_;
};



} // namespace core


#endif // CORE_INCREMENTAL_COMMAND_HPP
