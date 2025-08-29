/*
 * VersionTests.cpp
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

#include <core/Version.hpp>

namespace rstudio {
namespace core {
namespace {

void compareVersionLessThan(const std::string& lhs, const std::string& rhs)
{
   Version vl(lhs);
   Version vr(rhs);
   
   EXPECT_LT(vl, vr);
   EXPECT_LE(vl, vr);
   EXPECT_NE(vl, vr);
   EXPECT_GT(vr, vl);
   EXPECT_GE(vr, vl);
}

void compareVersionEqual(const std::string& lhs, const std::string& rhs)
{
   EXPECT_EQ(Version(lhs), Version(rhs));
}

TEST(VersionTest, VariousVersionsAreComparedCorrectly)
{
   compareVersionLessThan("3.2.0", "3.3.0");
   compareVersionLessThan("3.2", "3.3.1");
   compareVersionLessThan("3", "3.0.1");
   compareVersionLessThan("3.3.0", "3.3.1");
   
   compareVersionEqual("3.0.0", "3.0.0.0");
   compareVersionEqual("3", "3.0-0");
}

} // end anonymous namespace
} // end namespace core
} // end namespace rstudio
