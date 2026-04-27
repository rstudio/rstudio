/*
 * Jwt.hpp
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

#ifndef CORE_JWT_HPP
#define CORE_JWT_HPP

#include <string>
#include <vector>

#include <boost/optional.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Result.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
namespace jwt {

// Represents a decoded (but not signature-verified) JSON Web Token (RFC 7519).
// Provides typed accessors for registered claims and access to the full
// header/claims objects for custom fields.
//
// This class does NOT perform signature verification. If you need to verify
// signatures, do so before or after decoding.
class Jwt
{
public:
   // Decode a JWT string (header.payload.signature) without verifying the
   // signature. Returns an error if the token structure is malformed.
   static Result<Jwt> decode(const std::string& token);

   // Construct an empty JWT suitable for building and encoding.
   Jwt();

   // Encode this JWT as an unsigned token string (alg: "none", empty signature).
   Result<std::string> encode() const;

   // -- Registered claim accessors (RFC 7519 §4.1) --

   boost::optional<std::string> issuer() const;
   boost::optional<std::string> subject() const;
   // Per RFC 7519 §4.1.3, "aud" may be a string or an array of strings.
   // audience() returns the single string, or the first element of the array.
   // audiences() returns all values as a vector.
   boost::optional<std::string> audience() const;
   std::vector<std::string> audiences() const;
   boost::optional<boost::posix_time::ptime> expiration() const;
   boost::optional<boost::posix_time::ptime> notBefore() const;
   boost::optional<boost::posix_time::ptime> issuedAt() const;
   boost::optional<std::string> jwtId() const;

   // -- Registered claim setters --

   void setIssuer(const std::string& iss);
   void setSubject(const std::string& sub);
   void setAudience(const std::string& aud);
   void setExpiration(const boost::posix_time::ptime& exp);
   void setNotBefore(const boost::posix_time::ptime& nbf);
   void setIssuedAt(const boost::posix_time::ptime& iat);
   void setJwtId(const std::string& jti);

   // -- Full header/claims access for custom fields --

   const json::Object& header() const { return header_; }
   json::Object& header() { return header_; }

   const json::Object& claims() const { return claims_; }
   json::Object& claims() { return claims_; }

private:
   // Read a string claim, returning empty if missing or wrong type.
   boost::optional<std::string> stringClaim(const std::string& name) const;

   // Read a numeric claim as a ptime, handling int/int64/double.
   boost::optional<boost::posix_time::ptime> timeClaim(const std::string& name) const;

   json::Object header_;
   json::Object claims_;
};

} // namespace jwt
} // namespace core
} // namespace rstudio

#endif // CORE_JWT_HPP
