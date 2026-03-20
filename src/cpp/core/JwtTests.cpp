/*
 * JwtTests.cpp
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

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <core/DateTime.hpp>
#include <core/Jwt.hpp>

namespace rstudio {
namespace core {
namespace jwt {

// -- Decoding tests --

TEST(JwtTest, DecodeStringClaims)
{
   Jwt built;
   built.setIssuer("https://example.com");
   built.setSubject("user@test.com");
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->issuer().get(), "https://example.com");
   EXPECT_EQ(result->subject().get(), "user@test.com");
}

TEST(JwtTest, DecodeTimestampClaims)
{
   auto exp = date_time::timeFromSecondsSinceEpoch(1700000000);
   auto iat = date_time::timeFromSecondsSinceEpoch(1699999000);

   Jwt built;
   built.setExpiration(exp);
   built.setIssuedAt(iat);
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   ASSERT_TRUE(result->expiration().has_value());
   EXPECT_EQ(result->expiration().get(), exp);
   ASSERT_TRUE(result->issuedAt().has_value());
   EXPECT_EQ(result->issuedAt().get(), iat);
}

TEST(JwtTest, DecodePost2038Timestamp)
{
   // 2147483648 is INT32_MAX + 1, forcing int64 storage.
   Jwt built;
   built.claims()["exp"] = static_cast<int64_t>(2147483648);
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   ASSERT_TRUE(result->expiration().has_value());
   EXPECT_EQ(result->expiration().get(), date_time::timeFromSecondsSinceEpoch(2147483648.0));
}

TEST(JwtTest, DecodeCustomClaims)
{
   Jwt built;
   built.claims()["custom_field"] = "custom_value";
   built.claims()["numeric_field"] = 42;
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->claims()["custom_field"].getString(), "custom_value");
   EXPECT_EQ(result->claims()["numeric_field"].getInt(), 42);
}

TEST(JwtTest, DecodePreservesHeader)
{
   // External JWTs have meaningful headers (alg, kid, etc.)
   Jwt built;
   built.header()["alg"] = "RS256";
   built.header()["kid"] = "key-123";
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->header()["alg"].getString(), "RS256");
   EXPECT_EQ(result->header()["kid"].getString(), "key-123");
}

TEST(JwtTest, MissingClaimsReturnEmpty)
{
   Jwt built;
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_FALSE(result->issuer().has_value());
   EXPECT_FALSE(result->subject().has_value());
   EXPECT_FALSE(result->expiration().has_value());
   EXPECT_FALSE(result->issuedAt().has_value());
   EXPECT_FALSE(result->notBefore().has_value());
   EXPECT_FALSE(result->jwtId().has_value());
}

TEST(JwtTest, WrongTypeClaimsReturnEmpty)
{
   Jwt built;
   // Set exp as a string instead of a number
   built.claims()["exp"] = "not-a-number";
   // Set iss as a number instead of a string
   built.claims()["iss"] = 42;
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_FALSE(result->expiration().has_value());
   EXPECT_FALSE(result->issuer().has_value());
}

// -- Encoding tests --

TEST(JwtTest, EncodeProducesThreeSegments)
{
   Jwt jwt;
   auto token = jwt.encode();
   ASSERT_TRUE(token.has_value());

   size_t firstDot = token->find('.');
   ASSERT_NE(firstDot, std::string::npos);
   size_t secondDot = token->find('.', firstDot + 1);
   ASSERT_NE(secondDot, std::string::npos);

   // Unsigned JWT has empty signature (nothing after second dot)
   EXPECT_EQ(token->size(), secondDot + 1);
}

TEST(JwtTest, EncodeDecodeRoundTrip)
{
   auto exp = date_time::timeFromSecondsSinceEpoch(1800000000);

   Jwt original;
   original.setIssuer("https://auth.example.com");
   original.setSubject("alice");
   original.setAudience("my-app");
   original.setExpiration(exp);
   original.setJwtId("abc-123");

   auto token = original.encode();
   ASSERT_TRUE(token.has_value());
   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->issuer().get(), "https://auth.example.com");
   EXPECT_EQ(result->subject().get(), "alice");
   EXPECT_EQ(result->audience().get(), "my-app");
   EXPECT_EQ(result->expiration().get(), exp);
   EXPECT_EQ(result->jwtId().get(), "abc-123");
}

// -- Audience tests (RFC 7519 §4.1.3: string or array of strings) --

TEST(JwtTest, AudienceAsString)
{
   Jwt built;
   built.setAudience("my-app");
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->audience().get(), "my-app");
   ASSERT_EQ(result->audiences().size(), 1u);
   EXPECT_EQ(result->audiences()[0], "my-app");
}

TEST(JwtTest, AudienceAsArray)
{
   Jwt built;
   json::Array audArray;
   audArray.push_back("app-1");
   audArray.push_back("app-2");
   built.claims()["aud"] = audArray;
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_EQ(result->audience().get(), "app-1");
   ASSERT_EQ(result->audiences().size(), 2u);
   EXPECT_EQ(result->audiences()[0], "app-1");
   EXPECT_EQ(result->audiences()[1], "app-2");
}

TEST(JwtTest, AudienceMissingReturnsEmpty)
{
   Jwt built;
   auto token = built.encode();
   ASSERT_TRUE(token.has_value());

   auto result = Jwt::decode(token.value());
   ASSERT_TRUE(result.has_value());
   EXPECT_FALSE(result->audience().has_value());
   EXPECT_TRUE(result->audiences().empty());
}

// -- Error cases --

TEST(JwtTest, DecodeEmptyString)
{
   EXPECT_FALSE(Jwt::decode("").has_value());
}

TEST(JwtTest, DecodeNoDots)
{
   EXPECT_FALSE(Jwt::decode("nodots").has_value());
}

TEST(JwtTest, DecodeOnlyOneDot)
{
   EXPECT_FALSE(Jwt::decode("one.dot").has_value());
}

TEST(JwtTest, DecodeInvalidBase64Payload)
{
   EXPECT_FALSE(Jwt::decode("header.!!!invalid!!!.sig").has_value());
}

TEST(JwtTest, DecodeNonJsonPayload)
{
   // "not json" base64url-encoded = "bm90IGpzb24"
   EXPECT_FALSE(Jwt::decode("header.bm90IGpzb24.sig").has_value());
}

} // namespace jwt
} // namespace core
} // namespace rstudio
