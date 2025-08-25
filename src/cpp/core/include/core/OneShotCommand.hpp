/*
 * OneShotCommand.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
#ifndef CORE_ONESHOT_COMMAND_HPP
#define CORE_ONESHOT_COMMAND_HPP

#include <core/ScheduledCommand.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/utility.hpp>

namespace rstudio {
namespace core {

class OneShotCommand : public ScheduledCommand
{
public:

   OneShotCommand(const boost::function<bool()>& execute,
                  const boost::posix_time::time_duration& waitFor);

   ~OneShotCommand() override = default;

public:
   void execute() override;
   virtual boost::posix_time::time_duration period();

protected:
   const boost::posix_time::time_duration waitFor_;
   boost::posix_time::ptime executionTime_;
};

} // namespace core
} // namespace rstudio

#endif // CORE_ONESHOT_COMMAND_HPP
