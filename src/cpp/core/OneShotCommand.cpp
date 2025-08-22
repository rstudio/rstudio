/*
 * OneShotCommand.cpp
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
#include <core/OneShotCommand.hpp>


namespace rstudio {
namespace core {


OneShotCommand::OneShotCommand(const boost::function<bool()>& execute,
                  const boost::posix_time::time_duration& waitFor)
   : ScheduledCommand(execute),
   waitFor_(waitFor)
{
   if (waitFor == boost::posix_time::not_a_date_time)
      executionTime_ = now();
   else
      executionTime_ = now() + waitFor_;
}

void OneShotCommand::execute()
{
   if (now() > executionTime_)
   {
      execute_();
      finished_ = true;
   }
}

boost::posix_time::time_duration OneShotCommand::period()
{
   return waitFor_;
}

} // namespace core
} // namespace rstudio
