/*
 * ServerScheduler.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_SCHEDULER_HPP
#define SERVER_SCHEDULER_HPP

#include <string>

#include <core/ScheduledCommand.hpp>

namespace rstudio {
namespace server {
namespace scheduler {

// add a scheduled command to the server
//
// note that this function does not synchronize access to the list of
// scheduled commands so it should ONLY be called during server init
void addCommand(boost::shared_ptr<core::ScheduledCommand> pCmd);

} // namespace scheduler
} // namespace server
} // namespace rstudio

#endif // SERVER_SCHEDULER_HPP

