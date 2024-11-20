/*
 * SecureCookie.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */



#include <sys/stat.h>

#include <boost/optional.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/gregorian/gregorian.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/URL.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/Util.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <core/system/Crypto.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/Xdg.hpp>

#include <server_core/http/SecureCookie.hpp>
#include <server_core/SecureKeyFile.hpp>

#ifdef RSTUDIO_HAS_SOCI
#include <server_core/ServerDatabase.hpp>
#endif

namespace rstudio {
namespace core {
namespace http {
namespace secure_cookie {

namespace secure_cookie_overlay {
   Error initialize(const FilePath& secureKeyFile);
}
namespace {

// cookie field delimiter
const char * const kDelim = "|";
const char * const kExpiresDelim = ";";

// secure cookie key
std::string s_secureCookieKey;
std::string s_secureCookieKeyPath; // absolute path to secure-cookie-file used to obtain the key value
std::string s_secureCookieKeyHash; // 1-way hash of the secureCookieKey value

Error base64HMAC(const std::string& value,
                 const std::string& expires,
                 std::string* pHMAC)
{
   // compute message to apply hmac to
   return hashWithSecureKey(value + expires, pHMAC);
}

Error ensureKeyStrength(const std::string& key)
{
   // ensure the key is at least 256 bits (32 bytes) in strength
   // this is good security practice, and our encryption and decryption
   // operations require a key of that size at least
   if (key.size() < 32)
   {
      LOG_ERROR_MESSAGE("secure-cookie-key specified is not strong enough! It must be at least 256 bits (32 bytes/characters) long!");
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }

   return Success();
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
   return core::system::crypto::base64Encode(hmac, *pHMAC);
}

http::Cookie createSecureCookie(const std::string& name,
                                const std::string& value,
                                const core::http::Request& request,
                                const boost::posix_time::time_duration& validDuration,
                                const boost::optional<boost::posix_time::ptime>& loginExpiry,
                                const std::string& path /* = "/"*/,
                                bool secure /*= false*/,
                                http::Cookie::SameSite sameSite /*= SameSite::Undefined*/)
{
   // generate expires string
   boost::posix_time::ptime startTime = boost::posix_time::second_clock::universal_time();
   std::string expires = http::util::httpDate(startTime + validDuration);

   if (loginExpiry)
      expires += kExpiresDelim + http::util::httpDate(loginExpiry.get());

   // form signed cookie value (will simply be an empty string if an
   // error occurs during encoding. this will cause the application to
   // fail downstream which is what we want given that we couldn't properly
   // secure the cookie)
   std::string signedCookieValue;
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
                       sameSite,
                       true, // HTTP only
                       secure);
}

std::string readSecureCookie(const core::http::Request& request,
                             const std::string& name)
{
   // get the signed cookie value
   std::string signedCookieValue = request.cookieValue(name);
   if (signedCookieValue.empty())
      return std::string();

   return readSecureCookie(signedCookieValue);
}

std::string readSecureCookie(const std::string& signedCookieValue)
{
   std::string value;
   boost::posix_time::ptime expires;
   boost::optional<boost::posix_time::ptime> loginExpiry;
   readSecureCookie(signedCookieValue, &value, &expires, &loginExpiry);

   // ok to return the value
   return value;
}

void readSecureCookie(
   const std::string& signedCookieValue,
   std::string* pValue,
   boost::posix_time::ptime* pExpires,
   boost::optional<boost::posix_time::ptime>* pLoginExpiry)
{
   *pValue = "";
   *pLoginExpiry = boost::none;

   // split it into its parts (url decode them as well)
   std::string value, expires, hmac;

   using namespace boost;
   posix_time::ptime loginExpiry;

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
      return;
   }

   // compute the hmac of the value + expires
   std::string computedHmac;
   Error error = base64HMAC(value, expires, &computedHmac);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // compare hmac to the one in the cookie
   if (hmac != computedHmac)
   {
      // will occur in normal course of operations if the user upgrades
      // their browser (and the User-Agent changes). could also occur
      // in the case of an attempted forgery
      return;
   }

   // check the expiration
   using namespace boost::posix_time;
   if (expires.find(kExpiresDelim) != std::string::npos)
   {
      // expires contains a login expiry, so we need to parse it out
      std::string loginExpiryStr = expires.substr(expires.find(kExpiresDelim) + 1);
      expires = expires.substr(0, expires.find(kExpiresDelim));

      loginExpiry = http::util::parseHttpDate(loginExpiryStr);
      if (loginExpiry.is_not_a_date_time())
         return;
      else if (loginExpiry <= second_clock::universal_time())
         return;

      *pLoginExpiry = loginExpiry;
   }

   ptime expiresTime = http::util::parseHttpDate(expires);
   if (expiresTime.is_not_a_date_time())
      return;
   else if (expiresTime <= second_clock::universal_time())
      return;

