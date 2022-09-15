/*
 * Timer.hpp
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

#ifndef CORE_TIMER_HPP
#define CORE_TIMER_HPP

#include <functional>
#include <boost/asio/io_service.hpp>
#include <boost/asio/deadline_timer.hpp>

namespace rstudio {
namespace core {

using TimerCallback = std::function<void(const boost::system::error_code& ec)>;

class ITimer
{
public:
   virtual void cancel() = 0;
   virtual void setExpiration(const boost::posix_time::time_duration& timeDuration) = 0;
   virtual void wait(const TimerCallback& callback) = 0;

   virtual ~ITimer() {}
};

class Timer : public ITimer
{
public:
   Timer(boost::asio::io_service& ioService);
   virtual ~Timer() {}

   virtual void cancel() override;
   virtual void setExpiration(const boost::posix_time::time_duration& timeDuration) override;
   virtual void wait(const std::function<void(const boost::system::error_code& ec)>& callback) override;

private:
   boost::asio::deadline_timer timer_;
};

using TimerPtr = boost::shared_ptr<ITimer>;

} // namespace core
} // namespace rstudio

#endif // CORE_TIMER_HPP

