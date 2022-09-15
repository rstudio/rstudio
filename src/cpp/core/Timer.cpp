/*
 * Timer.cpp
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

#include <core/Timer.hpp>

namespace rstudio {
namespace core {

Timer::Timer(boost::asio::io_service& ioService) :
   timer_(ioService)
{
}

void Timer::setExpiration(const boost::posix_time::time_duration& timeDuration)
{
   timer_.expires_from_now(timeDuration);
}

void Timer::cancel()
{
   timer_.cancel();
}

void Timer::wait(const std::function<void(const boost::system::error_code& ec)>& callback)
{
   timer_.async_wait(callback);
}

} // namespace core
} // namespace rstudio
