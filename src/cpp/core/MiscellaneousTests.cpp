/*
 * MiscellaneousTests.cpp
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

#include <tests/TestThat.hpp>

#include <vector>
#include <string>
#include <iostream>

#include <core/Algorithm.hpp>
#include <core/RegexUtils.hpp>
#include <core/collection/LruCache.hpp>
#include <core/collection/Position.hpp>
#include <core/http/Request.hpp>
#include <shared_core/json/Json.hpp>

#include <core/system/Types.hpp>

namespace rstudio {
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

test_context("Position")
{
   test_that("Positions are compared correctly")
   {
      expect_true(Position(0, 0) == Position(0, 0));
      expect_true(Position(0, 0) <  Position(0, 1));
      expect_true(Position(0, 0) <  Position(1, 0));
      expect_true(Position(2, 2) <  Position(2, 4));
   }
   
}

test_context("Splitting")
{
   test_that("core::algorithm::split handles multi-character delimiters")
   {
      std::string contents = "foo::bar::baz";
      std::vector<std::string> splat = core::algorithm::split(contents, "::");
      expect_true(splat.size() == 3);
      if (splat.size() == 3)
      {
         expect_true(splat[0] == "foo");
         expect_true(splat[1] == "bar");
         expect_true(splat[2] == "baz");
      }
   }
}

test_context("Regular Expressions")
{
   test_that("Exceptions caused by regular expression complexity are caught")
   {
      boost::regex pattern("(\\w*|\\w*)*@");
      std::string haystack =
            "abcdefghijklmnopqrstuvwxyz"
            "abcdefghijklmnopqrstuvwxyz"
            "|@";
      
      // boost-level APIs will throw an exception
      {
         REQUIRE_THROWS(
                  boost::regex_match(
                     haystack.begin(),
                     haystack.end(),
                     pattern));
      }
      
      {
         REQUIRE_THROWS(
                  boost::regex_search(
                     haystack.begin(),
                     haystack.end(),
                     pattern));
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
         expect_false(result);
      }
      
      {
         SuppressOutputScope scope;
         bool result = core::regex_utils::search(
                  haystack.begin(),
                  haystack.end(),
                  pattern);

         // although in theory the above regular expression matches
         // we should instead see a report regarding complexity overload
         expect_false(result);
      }
      
   }
}

test_context("HttpRequest")
{
   test_that("Accept encoding works properly")
   {
      std::string encodingStr = "gzip,deflate,br";
      std::string encodingStr2 = "gzip, deflate, br";

      core::http::Request request;
      request.setHeader("Accept-Encoding", encodingStr);

      expect_true(request.acceptsEncoding("gzip"));
      expect_true(request.acceptsEncoding("deflate"));
      expect_true(request.acceptsEncoding("br"));
      expect_false(request.acceptsEncoding("gzip,deflate,br"));

      request.setHeader("Accept-Encoding", encodingStr2);
      expect_true(request.acceptsEncoding("gzip"));
      expect_true(request.acceptsEncoding("deflate"));
      expect_true(request.acceptsEncoding("br"));
      expect_false(request.acceptsEncoding("gzip,deflate,br"));
   }
}

test_context("LruCache")
{
   test_that("Can update the same value multiple times")
   {
      LruCache<std::string, int> cache(10);
      for (int i = 0; i < 1000; ++i)
      {
         cache.insert("val", i);
      }

      expect_true(cache.size() == 1);

      int val;
      expect_true(cache.get("val", &val));
      expect_true(val == 999);
   }

   test_that("Cache never grows past max size")
   {
      LruCache<int, int> cache(100);
      for (int i = 0; i < 1000; ++i)
      {
         cache.insert(i, i);
      }

      expect_true(cache.size() == 100);

      int val;
      expect_true(cache.get(999, &val));
      expect_true(val == 999);

      expect_false(cache.get(100, &val));
   }

   test_that("Explicit cache removal works")
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
      expect_false(cache.get(0, &val));

      expect_true(cache.get(50, &val));
      expect_true(val == 50);

      cache.remove(50);

      expect_false(cache.get(50, &val));
      expect_true(cache.size() == 99);
   }

   test_that("Reads update the access time of the cache entry")
   {
      LruCache<int, int> cache(100);
      cache.insert(5000, 1);

      int val;
      for (int i = 0; i < 1000; ++i)
      {
         expect_true(cache.get(5000, &val));
         expect_true(val == 1);

         cache.insert(i, i);
      }

      expect_true(cache.size() == 100);
      expect_true(cache.get(5000, &val));
      expect_false(cache.get(900, &val));
   }

   test_that("No values when explicitly removed")
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

      expect_true(cache.size() == 0);
   }
}

test_context("Options")
{
   test_that("Options are properly serialized/deserialized")
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
         expect_true(options[i].first == options2[i].first);
         expect_true(options[i].second == options2[i].second);
      }
   }
}

} // namespace unit_tests
} // namespace rstudio
