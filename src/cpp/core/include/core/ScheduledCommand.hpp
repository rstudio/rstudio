/*
 * ScheduledCommand.hpp
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


#ifndef CORE_SCHEDULED_COMMAND_HPP
#define CORE_SCHEDULED_COMMAND_HPP

#include <boost/utility.hpp>
#include <boost/function.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace core {

// NOTE: execute function should return true if it has more work to do
// or false to indicate all work is completed

class ScheduledCommand : boost::noncopyable
{
public:
   explicit ScheduledCommand(const boost::function<bool()>& execute)
      : execute_(execute), finished_(false)
   {
   }

   virtual ~ScheduledCommand() {}

   // COPYING: boost::noncopyable

public:
   virtual void execute() = 0;

   bool finished() const { return finished_; }

protected:
   boost::function<bool()> execute_;
   bool finished_;

protected:
   static boost::posix_time::ptime now()
   {
      return boost::posix_time::microsec_clock::universal_time();
   }
};



} // namespace core
} // namespace rstudio


#endif // CORE_SCHEDULED_COMMAND_HPP
