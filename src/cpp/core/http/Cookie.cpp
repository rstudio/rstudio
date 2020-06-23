/*
 * Cookie.cpp
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

#include <core/http/Cookie.hpp>

#include <core/http/Request.hpp>
#include <core/http/URL.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>

using namespace boost::gregorian;

namespace rstudio {
namespace core {
namespace http {

Cookie::Cookie(const Request& request,
               const std::string& name,
               const std::string& value, 
               const std::string& path,
               SameSite sameSite /*= SameSite::Undefined*/,
               bool httpOnly /*= false*/, 
               bool secure /*= false*/) 
   :  name_(name), 
      value_(value), 
      path_(path), 
      expires_(not_a_date_time),
      sameSite_(sameSite),
      httpOnly_(httpOnly),
      secure_(secure)
{
   if (path.empty() && URL::complete(request.uri(), "") != "/")
   {
      // If we're here, it means we're using an implicit path that
      // isn't the root. In other words, we're setting a cookie
      // that will only apply under the current "path". Since this
      // is not something we're likely to want, we warn--if this
      // is actually the desired behavior then use an explicit path
      // value.
      LOG_WARNING_MESSAGE("Implicit path used with non-root URL (" +
                          request.uri() + ")");
   }
}
   
Cookie::~Cookie()
{
}

void Cookie::setExpires(const boost::posix_time::time_duration& expiresFromNow)
{
   expires_ = boost::posix_time::ptime(boost::posix_time::second_clock::universal_time() + expiresFromNow);
}

void Cookie::setExpiresDelete() 
{
   expires_ = boost::posix_time::ptime(date(day_clock::universal_day() - days(2)),
                                       boost::posix_time::time_duration(23, 59, 59));
}

void Cookie::setHttpOnly()   
{
   httpOnly_ = true;
}

void Cookie::setSecure()
{
   secure_ = true;
}

void Cookie::setSameSite(SameSite sameSite)
{
   sameSite_ = sameSite;
}

std::string Cookie::cookieHeaderValue() const
{
   // basic name/value
   std::ostringstream headerValue;
   headerValue << name() << "=" << value();

   // expiries if specified
   if ( !expires().is_not_a_date_time() )
   {
      date::ymd_type ymd = expires_.date().year_month_day();
      greg_weekday wd = expires_.date().day_of_week();

      headerValue << "; expires=";
      headerValue << wd.as_short_string() << ", " 
                  << ymd.day << " " << ymd.month.as_short_string() << " "
                  << ymd.year << " " << expires_.time_of_day().hours() << ":"
                  << expires_.time_of_day().minutes() << ":"
                  << expires_.time_of_day().seconds() << " GMT";
   }

   // path if specified
   if ( !path().empty() )
      headerValue << "; path=" << path();

   // domain if specified
   if ( !domain().empty() )
      headerValue << "; domain=" << domain();
   
   // http only if specified
   if (httpOnly_)
      headerValue << "; HttpOnly";

   switch (sameSite_)
   {
      case SameSite::None:
         headerValue << "; SameSite=None";
         break;
      case SameSite::Lax:
         headerValue << "; SameSite=Lax";
         break;
      case SameSite::Strict:
         headerValue << "; SameSite=Strict";
         break;
      case SameSite::Undefined:
         // do nothing
         break;
   }

   // secure if specified
   if (secure_)
      headerValue << "; secure";

   // return the header value 
   return headerValue.str();
}

} // namespace http
} // namespace core
} // namespace rstudio
