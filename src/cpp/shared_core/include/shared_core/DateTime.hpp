/*
 * DateTime.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
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

#include <algorithm>
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

// Parses dates in ISO 8601:2004 Extended format (YYYY-MM-DD hh:mm:ss).
// Ignores time zone specifiers.
inline bool parseUtcTimeFromIsoString(const std::string& timeStr,
                                      boost::posix_time::ptime *pOutTime)
{
   return parseUtcTimeFromFormatString(timeStr,
                                       "%Y-%m-%d %H:%M:%S %ZP",
                                       pOutTime);
}

// Parses dates in ISO 8601:2019 Extended format (YYYY-MM-DDThh:mm:ss).
// Ignores time zone specifiers.
inline bool parseUtcTimeFromIso8601String(const std::string& timeStr,
                                          boost::posix_time::ptime *pOutTime)
{
   return parseUtcTimeFromFormatString(timeStr,
                                       kIso8601Format,
                                       pOutTime);
}

// Parses dates in any of the following formats:
//    UNIX timestamp (in seconds, no fractional seconds)
//    ISO 8601:2004 Basic (YYYYMMDD hhmmss.sss)
//    ISO 8601:2004 Extended (YYYY-MM-DD hh:mm:ss.sss)
//    ISO 8601:2019 Basic (YYYYMMDDThhmmss.sss)
//    ISO 8601:2019 Extended (YYYY-MM-DDThh:mm:ss.sss)
// Fractional seconds are optional. The ISO 8601 formats support time zones
// in the form Z, [+-]hh, [+-]hhmm, or [+-]hh:mm. If no zone information is
// provided, UTC is assumed.
//
// The return value is always in UTC.
inline bool parseUtcTimeFromZoneString(std::string timeStr,
                                       boost::posix_time::ptime *pOutTime)
{
   try {
      if (std::all_of(timeStr.begin(), timeStr.end(), [](char ch){ return std::isdigit(ch); }))
      {
         // purely numeric, treat as UNIX timestamp
         time_t timestamp = std::stoll(timeStr);
         *pOutTime = boost::posix_time::from_time_t(timestamp);
         return true;
      }

      std::string zoneStr;
      std::size_t zonePos = timeStr.rfind('Z');
      if (zonePos == std::string::npos)
      {
         zonePos = timeStr.rfind('+');
         if (zonePos == std::string::npos)
         {
            // only check the last 6 chars to avoid getting confused by a - in the date
            zonePos = timeStr.find('-', timeStr.size() - 6);
         }
      }
      if (zonePos != std::string::npos)
      {
         zoneStr = timeStr.substr(zonePos);
         timeStr = timeStr.substr(0, zonePos);
      }

      std::size_t tPos = timeStr.find('T');
      if (tPos == std::string::npos)
         tPos = timeStr.find(' ');

      std::size_t dashPos = timeStr.find('-');
      if (dashPos == std::string::npos)
      {
         // ISO 8601 Basic
         timeStr[tPos] = 'T'; // convert 2004 to 2019
         *pOutTime = boost::posix_time::from_iso_string(timeStr);
      }
      else
      {
         // ISO 8601 Extended
         timeStr[tPos] = ' '; // convert 2019 to 2004 because that's what this version of Boost supports
         *pOutTime = boost::posix_time::time_from_string(timeStr);
      }

      if (!zoneStr.empty() && zoneStr != "Z")
      {
         int hours = 0;
         int minutes = 0;
         if (zoneStr.size() <= 3)
         {
            hours = std::stoi(zoneStr);
         }
         else
         {
            // Split the string this way in case there's a colon
            hours = std::stoi(zoneStr.substr(0, 3));
            minutes = std::stoi(zoneStr.substr(zoneStr.size() - 2));
         }

         if (hours < 0)
            minutes = -minutes;

         minutes += hours * 60;

         *pOutTime -= boost::posix_time::minutes(minutes);
      }
      return true;
   }
   catch (boost::bad_lexical_cast&)
   {
      // invalid input string, return failure but it's not an error
      return false;
   }
   catch (std::exception& e)
   {
#ifdef LOG_DEBUG_MESSAGE
      LOG_DEBUG_MESSAGE(std::string("Error parsing date \"") + timeStr + "\": " + e.what());
#endif
      return false;
   }
}

} // namespace date_time
} // namespace core
} // namespace rstudio

#endif
