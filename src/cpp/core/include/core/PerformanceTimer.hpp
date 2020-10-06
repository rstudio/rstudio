/*
 * PerformanceTimer.hpp
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

#ifndef CORE_PERFORMANCE_TIMER_HPP
#define CORE_PERFORMANCE_TIMER_HPP

#include <iosfwd>
#include <vector>
#include <string>

#include <boost/utility.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace core {
   
class PerformanceTimer : boost::noncopyable
{
public:
   PerformanceTimer();
   explicit PerformanceTimer(const std::string& step);
   virtual ~PerformanceTimer();
   // COPYING: boost::noncopyable
   
public:
   void start(const std::string& step);
   void advance(const std::string& step);
   void stop();
   bool running() const;
   
private:
   boost::posix_time::ptime now() const;
   void recordPendingStep();
   
private:
   typedef std::pair<std::string,boost::posix_time::time_duration> Step;
   typedef std::vector<Step> Steps;
   
   boost::posix_time::ptime startTime_;
   Steps steps_;
   
   friend std::ostream& operator << (std::ostream& stream, 
                                     const PerformanceTimer& t);
};

std::ostream& operator << (std::ostream& os, const PerformanceTimer& t);

} // namespace core 
} // namespace rstudio

#define TIME_FUNCTION core::PerformanceTimer t(BOOST_CURRENT_FUNCTION);

#endif // CORE_PERFORMANCE_TIMER_HPP

