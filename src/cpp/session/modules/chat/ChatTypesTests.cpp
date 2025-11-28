/*
 * ChatTypesTests.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatTypes.hpp"

#include <tests/TestThat.hpp>

using namespace rstudio::session::modules::chat::types;

test_context("SemanticVersion")
{
   test_that("Parse valid full versions (major.minor.patch)")
   {
      SemanticVersion v;

      expect_true(v.parse("1.2.3"));
      expect_equal(v.major, 1);
      expect_equal(v.minor, 2);
      expect_equal(v.patch, 3);
   }

   test_that("Parse versions with v prefix")
   {
      SemanticVersion v;

      expect_true(v.parse("v2.1.0"));
      expect_equal(v.major, 2);
      expect_equal(v.minor, 1);
      expect_equal(v.patch, 0);
   }

   test_that("Parse partial versions (major only, major.minor)")
   {
      SemanticVersion v1;
      expect_true(v1.parse("3"));
      expect_equal(v1.major, 3);
      expect_equal(v1.minor, 0);
      expect_equal(v1.patch, 0);

      SemanticVersion v2;
      expect_true(v2.parse("2.5"));
      expect_equal(v2.major, 2);
      expect_equal(v2.minor, 5);
      expect_equal(v2.patch, 0);
   }

   test_that("Reject invalid versions")
   {
      SemanticVersion v;

      expect_false(v.parse(""));           // Empty string
      expect_false(v.parse("abc"));        // Non-numeric
      expect_false(v.parse("1.a.3"));      // Non-numeric minor
      expect_false(v.parse("1.2.x"));      // Non-numeric patch
      expect_false(v.parse("-1.2.3"));     // Negative major
      expect_false(v.parse("1.-2.3"));     // Negative minor
      expect_false(v.parse("1.2.-3"));     // Negative patch
   }

   test_that("Compare major versions")
   {
      SemanticVersion v1, v2;

      v1.parse("2.0.0");
      v2.parse("1.9.9");
      expect_true(v1 > v2);

      v1.parse("1.0.0");
      v2.parse("2.0.0");
      expect_false(v1 > v2);
   }

   test_that("Compare minor versions")
   {
      SemanticVersion v1, v2;

      v1.parse("1.5.0");
      v2.parse("1.4.9");
      expect_true(v1 > v2);

      v1.parse("1.3.0");
      v2.parse("1.4.0");
      expect_false(v1 > v2);
   }

   test_that("Compare patch versions")
   {
      SemanticVersion v1, v2;

      v1.parse("1.2.5");
      v2.parse("1.2.4");
      expect_true(v1 > v2);

      v1.parse("1.2.3");
      v2.parse("1.2.4");
      expect_false(v1 > v2);
   }

   test_that("Equal versions are not greater than each other")
   {
      SemanticVersion v1, v2;

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_false(v1 > v2);
      expect_false(v2 > v1);
   }

   test_that("Less-than operator works correctly")
   {
      SemanticVersion v1, v2;

      v1.parse("1.2.3");
      v2.parse("2.0.0");
      expect_true(v1 < v2);
      expect_false(v2 < v1);

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_false(v1 < v2);
   }

   test_that("Greater-than-or-equal operator works correctly")
   {
      SemanticVersion v1, v2;

      v1.parse("2.0.0");
      v2.parse("1.9.9");
      expect_true(v1 >= v2);

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_true(v1 >= v2);

      v1.parse("1.0.0");
      v2.parse("2.0.0");
      expect_false(v1 >= v2);
   }

   test_that("Less-than-or-equal operator works correctly")
   {
      SemanticVersion v1, v2;

      v1.parse("1.0.0");
      v2.parse("2.0.0");
      expect_true(v1 <= v2);

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_true(v1 <= v2);

      v1.parse("2.0.0");
      v2.parse("1.9.9");
      expect_false(v1 <= v2);
   }

   test_that("Equality operator works correctly")
   {
      SemanticVersion v1, v2;

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_true(v1 == v2);

      v1.parse("1.2.3");
      v2.parse("1.2.4");
      expect_false(v1 == v2);

      v1.parse("1.2.3");
      v2.parse("1.3.3");
      expect_false(v1 == v2);

      v1.parse("1.2.3");
      v2.parse("2.2.3");
      expect_false(v1 == v2);
   }

   test_that("Inequality operator works correctly")
   {
      SemanticVersion v1, v2;

      v1.parse("1.2.3");
      v2.parse("1.2.4");
      expect_true(v1 != v2);

      v1.parse("1.2.3");
      v2.parse("1.2.3");
      expect_false(v1 != v2);
   }
}
