/*
 * PeriodicCommand.hpp
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


#ifndef CORE_PERIODIC_COMMAND_HPP
#define CORE_PERIODIC_COMMAND_HPP

#include <core/ScheduledCommand.hpp>

namespace rstudio {
namespace core {

class PeriodicCommand : public ScheduledCommand
{
public:
   PeriodicCommand(const boost::posix_time::time_duration& period,
                   const boost::function<bool()>& execute,
                   bool immediate = true)
      : ScheduledCommand(execute),
        period_(period)
   {
      if (immediate)
         nextExecutionTime_ = now();
      else
         nextExecutionTime_ = now() + period_;

   }

   virtual ~PeriodicCommand() {}

   // COPYING: boost::noncopyable

public:
   virtual void execute()
   {
      if (now() > nextExecutionTime_)
      {
         if (execute_())
         {
            nextExecutionTime_ = now() + period_;
         }
         else
         {
            finished_ = true;
         }
      }
   }

   virtual boost::posix_time::time_duration period()
   {
      return period_;
   }

private:
   const boost::posix_time::time_duration period_;
   boost::posix_time::ptime nextExecutionTime_;
};



} // namespace core
} // namespace rstudio


#endif // CORE_PERIODIC_COMMAND_HPP
