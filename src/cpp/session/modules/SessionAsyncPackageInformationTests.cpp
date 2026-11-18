/*
 * SessionAsyncPackageInformationTests.cpp
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

#include "SessionAsyncPackageInformation.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

TEST(SessionAsyncPackageInformationTest, CommandQuotesEachPackage)
{
   EXPECT_EQ(
            ".rs.getPackageInformation('utils','data.table');",
            packageInformationCommand({"utils", "data.table"}));
}

TEST(SessionAsyncPackageInformationTest, CommandDropsInvalidPackageNames)
{
   EXPECT_EQ(
            ".rs.getPackageInformation('stats');",
            packageInformationCommand({
               "utils', 'stats",
               "stats",
               "my pkg(1)",
               "a\\'b",
               "a\nb"}));
}

TEST(SessionAsyncPackageInformationTest, CommandIsEmptyWithoutValidPackages)
{
   EXPECT_EQ("", packageInformationCommand({}));
   EXPECT_EQ("", packageInformationCommand({"utils', 'stats"}));
}

} // namespace r_packages
} // namespace modules
} // namespace session
} // namespace rstudio
