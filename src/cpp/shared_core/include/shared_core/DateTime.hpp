/*
 * DateTime.hpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#ifndef SHARED_CORE_DATE_TIME_HPP
#define SHARED_CORE_DATE_TIME_HPP

#include <boost/date_time/local_time/local_time.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <string>

namespace rstudio {
namespace core {
namespace date_time {

constexpr const char* kIso8601Format {"%Y-%m-%dT%H:%M:%S%FZ"};

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

inline bool parseUtcTimeFromFormatString(const std::string& timeStr,
                                         const std::string& formatStr,
                                         boost::posix_time::ptime *pOutTime)
{
   using namespace boost::local_time;

   std::stringstream ss(timeStr);
   local_time_input_facet* ifc = new local_time_input_facet(formatStr);

   ss.imbue(std::locale(ss.getloc(), ifc));

   local_date_time ldt(not_a_date_time);

   if (ss >> ldt)
   {
      *pOutTime = ldt.utc_time();
      return true;
   }

   return false;
}

inline bool parseUtcTimeFromIsoString(const std::string& timeStr,
                                      boost::posix_time::ptime *pOutTime)
{
   return parseUtcTimeFromFormatString(timeStr,
                                       "%Y-%m-%d %H:%M:%S %ZP",
                                       pOutTime);
}

inline bool parseUtcTimeFromIso8601String(const std::string& timeStr,
                                          boost::posix_time::ptime *pOutTime)
{
   return parseUtcTimeFromFormatString(timeStr,
                                       kIso8601Format,
                                       pOutTime);
}

} // namespace date_time
} // namespace core
} // namespace rstudio

#endif
