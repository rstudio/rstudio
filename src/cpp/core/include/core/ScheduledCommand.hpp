/*
 * ScheduledCommand.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

/*
* @brief Command that can be queued to run fom the async server
* @details See child classes for scheduling options
*/
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
