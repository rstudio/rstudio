/*
 * VersionTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/Version.hpp>

namespace rstudio {
namespace core {
namespace {

void compareVersionLessThan(const std::string& lhs, const std::string& rhs)
{
   Version vl(lhs);
   Version vr(rhs);
   
   expect_true(vl <  vr);
   expect_true(vl <= vr);
   expect_true(vl != vr);
   expect_true(vr >  vl);
   expect_true(vr >= vl);
}

void compareVersionEqual(const std::string& lhs, const std::string& rhs)
{
   expect_true(Version(lhs) == Version(rhs));
}

context("Version")
{
   test_that("Various versions are compared correctly")
   {
      compareVersionLessThan("3.2.0", "3.3.0");
      compareVersionLessThan("3.2", "3.3.1");
      compareVersionLessThan("3", "3.0.1");
      compareVersionLessThan("3.3.0", "3.3.1");
      
      compareVersionEqual("3.0.0", "3.0.0.0");
      compareVersionEqual("3", "3.0-0");
   }
}

} // end anonymous namespace
} // end namespace core
} // end namespace rstudio
