/*
 * TestThat.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef CORE_TESTTHAT_HPP
#define CORE_TESTTHAT_HPP

# include "vendor/catch.hpp"

# define test_that(__X__, ...) TEST_CASE(__X__, __FILE__, ##__VA_ARGS__)
# define expect_true(__X__) CHECK(__X__)
# define expect_false(__X__) CHECK_FALSE(__X__)

#endif

