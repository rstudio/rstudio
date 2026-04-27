/*
 * Jwt.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <core/Jwt.hpp>
#include <core/Base64.hpp>
#include <core/DateTime.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace jwt {

namespace {

Error jwtError(const std::string& reason, const ErrorLocation& location)
{
   Error error = systemError(
      boost::system::errc::invalid_argument,
      location);
   error.addProperty("reason", reason);
   return error;
}

Result<json::Object> decodeSegment(const std::string& base64UrlSegment,
                                   const std::string& segmentName)
{
   std::string decoded;
   Error error = base64::decodeUrl(base64UrlSegment, &decoded);
   if (error)
      return Unexpected(error);

   json::Value value;
   if (value.parse(decoded) || !value.isObject())
      return Unexpected(jwtError("JWT " + segmentName +
                                 " is not a valid JSON object", ERROR_LOCATION));

   return value.getObject();
}

Result<std::string> encodeSegment(const json::Object& obj)
{
   std::string encoded;
   Error error = base64::encodeUrl(obj.write(), &encoded);
   if (error)
      return Unexpected(error);
   return encoded;
}

// Extract a numeric value from a JSON value as a double.
// Handles int, int64, and double types (e.g. post-2038 timestamps that
// exceed INT32_MAX may be stored as int64 by JSON parsers).
boost::optional<double> numericValue(const json::Value& val)
{
   if (val.isInt64())
      return static_cast<double>(val.getInt64());
   else if (val.isInt())
      return static_cast<double>(val.getInt());
   else if (val.isDouble())
      return val.getDouble();
   return boost::none;
}

} // anonymous namespace

Jwt::Jwt()
{
   header_["alg"] = "none";
   header_["typ"] = "JWT";
}

Result<Jwt> Jwt::decode(const std::string& token)
{
   size_t firstDot = token.find('.');
   if (firstDot == std::string::npos)
      return Unexpected(jwtError("JWT has no dot separators", ERROR_LOCATION));

   size_t secondDot = token.find('.', firstDot + 1);
   if (secondDot == std::string::npos)
      return Unexpected(jwtError("JWT has only one dot separator", ERROR_LOCATION));

   auto headerResult = decodeSegment(token.substr(0, firstDot), "header");
   if (!headerResult.has_value())
      return Unexpected(headerResult.error());

   auto claimsResult = decodeSegment(
      token.substr(firstDot + 1, secondDot - firstDot - 1), "payload");
   if (!claimsResult.has_value())
      return Unexpected(claimsResult.error());

   Jwt jwt;
   jwt.header_ = headerResult.value();
   jwt.claims_ = claimsResult.value();
   return jwt;
}

Result<std::string> Jwt::encode() const
{
   auto headerResult = encodeSegment(header_);
   if (!headerResult.has_value())
      return Unexpected(headerResult.error());

   auto claimsResult = encodeSegment(claims_);
   if (!claimsResult.has_value())
      return Unexpected(claimsResult.error());

   return headerResult.value() + "." + claimsResult.value() + ".";
}

// -- Registered claim accessors --

boost::optional<std::string> Jwt::issuer() const { return stringClaim("iss"); }
boost::optional<std::string> Jwt::subject() const { return stringClaim("sub"); }
boost::optional<std::string> Jwt::audience() const
{
   json::Object::Iterator it = claims_.find("aud");
   if (it == claims_.end())
      return boost::none;
   json::Value val = (*it).getValue();
   if (val.isString())
      return val.getString();
   if (val.isArray())
   {
      json::Array arr = val.getArray();
      if (arr.getSize() > 0 && arr[0].isString())
         return arr[0].getString();
   }
   return boost::none;
}

std::vector<std::string> Jwt::audiences() const
{
   std::vector<std::string> result;
   json::Object::Iterator it = claims_.find("aud");
   if (it == claims_.end())
      return result;
   json::Value val = (*it).getValue();
   if (val.isString())
   {
      result.push_back(val.getString());
   }
   else if (val.isArray())
   {
      json::Array arr = val.getArray();
      for (size_t i = 0; i < arr.getSize(); ++i)
      {
         if (arr[i].isString())
            result.push_back(arr[i].getString());
      }
   }
   return result;
}

boost::optional<std::string> Jwt::jwtId() const { return stringClaim("jti"); }

boost::optional<boost::posix_time::ptime> Jwt::expiration() const { return timeClaim("exp"); }
boost::optional<boost::posix_time::ptime> Jwt::notBefore() const { return timeClaim("nbf"); }
boost::optional<boost::posix_time::ptime> Jwt::issuedAt() const { return timeClaim("iat"); }

// -- Registered claim setters --

void Jwt::setIssuer(const std::string& iss) { claims_["iss"] = iss; }
void Jwt::setSubject(const std::string& sub) { claims_["sub"] = sub; }
void Jwt::setAudience(const std::string& aud) { claims_["aud"] = aud; }
void Jwt::setJwtId(const std::string& jti) { claims_["jti"] = jti; }

void Jwt::setExpiration(const boost::posix_time::ptime& exp)
{
   claims_["exp"] = static_cast<int64_t>(date_time::secondsSinceEpoch(exp));
}

void Jwt::setNotBefore(const boost::posix_time::ptime& nbf)
{
   claims_["nbf"] = static_cast<int64_t>(date_time::secondsSinceEpoch(nbf));
}

void Jwt::setIssuedAt(const boost::posix_time::ptime& iat)
{
   claims_["iat"] = static_cast<int64_t>(date_time::secondsSinceEpoch(iat));
}

// -- Private helpers --

boost::optional<std::string> Jwt::stringClaim(const std::string& name) const
{
   json::Object::Iterator it = claims_.find(name);
   if (it == claims_.end())
      return boost::none;
   json::Value val = (*it).getValue();
   if (!val.isString())
      return boost::none;
   return val.getString();
}

boost::optional<boost::posix_time::ptime> Jwt::timeClaim(const std::string& name) const
{
   json::Object::Iterator it = claims_.find(name);
   if (it == claims_.end())
      return boost::none;
   auto val = numericValue((*it).getValue());
   if (!val)
      return boost::none;
   return date_time::timeFromSecondsSinceEpoch(*val);
}

} // namespace jwt
} // namespace core
} // namespace rstudio
