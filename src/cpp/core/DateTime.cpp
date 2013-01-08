/*
 * DateTime.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/DateTime.hpp>

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
   return diff.total_milliseconds();
}
   
double millisecondsSinceEpoch(std::time_t time)
{
   return std::difftime(time, 0) * 1000;
}
   
boost::posix_time::ptime timeFromMillisecondsSinceEpoch(int64_t ms)
{
   using namespace boost::gregorian;
   using namespace boost::posix_time;

   ptime time_t_epoch(date(1970,1,1));
   return time_t_epoch + milliseconds(ms);
}

std::string format(const boost::posix_time::ptime& datetime,
                   const std::string& format)
{
   using namespace boost::posix_time;

   // facet for http date (construct w/ a_ref == 1 so we manage memory)
   time_facet httpDateFacet(1);
   httpDateFacet.format(format.c_str());

   // output and return the date
   std::ostringstream dateStream;
   dateStream.imbue(std::locale(dateStream.getloc(), &httpDateFacet));
   dateStream << datetime;
   return dateStream.str();
}
   
} // namespace date_time
} // namespace core 