   *pExpires = expiresTime;
   *pValue = value;
}

http::Cookie set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const boost::optional<boost::posix_time::time_duration>& expiresFromNow,
         const boost::optional<boost::posix_time::ptime>& loginExpiry,
         const std::string& path,
         http::Response* pResponse,
         bool secure,
         http::Cookie::SameSite sameSite)
{
   // create secure cookie
   http::Cookie cookie = createSecureCookie(name,
                                            value,
                                            request,
                                            validDuration,
                                            loginExpiry,
                                            path,
                                            secure,
                                            sameSite);

   // expire from browser as requested
   if (expiresFromNow.is_initialized())
      cookie.setExpires(*expiresFromNow);

   // add to response
   pResponse->addCookie(cookie);

   return cookie;
}

void remove(const http::Request& request,
            const std::string& name,
            const std::string& path,
            core::http::Response* pResponse,
            bool secure,
            http::Cookie::SameSite sameSite)
{
   // create cookie
   http::Cookie cookie(request, name, std::string(), path);

   // expire delete
   cookie.setExpiresDelete();

   // secure cookies are set http only, so clear them that way
   cookie.setHttpOnly();

   if (secure)
   {
      // set secure flag when removing secure cookies; there's no need for a "secure" empty value
      // but best practices generally dictate always setting the secure flag on cookies delivered
      // over https
      cookie.setSecure();
   }

   cookie.setSameSite(sameSite);

   // add to response
   pResponse->addCookie(cookie);
}

const std::string& getKey()
{
   return s_secureCookieKey;
}

const std::string& getKeyFileUsed()
{
   return s_secureCookieKeyPath;
}

const std::string& getKeyHash()
{
   return s_secureCookieKeyHash;
}

Error initialize()
{
   Error error = key_file::readSecureKeyFile("secure-cookie-key", &s_secureCookieKey, &s_secureCookieKeyHash, &s_secureCookieKeyPath);
   if (error)
      return error;

   return ensureKeyStrength(s_secureCookieKey);
}

Error initialize(const FilePath& secureKeyFile)
{
   return initialize(false, secureKeyFile);
}

Error initialize(bool isLoadBalanced, const FilePath& secureKeyFile)
{
   if (secureKeyFile.isEmpty())
   {
      if (!isLoadBalanced)
         return initialize();

      FilePath secureKeyPath = core::system::xdg::findSystemConfigFile(
            "secure key", "secure-cookie-key");
      if (!secureKeyPath.exists())
         secureKeyPath = core::FilePath("/var/lib/rstudio-server")
            .completePath("secure-cookie-key");
#ifdef RSTUDIO_HAS_SOCI
      if (!secureKeyPath.exists())
      {
         LOG_DEBUG_MESSAGE("The secure-cookie-key does not exist on the filesystem, "
            "attempting to read from database");
         boost::shared_ptr<database::IConnection> pConnection;
         if (!server_core::database::getConnection(boost::posix_time::seconds(5), &pConnection))
         {
            LOG_ERROR_MESSAGE("Unable to connect to database to retrieve load-balanced secure-cookie-key");
            return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
         }
         Error error = database::execAndProcessQuery(pConnection,
            "SELECT secure_cookie_key FROM cluster",
            [=](const database::Row& row) -> Error
            {
               std::string secureCookieKey = database::getRowStringValue(row, "secure_cookie_key");
               if (!secureCookieKey.empty())
               {
                  LOG_DEBUG_MESSAGE("Secure cookie key found in database, writing to file at " + secureKeyPath.getAbsolutePath());

                  Error error = key_file::writeSecureKeyFile(secureCookieKey, secureKeyPath);
                  s_secureCookieKey = secureCookieKey;
                  s_secureCookieKeyPath = secureKeyPath.getAbsolutePath();
                  if (error)
                     return error;
               }
               return Success();
            });
         if (error)
            return error;
      }
#endif
      if (s_secureCookieKey.empty())
      {
         Error error = key_file::readSecureKeyFile(secureKeyPath, &s_secureCookieKey, &s_secureCookieKeyHash, &s_secureCookieKeyPath);
         if (error)
            return error;
      }
   }
   else
   {
      Error error = key_file::readSecureKeyFile(secureKeyFile, &s_secureCookieKey, &s_secureCookieKeyHash, &s_secureCookieKeyPath);
      if (error)
         return error;
   }

   return ensureKeyStrength(s_secureCookieKey);
}

Error initialize(const std::string& secureKey, const FilePath& secureKeyFile)
{
   Error error = key_file::writeSecureKeyFile(secureKey, secureKeyFile);
   if (error)
      return error;

   s_secureCookieKey = secureKey;
   s_secureCookieKeyPath = secureKeyFile.getAbsolutePath();

   return ensureKeyStrength(s_secureCookieKey);
}

} // namespace secure_cookie
} // namespace http
} // namespace core
} // namespace rstudio