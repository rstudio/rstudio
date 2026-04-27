/*
 * MiscellaneousTests.cpp
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

#include <gtest/gtest.h>

#include <vector>
#include <string>
#include <iostream>

#include <shared_core/json/Json.hpp>
#include <shared_core/DateTime.hpp>

#include <core/Algorithm.hpp>
#include <core/RegexUtils.hpp>
#include <core/Truncating.hpp>
#include <core/collection/LruCache.hpp>
#include <core/collection/Position.hpp>
#include <core/http/Request.hpp>

#include <core/system/Types.hpp>

namespace rstudio {
namespace core {
namespace unit_tests {

using namespace core::collection;

class SuppressOutputScope
{
public:
   SuppressOutputScope()
   {
      pCoutBuf_ = std::cout.rdbuf(nullptr);
      pCerrBuf_ = std::cerr.rdbuf(nullptr);
   }

   ~SuppressOutputScope()
   {
      std::cout.rdbuf(pCoutBuf_);
      std::cerr.rdbuf(pCerrBuf_);
   }

private:
   std::streambuf* pCoutBuf_;
   std::streambuf* pCerrBuf_;
};

TEST(MiscTest, CanLogSuccess)
{
   // attempts to log default-constructed Error objects could segfault
   // https://github.com/rstudio/rstudio/issues/9113
   Error error;
   LOG_ERROR(error);
   LOG_ERROR(Success());
}

TEST(MiscTest, PositionComparison)
{
   EXPECT_TRUE(Position(0, 0) == Position(0, 0));
   EXPECT_TRUE(Position(0, 0) <  Position(0, 1));
   EXPECT_TRUE(Position(0, 0) <  Position(1, 0));
   EXPECT_TRUE(Position(2, 2) <  Position(2, 4));
}

TEST(MiscTest, SplitWithMultiCharDelimiters)
{
   std::string contents = "foo::bar::baz";
   std::vector<std::string> splat = core::algorithm::split(contents, "::");
   ASSERT_EQ(3u, splat.size());
   EXPECT_EQ(std::string("foo"), splat[0]);
   EXPECT_EQ(std::string("bar"), splat[1]);
   EXPECT_EQ(std::string("baz"), splat[2]);
}

TEST(MiscTest, RegexComplexityExceptionHandling)
{
   boost::regex pattern("(\\w*|\\w*)*@");
   std::string haystack =
         "abcdefghijklmnopqrstuvwxyz"
         "abcdefghijklmnopqrstuvwxyz"
         "|@";

   // boost-level APIs will throw an exception
   {
      // REQUIRE_THROWS - not directly supported in gtest
      try {
         boost::regex_match(
            haystack.begin(),
            haystack.end(),
            pattern);
         FAIL() << "Expected boost::regex_match to throw an exception";
      } catch (...) {
         // Expected exception
      }
   }

   {
      // REQUIRE_THROWS - not directly supported in gtest
      try {
         boost::regex_search(
            haystack.begin(),
            haystack.end(),
            pattern);
         FAIL() << "Expected boost::regex_search to throw an exception";
      } catch (...) {
         // Expected exception
      }
   }

   // our wrappers catch and report exception, and return false
   {
      SuppressOutputScope scope;
      bool result = core::regex_utils::match(
               haystack.begin(),
               haystack.end(),
               pattern);

      // although in theory the above regular expression matches
      // we should instead see a report regarding complexity overload
      EXPECT_FALSE(result);
   }

   {
      SuppressOutputScope scope;
      bool result = core::regex_utils::search(
               haystack.begin(),
               haystack.end(),
               pattern);

      // although in theory the above regular expression matches
      // we should instead see a report regarding complexity overload
      EXPECT_FALSE(result);
   }

}

TEST(MiscTest, HttpAcceptEncodingParsing)
{
   std::string encodingStr = "gzip,deflate,br";
   std::string encodingStr2 = "gzip, deflate, br";

   core::http::Request request;
   request.setHeader("Accept-Encoding", encodingStr);

   EXPECT_TRUE(request.acceptsEncoding("gzip"));
   EXPECT_TRUE(request.acceptsEncoding("deflate"));
   EXPECT_TRUE(request.acceptsEncoding("br"));
   EXPECT_FALSE(request.acceptsEncoding("gzip,deflate,br"));

   request.setHeader("Accept-Encoding", encodingStr2);
   EXPECT_TRUE(request.acceptsEncoding("gzip"));
   EXPECT_TRUE(request.acceptsEncoding("deflate"));
   EXPECT_TRUE(request.acceptsEncoding("br"));
   EXPECT_FALSE(request.acceptsEncoding("gzip,deflate,br"));
}

TEST(LruCacheTest, UpdatesSameValueMultipleTimes)
{
   LruCache<std::string, int> cache(10);
   for (int i = 0; i < 1000; ++i)
   {
      cache.insert("val", i);
   }

   EXPECT_EQ(1u, cache.size());

   int val;
   EXPECT_TRUE(cache.get("val", &val));
   EXPECT_EQ(999, val);
}

TEST(LruCacheTest, RespectsSizeLimit)
{
   LruCache<int, int> cache(100);
   for (int i = 0; i < 1000; ++i)
   {
      cache.insert(i, i);
   }

   EXPECT_EQ(100u, cache.size());

   int val;
   EXPECT_TRUE(cache.get(999, &val));
   EXPECT_EQ(999, val);

   EXPECT_FALSE(cache.get(100, &val));
}

TEST(LruCacheTest, ExplicitRemovalWorks)
{
   LruCache<int, int> cache(100);
   for (int i = 0; i < 100; ++i)
   {
      cache.insert(5000, i);
      cache.insert(i, i);
   }

   // expect that 0 has been kicked out
   // this is because we added an insert for 5000 so it should have
   // removed 0 (the oldest entry)
   int val;
   EXPECT_FALSE(cache.get(0, &val));

   EXPECT_TRUE(cache.get(50, &val));
   EXPECT_EQ(val, 50);

   cache.remove(50);

   EXPECT_FALSE(cache.get(50, &val));
   EXPECT_EQ(99u, cache.size());
}

TEST(LruCacheTest, ReadsUpdateAccessTime)
{
   LruCache<int, int> cache(100);
   cache.insert(5000, 1);

   int val;
   for (int i = 0; i < 1000; ++i)
   {
      EXPECT_TRUE(cache.get(5000, &val));
   EXPECT_EQ(1, val);

      cache.insert(i, i);
   }

   EXPECT_EQ(100u, cache.size());
   EXPECT_TRUE(cache.get(5000, &val));
   EXPECT_FALSE(cache.get(900, &val));
}

TEST(LruCacheTest, NoValuesAfterRemoval)
{
   LruCache<int, int> cache(100);

   for (int i = 0; i < 1000; ++i)
   {
      cache.insert(i, i);
   }

   for (int i = 999; i >= 900; --i)
   {
      cache.remove(i);
   }

   EXPECT_EQ(0u, cache.size());
}

TEST(MiscTest, OptionsSerializationDeserialization)
{
   core::system::Options options;
   options.push_back({"abc", "123"});
   options.push_back({"abc=123", "456"});
   options.push_back({"abc=", "=123"});
   options.push_back({"abc", std::string()});
   options.push_back({"abc=", std::string()});

   core::json::Array optionsArray = core::json::Array(options);
   core::system::Options options2 = optionsArray.toStringPairList();

   for (size_t i = 0; i < options.size(); ++i)
   {
      EXPECT_EQ(options[i].first, options2[i].first);
      EXPECT_EQ(options[i].second, options2[i].second);
   }
}

TEST(LruCacheTest, TruncatingHandlesNormalOperations)
{
   EXPECT_TRUE(Truncating<int>(4) + 4 == 8);
   EXPECT_TRUE(Truncating<int>(4) - 4 == 0);
   EXPECT_TRUE(Truncating<int>(4) * 4 == 16);
}

TEST(LruCacheTest, TruncatingHandlesOverflow)
{
   EXPECT_TRUE(Truncating<int>(42) + INT_MAX == INT_MAX);
   EXPECT_TRUE(Truncating<int>(42) + INT_MIN == 42 + INT_MIN);
   EXPECT_TRUE(Truncating<int>(42) - INT_MAX == 42 - INT_MAX);
   EXPECT_TRUE(Truncating<int>(42) - INT_MIN == INT_MAX);
   EXPECT_TRUE(Truncating<int>(42) * INT_MAX == INT_MAX);
   EXPECT_TRUE(Truncating<int>(42) * INT_MIN == INT_MIN);

   EXPECT_TRUE(Truncating<int>(-42) + INT_MAX == -42 + INT_MAX);
   EXPECT_TRUE(Truncating<int>(-42) + INT_MIN == INT_MIN);
   EXPECT_EQ(Truncating<int>(-42) - INT_MAX, INT_MIN);
   EXPECT_TRUE(Truncating<int>(-42) - INT_MIN == -42 - INT_MIN);
   EXPECT_TRUE(Truncating<int>(-42) * INT_MAX == INT_MIN);
   EXPECT_TRUE(Truncating<int>(-42) * INT_MIN == INT_MAX);
}

TEST(LruCacheTest, TruncatingExampleWorks)
{
   Truncating<int> x(INT_MAX);
   auto y = x + 1;
   EXPECT_EQ(y, INT_MAX);
}

TEST(LruCacheTest, TruncatingDifferentSizes)
{
   Truncating<long> x = 4;
   Truncating<short> y = 4;
   EXPECT_EQ(x + y, 8);
   EXPECT_EQ(x - y, 0);
}

TEST(DateTimeTest, ParsesTimestamps)
{
   using namespace rstudio::core::date_time;
   std::string unixTime = "1767225600";
   std::string basic2004 = "20260101 000000";
   std::string extended2004 = "2026-01-01 00:00:00.000";
   std::string basic2019 = "20260101T000000";
   std::string extended2019 = "2026-01-01T00:00:00.000";

   boost::posix_time::ptime expected = boost::posix_time::from_time_t(1767225600);

   boost::posix_time::ptime outUnix;
   EXPECT_TRUE(parseUtcTimeFromZoneString(unixTime, &outUnix));
   EXPECT_EQ(outUnix, expected);

   boost::posix_time::ptime outBasic2004;
   EXPECT_TRUE(parseUtcTimeFromZoneString(basic2004, &outBasic2004));
   EXPECT_EQ(outBasic2004, expected);

   boost::posix_time::ptime outExtended2004;
   EXPECT_TRUE(parseUtcTimeFromZoneString(extended2004, &outExtended2004));
   EXPECT_EQ(outExtended2004, expected);

   boost::posix_time::ptime outBasic2019;
   EXPECT_TRUE(parseUtcTimeFromZoneString(basic2019, &outBasic2019));
   EXPECT_EQ(outBasic2019, expected);

   boost::posix_time::ptime outExtended2019;
   EXPECT_TRUE(parseUtcTimeFromZoneString(extended2019, &outExtended2019));
   EXPECT_EQ(outExtended2019, expected);
}

TEST(DateTimeTest, ParsesTimeZones)
{
   using namespace rstudio::core::date_time;
   std::string zulu = "2026-01-01T00:00:00.000Z";
   std::string utcShort = "2026-01-01T00:00:00.000+00";
   std::string utcLong = "2026-01-01T00:00:00.000+0000";
   std::string utcExt = "2026-01-01T00:00:00.000+00:00";
   std::string cstShort = "2025-12-31T18:00:00.000-06";
   std::string cstLong = "2025-12-31T18:00:00.000-0600";
   std::string cstExt = "2025-12-31T18:00:00.000-06:00";

   boost::posix_time::ptime expected = boost::posix_time::from_time_t(1767225600);

   boost::posix_time::ptime outZulu;
   EXPECT_TRUE(parseUtcTimeFromZoneString(zulu, &outZulu));
   EXPECT_EQ(outZulu, expected);

   boost::posix_time::ptime outUtcShort;
   EXPECT_TRUE(parseUtcTimeFromZoneString(utcShort, &outUtcShort));
   EXPECT_EQ(outUtcShort, expected);

   boost::posix_time::ptime outUtcLong;
   EXPECT_TRUE(parseUtcTimeFromZoneString(utcLong, &outUtcLong));
   EXPECT_EQ(outUtcLong, expected);

   boost::posix_time::ptime outUtcExt;
   EXPECT_TRUE(parseUtcTimeFromZoneString(utcExt, &outUtcExt));
   EXPECT_EQ(outUtcExt, expected);

   boost::posix_time::ptime outCstShort;
   EXPECT_TRUE(parseUtcTimeFromZoneString(cstShort, &outCstShort));
   EXPECT_EQ(outCstShort, expected);

   boost::posix_time::ptime outCstLong;
   EXPECT_TRUE(parseUtcTimeFromZoneString(cstLong, &outCstLong));
   EXPECT_EQ(outCstLong, expected);

   boost::posix_time::ptime outCstExt;
   EXPECT_TRUE(parseUtcTimeFromZoneString(cstExt, &outCstExt));
   EXPECT_EQ(outCstExt, expected);
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio
