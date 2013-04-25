/*
 * Cookie.cpp
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

#include <core/http/Cookie.hpp>

#include <core/http/URL.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>

using namespace boost::gregorian ;

namespace core {
namespace http {

Cookie::Cookie(const Request& request,
               const std::string& name,
               const std::string& value, 
               const std::string& path,
               bool httpOnly) 
   :  name_(name), 
      value_(value), 
      path_(path), 
      expires_(not_a_date_time),
      httpOnly_(httpOnly)
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

void Cookie::setExpires(const days& expiresDays) 
{
   expires_ = date(day_clock::universal_day() + expiresDays) ; 
}

void Cookie::setExpiresDelete() 
{
   expires_ = date(day_clock::universal_day() - days(2)) ;
}

void Cookie::setHttpOnly()   
{
   httpOnly_ = true;
}
   
std::string Cookie::cookieHeaderValue() const
{
   // basic name/value
   std::ostringstream headerValue ;
   headerValue << name() << "=" << value() ;

   // expiries if specified
   if ( !expires().is_not_a_date() )
   {
      date::ymd_type ymd = expires_.year_month_day() ;
      greg_weekday wd = expires_.day_of_week() ;

      headerValue << "; expires=" ;
      headerValue << wd.as_short_string() << ", " 
                  << ymd.day << "-" << ymd.month.as_short_string() << "-" 
                  << ymd.year << " 23:59:59 GMT" ;
   }

   // path if specified
   if ( !path().empty() )
      headerValue << "; path=" << path();

   // domain if specified
   if ( !domain().empty() )
      headerValue << "; domain=" << domain() ;
   
   // http only if specified
   if (httpOnly_)
      headerValue << "; HttpOnly";

   // return the header value 
   return headerValue.str() ;
}

} // namespace http
} // namespace core
