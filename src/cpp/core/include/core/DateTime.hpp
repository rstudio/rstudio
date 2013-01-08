/*
 * DateTime.hpp
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

#ifndef CORE_DATE_TIME_HPP
#define CORE_DATE_TIME_HPP

#include <ctime>

#include <boost/date_time/posix_time/posix_time.hpp>

namespace core {
namespace date_time {

double secondsSinceEpoch();   
double secondsSinceEpoch(const boost::posix_time::ptime& time);
double secondsSinceEpoch(std::time_t time);      
   
double millisecondsSinceEpoch();
double millisecondsSinceEpoch(const boost::posix_time::ptime& time);
double millisecondsSinceEpoch(std::time_t time);   
   
boost::posix_time::ptime timeFromMillisecondsSinceEpoch(int64_t ms);

std::string format(const boost::posix_time::ptime& datetime,
                   const std::string& format);

} // namespace date_time
} // namespace core 


#endif // CORE_DATE_TIME_HPP

