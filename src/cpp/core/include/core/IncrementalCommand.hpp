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

class IncrementalCommand : boost::noncopyable
{
public:
   IncrementalCommand(const boost::posix_time::time_duration& initialTime,
                      const boost::posix_time::time_duration& incrementalTime,
                      const boost::function<bool()>& execute)
      : initialTime_(initialTime),
        incrementalTime_(incrementalTime),
        execute_(execute),
        finished_(false)
   {

   }

   virtual ~IncrementalCommand() {}

   // COPYING: boost::noncopyable

public:
   bool execute()
   {
      if (!finished_)
      {
         finished_ = execute_();

      }
      return finished_;
   }

   bool finished() const { return finished_; }

private:
   boost::posix_time::time_duration initialTime_;
   boost::posix_time::time_duration incrementalTime_;
   boost::function<bool()> execute_;
   bool finished_;
};



} // namespace core


#endif // CORE_INCREMENTAL_COMMAND_HPP
