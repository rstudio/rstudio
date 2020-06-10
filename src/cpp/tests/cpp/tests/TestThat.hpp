/*
 * TestThat.hpp
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

#ifndef TESTS_TESTTHAT_HPP
#define TESTS_TESTTHAT_HPP

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

# include "vendor/catch.hpp"

# ifndef RSTUDIO_NO_TESTTHAT_ALIASES

#  define test_context(__X__, ...) TEST_CASE(__X__, __FILE__)
#  define test_that SECTION
#  define expect_true(x) CHECK((x))
#  define expect_false(x) CHECK_FALSE((x))
#  define expect_equal(x,y) REQUIRE((x) == (y))

# endif

#else

# ifndef RSTUDIO_NO_TESTTHAT_ALIASES

#  define test_context(__X__, ...) void RSTUDIO_UNIT_TESTS_DISABLED_##__LINE__()
#  define test_that(__X__) if (false)
#  define expect_true(__X__)
#  define expect_false(__X__)

# endif

#endif

#endif

