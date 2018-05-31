/*
 * SessionSignature.cpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include <core/Error.hpp>
#include <core/http/Util.hpp>
#include <core/system/Crypto.hpp>

#include <server_core/sessions/SessionSignature.hpp>

namespace rstudio {
namespace server_core {
namespace sessions {

using namespace core;
using namespace core::system;

// redefinition here so we do not have to include session constants
#define kRStudioUserIdentityDisplay     "X-RStudioUserIdentity"

Error signRequest(const std::string& rsaPrivateKey,
                  http::Request& request,
                  bool includeUsername)
{
   std::string username;
   if (includeUsername)
   {
      // get request username - this MUST be set
      username = request.headerValue(kRStudioUserIdentityDisplay);
      if (username.empty())
         return systemError(boost::system::errc::permission_denied, "No user specified", ERROR_LOCATION);
   }

   // get request date - stamp our own if it is not present
   // including the date in message signatures prevents replay attacks
   std::string date = request.headerValue("Date");
   if (date.empty())
   {
      date = http::util::httpDate();
      request.setHeader("Date", date);
   }

   std::string payload = includeUsername ? username + "\n" : std::string();
   payload += (date + "\n" + request.body());

   // calculate message signature
   std::string signature;
   Error error = crypto::rsaSign(payload, rsaPrivateKey, &signature);
   if (error)
      return error;

   // base-64 encode the signature so it can be put on the HTTP request
   std::vector<unsigned char> signatureData;
   std::copy(signature.begin(), signature.end(), std::back_inserter(signatureData));

   std::string signatureHeader;
   error = crypto::base64Encode(signatureData, &signatureHeader);
   if (error)
      return error;

   // stamp signature on the reuqest
   request.setHeader(kRStudioMessageSignature, signatureHeader);
   return Success();
}

Error verifyRequestSignature(const std::string& rsaPublicKey,
                             const http::Request& request,
                             bool includeUsername)
{
   return verifyRequestSignature(rsaPublicKey, std::string(), request, includeUsername);
}

Error verifyRequestSignature(const std::string& rsaPublicKey,
                             const std::string& expectedUser,
                             const http::Request& request,
                             bool includeUsername)
{
   // get signature from request
   std::string signature = request.headerValue(kRStudioMessageSignature);
   if (signature.empty())
   {
      return systemError(boost::system::errc::permission_denied,
                        "No signature specified on request",
                         ERROR_LOCATION);
   }

   // base-64 decode the signature included on the request
   // it is encoded to allow transmission of the binary payload over HTTP
   std::vector<unsigned char> decoded;
   Error error = core::system::crypto::base64Decode(signature, &decoded);
   if (error)
      return error;

   // construct a string representation of the decoded data
   std::string decodedSignature(decoded.begin(), decoded.end());

   // get date from signature - used in signature calculation
   // to prevent replay attacks
   std::string date = request.headerValue("Date");
   if (date.empty())
   {
      return systemError(boost::system::errc::permission_denied,
                        "No date specified on request",
                         ERROR_LOCATION);
   }

   // ensure that the date is actually valid - ensures attacker doesn't supply a phoney date that
   // wraps to the desired value or somehow exploits the system
   if (!core::http::util::isValidDate(date))
   {
      return systemError(boost::system::errc::permission_denied,
                        "Invalid date specified on request",
                         ERROR_LOCATION);
   }

   // ensure that the request is not stale - if it is, fail out as it could be a replay attack
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
   boost::posix_time::time_duration timeDelta = now - http::util::parseHttpDate(date);
   if (abs(timeDelta.total_seconds()) > 60)
   {
      return systemError(boost::system::errc::permission_denied,
                        "Received stale message with date " + date,
                         ERROR_LOCATION);
   }

   // get username from request
   std::string username = request.headerValue(kRStudioUserIdentityDisplay);
   if (includeUsername && username.empty())
   {
      return systemError(boost::system::errc::permission_denied,
                        "No username specified on request",
                         ERROR_LOCATION);
   }

   // ensure the user matches who we expect it to
   if (!expectedUser.empty() && username != expectedUser)
   {
      return systemError(boost::system::errc::permission_denied,
                        "Request from invalid user " + username +
                            ", expected " + expectedUser,
                         ERROR_LOCATION);
   }

   // calculate expected signature
   std::string payload = includeUsername ?  username + "\n" : std::string();
   payload += (date + "\n" + request.body());

   // ensure specified signature is valid
   error = core::system::crypto::rsaVerify(payload, decodedSignature, rsaPublicKey);
   if (error)
      return error;

   return Success();
}

} // namespace sessions
} // namespace server_core
} // namespace rstudio


