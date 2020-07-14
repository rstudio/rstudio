/*
 * PerformanceTimer.cpp
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

#include <core/PerformanceTimer.hpp>

#include <iostream>
#include <iomanip>

using namespace boost::posix_time;

namespace rstudio {
namespace core {
   
PerformanceTimer::PerformanceTimer()
   : startTime_(ptime(not_a_date_time))
{
}
   
PerformanceTimer::PerformanceTimer(const std::string& step)
   : startTime_(ptime(not_a_date_time))
{
   start(step);
}
   
PerformanceTimer::~PerformanceTimer() 
{
   try
   {
      if (running())
      {
         stop();
         std::cerr << *this;
      }
   }
   catch(...)
   {
   }
}

void PerformanceTimer::start(const std::string& step)   
{
   BOOST_ASSERT(!running());
   
   // reset state
   startTime_ = now();
   steps_.clear();
   
   // advance to step
   advance(step);
}
         
void PerformanceTimer::advance(const std::string& step)
{
   BOOST_ASSERT(running());
   
   // if there is an existing step pending then record its duration
   recordPendingStep();
   
   // record the start time and add a step
   startTime_ = now();
   steps_.push_back(std::make_pair(step, time_duration()));
}
   
void PerformanceTimer::stop()   
{
   BOOST_ASSERT(running());
   
   // if there is an existing step pending then record its duration
   recordPendingStep();
   
   // reset start time
   startTime_ = ptime(not_a_date_time);
}
   
bool PerformanceTimer::running() const
{
   return !startTime_.is_not_a_date_time();
}
   
void PerformanceTimer::recordPendingStep()
{
   if (!steps_.empty())
      steps_.back().second = now() - startTime_;
}
 
boost::posix_time::ptime PerformanceTimer::now() const
{
   return microsec_clock::universal_time();
}
   
std::ostream& operator << (std::ostream& os, const PerformanceTimer& t)
{
   BOOST_ASSERT(!t.running());
   
   // fixed width output
   std::ios::fmtflags oldFlags = os.setf(std::ios::fixed);
   
   os << "PERFORMANCE";

   // start on a new line if we have more than one step
   if (t.steps_.size() > 1)
      os << std::endl;

   for (PerformanceTimer::Steps::const_iterator 
         it = t.steps_.begin(); it != t.steps_.end(); ++it)
   {
      double ms = it->second.total_microseconds() * 0.001;
      os << std::setprecision(ms < 10 ? 1 : 0);
      os << " " << ms << " ms (" << it->first << ")" << std::endl;
   }
   
   // restore old output flags
   os.setf(oldFlags);
   
   return os;
}
         

} // namespace core 
} // namespace rstudio



