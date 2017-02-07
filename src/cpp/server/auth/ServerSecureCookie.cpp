/*
 * ServerSecureCookie.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <server/auth/ServerSecureCookie.hpp>

#include <sys/stat.h>

#include <boost/optional.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/gregorian/gregorian.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/URL.hpp>
#include <core/http/Cookie.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/Util.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <core/system/Crypto.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/FileMode.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerSecureKeyFile.hpp>

namespace rstudio {
namespace server {
namespace auth {
namespace secure_cookie {

namespace {

// cookie field delimiter
const char * const kDelim = "|";

// secure cookie key
std::string s_secureCookieKey ;


Error base64HMAC(const std::string& value,
                 const std::string& expires,
                 std::string* pHMAC)
{
   // compute message to apply hmac to
   return hashWithSecureKey(value + expires, pHMAC);
}

http::Cookie createSecureCookie(
                          const std::string& name,
                          const std::string& value,
                          const core::http::Request& request,
                          const boost::posix_time::time_duration& validDuration,
                          const std::string& path)
{
   // generate expires string
   std::string expires = http::util::httpDate(
            boost::posix_time::second_clock::universal_time() + validDuration);

   // form signed cookie value (will simply be an empty string if an
   // error occurs during encoding. this will cause the application to
   // fail downstream which is what we want given that we couldn't properly
   // secure the cookie)
   std::string signedCookieValue ;
   std::string hmac;
   Error error = base64HMAC(value, expires, &hmac);
   if (error)
   {
      LOG_ERROR(error);
      signedCookieValue = "";
   }
   else
   {
      signedCookieValue = http::util::urlEncode(value) +
                          kDelim +
                          http::util::urlEncode(expires) +
                          kDelim +
                          http::util::urlEncode(hmac);
   }

   // return the cookie
   return http::Cookie(request,
                       name,
                       signedCookieValue,
                       path,
                       true, // HTTP only
                       // secure if delivered via SSL
                       options().getOverlayOption("ssl-enabled") == "1");
}

} // anonymous namespace

Error hashWithSecureKey(const std::string& message, std::string* pHMAC)
{
   // get cookie key
   // NOTE: threadsafe because we never modify s_secureCookieKey and
   // the c_str accessor just returns a pointer to the internal data
   const char * cookieKey = s_secureCookieKey.c_str();

   // compute hmac for the message
   std::vector<unsigned char> hmac;
   Error error = core::system::crypto::HMAC_SHA2(message, cookieKey, &hmac);
   if (error)
      return error;

   // base 64 encode it
   return core::system::crypto::base64Encode(hmac, pHMAC);
}

std::string readSecureCookie(const core::http::Request& request,
                             const std::string& name)
{
   // get the signed cookie value
   std::string signedCookieValue = request.cookieValue(name);
   if (signedCookieValue.empty())
      return std::string();

   // split it into its parts (url decode them as well)
   std::string value, expires, hmac;
   using namespace boost;
   char_separator<char> separator(kDelim);
   tokenizer<char_separator<char> > cookieTokens(signedCookieValue, separator);
   tokenizer<char_separator<char> >::iterator cookieIter = cookieTokens.begin();
   if (cookieIter != cookieTokens.end())
      value = http::util::urlDecode(*cookieIter++);
   if (cookieIter != cookieTokens.end())
      expires = http::util::urlDecode(*cookieIter++);
   if (cookieIter != cookieTokens.end())
      hmac = http::util::urlDecode(*cookieIter++);

   // validate we got all the parts
   if ((cookieIter != cookieTokens.end()) ||
        value.empty() ||
        expires.empty() ||
        hmac.empty())
   {
      LOG_WARNING_MESSAGE("Invalid secure cookie (wrong number of fields): " +
                          signedCookieValue);
      return std::string();
   }

   // compute the hmac of the value + expires
   std::string computedHmac;
   Error error = base64HMAC(value, expires, &computedHmac);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   // compare hmac to the one in the cookie
   if (hmac != computedHmac)
   {
      // will occur in normal course of operations if the user upgrades
      // their browser (and the User-Agent changes). could also occur
      // in the case of an attempted forgery
      return std::string();
   }

   // check the expiration
   using namespace boost::posix_time;
   ptime expiresTime = http::util::parseHttpDate(expires);
   if (expiresTime.is_not_a_date_time())
      return std::string();
   else if (expiresTime <= second_clock::universal_time())
      return std::string();

   // ok to return the value
   return value;
}

void set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const std::string& path,
         http::Response* pResponse)
{
   secure_cookie::set(name,
                      value,
                      request,
                      validDuration,
                      boost::none,
                      path,
                      pResponse);
}

void set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const boost::optional<boost::gregorian::days>& cookieExpiresDays,
         const std::string& path,
         http::Response* pResponse)
{
   // create secure cookie
   http::Cookie cookie = createSecureCookie(name,
                                            value,
                                            request,
                                            validDuration,
                                            path);

   // expire from browser as requested
   if (cookieExpiresDays.is_initialized())
      cookie.setExpires(*cookieExpiresDays);

   // add to response
   pResponse->addCookie(cookie);
}

void remove(const http::Request& request,
            const std::string& name,
            const std::string& path,
            core::http::Response* pResponse)
{
   // create vanilla cookie (no need for secure cookie since we are removing)
   http::Cookie cookie(request, name, std::string(), path);

   // expire delete
   cookie.setExpiresDelete();

   // secure cookies are set http only, so clear them that way
   cookie.setHttpOnly();

   // add to response
   pResponse->addCookie(cookie);
}

Error initialize()
{
   return key_file::readSecureKeyFile("secure-cookie-key", &s_secureCookieKey);
}


} // namespace secure_cookie
} // namespace auth
} // namespace server
} // namespace rstudio
