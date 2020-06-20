/*
 * DateTime.hpp
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

#ifndef CORE_DATE_TIME_HPP
#define CORE_DATE_TIME_HPP

#include <ctime>

#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace core {
namespace date_time {

double secondsSinceEpoch();
double secondsSinceEpoch(const boost::posix_time::ptime& time);
double secondsSinceEpoch(std::time_t time);
   
double millisecondsSinceEpoch();
double millisecondsSinceEpoch(const boost::posix_time::ptime& time);
double millisecondsSinceEpoch(std::time_t time);
   
boost::posix_time::ptime timeFromSecondsSinceEpoch(double sec);
boost::posix_time::ptime timeFromMillisecondsSinceEpoch(int64_t ms);
inline boost::posix_time::ptime timeFromStdTime(std::time_t t)
{
   return boost::posix_time::ptime(boost::gregorian::date(1970,1,1)) +
         boost::posix_time::seconds(static_cast<long>(t));
}

template <typename TimeType>
std::string format(const TimeType& time,
                   const std::string& format)
{
   using namespace boost::posix_time;

   // facet for http date (construct w/ a_ref == 1 so we manage memory)
   time_facet httpDateFacet(1);
   httpDateFacet.format(format.c_str());

   // output and return the date
   std::ostringstream dateStream;
   dateStream.imbue(std::locale(dateStream.getloc(), &httpDateFacet));
   dateStream << time;
   return dateStream.str();
}

std::string millisecondsSinceEpochAsString(double ms);

bool parseUtcTimeFromIsoString(const std::string& timeStr,
                               boost::posix_time::ptime *pOutTime);

extern const std::string kIso8601Format;

bool parseUtcTimeFromIso8601String(const std::string& timeStr,
                                   boost::posix_time::ptime *pOutTime);

bool parseUtcTimeFromFormatString(const std::string& timeStr,
                                  const std::string& formatStr,
                                  boost::posix_time::ptime *pOutTime);

bool parseUtcTimeFromFormatStrings(const std::string& timeStr,
                                   const std::vector<std::string>& formats,
                                   boost::posix_time::ptime *pOutTime,
                                   std::string *pMatchingFormat);

boost::posix_time::time_duration getUtcOffset();

std::string getUtcOffsetString();

} // namespace date_time
} // namespace core 
} // namespace rstudio


#endif // CORE_DATE_TIME_HPP

