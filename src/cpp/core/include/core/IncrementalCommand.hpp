/*
 * IncrementalCommand.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#ifndef CORE_INCREMENTAL_COMMAND_HPP
#define CORE_INCREMENTAL_COMMAND_HPP


#include <core/ScheduledCommand.hpp>

namespace rstudio {
namespace core {

class IncrementalCommand : public ScheduledCommand
{
public:
   IncrementalCommand(
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute)
      : ScheduledCommand(execute), incrementalDuration_(incrementalDuration)
   {
   }

   IncrementalCommand(
         const boost::posix_time::time_duration& initialDuration,
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute)
      : ScheduledCommand(execute), incrementalDuration_(incrementalDuration)
   {
      executeUntil(now() + initialDuration);
   }

   virtual ~IncrementalCommand() {}

   // COPYING: boost::noncopyable

public:
   virtual void execute()
   {
      executeUntil(now() + incrementalDuration_);
   }

private:
   void executeUntil(const boost::posix_time::ptime& time)
   {
      while (!finished_ && (now() < time))
         finished_ = !execute_();
   }

private:
   const boost::posix_time::time_duration incrementalDuration_;
};



} // namespace core
} // namespace rstudio


#endif // CORE_INCREMENTAL_COMMAND_HPP
