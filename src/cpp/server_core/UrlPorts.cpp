/*
 * UrlPorts.cpp
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

#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_int.hpp>
#include <boost/random/variate_generator.hpp>
#include <boost/format.hpp>

#include <server_core/UrlPorts.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/RegexUtils.hpp>
#include <iomanip>

/* Port transformation is done in order to obscure port values in portmapped URLs. This improves
 * privacy, and makes it more difficult to try to connect to a local service as someone else. The
 * process works as follows:
 *
 * 1. On login, a cookie is set with 6 random bytes, which form the token for the transformation.
 *    The first two form the multiplier, and the last four are the key.
 *
 *    e.g. port-token=a433e59dc087 => multiplier a433, key e59dc087
 *
 * 2. To build an obscured port (for constructing a URL), the 6 bytes of the token are mixed with
 *    the 2 byte port value to create an obscured 4-byte (plus server nibble) port value, as follows:
 *
 *    a. The port number is sent through a modular multiplicative inverse. This doesn't add any
 *       security; it just obscures common ports in 2-byte space. 
 *
 *    b. The 2-byte value is multiplied by the 2-byte multiplier to get a 4-byte value.
 *
 *    c. The 4 byte value is XOR'ed with the key to form the final 4-byte obscured value.
 *
 *    d. If the port to be obscured is a server port (as opposed to a session port), an
 *       additional nibble based on the first digit of the key is prefixed to the obscured value.
 * 
 *    e. A URL is formed using a hex-encoded version of the value, e.g. /p/58fab3e4.
 *
 * 3. When processing portmapped URLs, the value from the port-token cookie is used to run the
 *    algorithm above in reverse to recover the raw port value.
 *
 * Note that this system is NOT CRYPTOGRAPHICALLY SECURE; in particular, if it's possible to observe
 * many obscured values from the same session, information about that session's token can be
 * inferred. This system is designed only to prevent casual attempts to abuse portmapped URLs by
 * making them user-specific and difficult to predict without prior knowledge. Any web service
 * running on the same host as RStudio Server should implement best practices for cross-site request
 * forgery (CSRF).
 */

#define TRANSFORM_PORT(x)   ((x * 8854)  % 65535)
#define DETRANSFORM_PORT(x) ((x * 61279) % 65535)

namespace rstudio {
namespace server_core {
namespace {

bool splitToken(const std::string& token, uint32_t* pMultiplier, uint64_t* pKey)
{
   try
   {
      // split the token into the multiplier and the key
      *pMultiplier = std::stoi(token.substr(0, 4), nullptr, 16);
      *pKey = std::stoul(token.substr(4), nullptr, 16);
      return true;
   }
   CATCH_UNEXPECTED_EXCEPTION

   return false;
}

} // anonymous namespace

std::string transformPort(const std::string& token, int port, bool server)
{
   uint32_t multiplier;
   uint64_t key;
   if (splitToken(token, &multiplier, &key))
   {
      // transform, multiply, xor, and return
      uint64_t result = ((TRANSFORM_PORT(port) * multiplier) ^ key);
      if (server)
      {
         // obtain the lower 4 bits (nibble) of the key's 1st byte
         // if zero, assume it to be at least one - otherwise when
         // formatted a leading zero wouldn't show up!
         // use this 9th digit to indicate server routing
         uint64_t nibble = ((key << 8) & 0xF00000000);
         result |= std::max(uint64_t(0x100000000), nibble);
      }
      return (boost::format("%08x") % result).str();
   }
   else
   {
      LOG_ERROR_MESSAGE("Cannot create URL with port token '" + token + "'.");
   }

   // return empty string for unexpected port
   return std::string();
}

int detransformPort(const std::string& token, const std::string& port, bool& server)
{
   uint32_t multiplier;
   uint64_t key;
   if (splitToken(token, &multiplier, &key))
   {
      try
      {
         // xor, divide, de-transform, and return
         uint64_t result = std::stoull(port, nullptr, 16);

         // a value over 8 hex digits long indicate a server routing
         server = result > 0xFFFFFFFF;
         if (server)
         {
            // the 9th digit (prefix) should be the lower
            // 4 bits of the key's 1st byte. If zero, it
            // should be at least one (to show up in the
            // formatted port string).
            uint64_t nibble = (key & 0x0F000000);
            if (nibble == 0)
               nibble = 0x01000000;

            // fails if the incoming value prefix doesn't match the key
            if (nibble != ((result & 0xF00000000) >> 8))
            {
               LOG_ERROR_MESSAGE("Invalid indicator on port token '" + token + "'.");
               return -1;
            }
            result &= 0xFFFFFFFF;
         }
         return DETRANSFORM_PORT((result ^ key) / multiplier);
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   else
   {
      LOG_ERROR_MESSAGE("Cannot use port token '" + token + "'.");
   }

   // return invalid port on failure
   return -1;
}

std::string generateNewPortToken()
{
   // configure random number generation for the token
   static boost::mt19937 gen(std::time(nullptr));
   boost::uniform_int<> dist(1, 65535);  // Avoid zeroes since we'll be multiplying and XORing
   boost::variate_generator<boost::mt19937&, boost::uniform_int<> > var(gen, dist);
   std::vector<uint32_t> token;

   // create 2 random bytes for the multiplier
   token.push_back(var());

   // create 4 random bytes for the key
   token.push_back(var());
   token.push_back(var());

   // convert to hex and format as a token
   std::ostringstream ostr;
   for (auto t: token)
      ostr << std::setw(4) << std::setfill('0') << std::hex << static_cast<uint32_t>(t);
   
   return ostr.str();
}

bool portmapPathForLocalhostUrl(const std::string& url, const std::string& token, 
      std::string* pPath)
{
   // match an http URL (ipv4 localhost or ipv6 localhst) and extract the port
   boost::regex re("http[s]?://(?:localhost|127\\.0\\.0\\.1|::1|\\[::1\\]):([0-9]+)(/.*)?");
   boost::smatch match;
   if (core::regex_utils::search(url, match, re))
   {
      bool ipv6 = (url.find("::1") != std::string::npos);

      // calculate the path
      std::string path = match[2];
      if (path.empty())
         path = "/";
      std::string portPath = ipv6 ? "p6/" : "p/";

      // convert port
      auto port = core::safe_convert::stringTo<int>(match[1]);
      if (!port)
         return false;

      // create and return computed path
      path = portPath + 
             transformPort(token, *port) + 
             path;
      *pPath = path;
      return true;
   }
   else
   {
      return false;
   }
}

}  // namespace server_core
}  // namespace rstudio
