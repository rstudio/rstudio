/*
 * DateTime.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/DateTime.hpp>
#include <shared_core/DateTime.hpp>

#include "boost/date_time/c_local_time_adjustor.hpp"
#include <boost/date_time/local_time/local_time.hpp>

namespace rstudio {
namespace core {
namespace date_time {   

double secondsSinceEpoch()
{
   return millisecondsSinceEpoch() / 1000;
}
   
double secondsSinceEpoch(const boost::posix_time::ptime& time)
{
   return millisecondsSinceEpoch(time) / 1000;
}
   
double secondsSinceEpoch(std::time_t time)
{
   return millisecondsSinceEpoch(time) / 1000;
}
   
double millisecondsSinceEpoch()
{
   return millisecondsSinceEpoch(
                     boost::posix_time::microsec_clock::universal_time());
}
   
double millisecondsSinceEpoch(const boost::posix_time::ptime& time)
{
   using namespace boost::gregorian;
   using namespace boost::posix_time;
   
   ptime time_t_epoch(date(1970,1,1));
   time_duration diff = time - time_t_epoch;
   return static_cast<double>(diff.total_milliseconds());
}
   
double millisecondsSinceEpoch(std::time_t time)
{
   return std::difftime(time, 0) * 1000;
}
   
boost::posix_time::ptime timeFromSecondsSinceEpoch(double sec)
{
   using namespace boost::gregorian;
   using namespace boost::posix_time;

   ptime time_t_epoch(date(1970,1,1));
   return time_t_epoch + seconds(static_cast<long>(sec));
}

boost::posix_time::ptime timeFromMillisecondsSinceEpoch(int64_t ms)
{
   using namespace boost::gregorian;
   using namespace boost::posix_time;

   ptime time_t_epoch(date(1970,1,1));
   return time_t_epoch + milliseconds(ms);
}

std::string millisecondsSinceEpochAsString(double ms)
{
   boost::posix_time::ptime time =
                   date_time::timeFromMillisecondsSinceEpoch(static_cast<int64_t>(ms));

   return core::date_time::format(time, "%d %b %Y %H:%M:%S");
}

boost::posix_time::time_duration getUtcOffset()
{
   using namespace boost::posix_time;
   typedef boost::date_time::c_local_adjustor<ptime> local_adj;

   const ptime utc_now = second_clock::universal_time();
   const ptime now = local_adj::utc_to_local(utc_now);

   return now - utc_now;
}

std::string getUtcOffsetString()
{
   using namespace boost::posix_time;

   std::stringstream out;

   time_facet* tf = new time_facet();
   tf->time_duration_format("%+%H:%M");
   out.imbue(std::locale(out.getloc(), tf));

   out << getUtcOffset();

   return out.str();
}
   
} // namespace date_time
} // namespace core 
} // namespace rstudio



