/*
 * ConnectionRetryProfile.hpp
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

#ifndef CORE_HTTP_CONNECTION_RETRY_PROFILE_HPP
#define CORE_HTTP_CONNECTION_RETRY_PROFILE_HPP

#include <boost/function.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace core {

class Error;

namespace http {

class Request;

struct ConnectionRetryProfile
{
   ConnectionRetryProfile()
      : maxWait(boost::posix_time::not_a_date_time),
        retryInterval(boost::posix_time::not_a_date_time)
   {
   }

   ConnectionRetryProfile(
         const boost::posix_time::time_duration& maxWait,
         const boost::posix_time::time_duration& retryInterval,
         const boost::function<Error(const http::Request&,bool)>& recoveryFunction =
                   boost::function<Error(const http::Request&,bool)>())
      : maxWait(maxWait),
        retryInterval(retryInterval),
        recoveryFunction(recoveryFunction)
   {
   }

   bool empty() const { return maxWait.is_not_a_date_time(); }

   boost::posix_time::time_duration maxWait;
   boost::posix_time::time_duration retryInterval;
   boost::function<Error(const http::Request&,bool)> recoveryFunction;
};


} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_CONNECTION_RETRY_PROFILE_HPP
