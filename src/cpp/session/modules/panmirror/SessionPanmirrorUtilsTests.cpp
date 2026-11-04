/*
 * SessionPanmirrorUtilsTests.cpp
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

#include <r/RExec.hpp>

#include "SessionPanmirrorUtils.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace {

const char * const kTestOption = "rstudio.panmirror_test_url";
const char * const kDefaultUrl = "https://example.org";

std::string baseUrlWithOption(const std::string& value)
{
   core::Error error = r::exec::executeString(
      std::string("options(") + kTestOption + " = " + value + ")");
   EXPECT_FALSE(error);

   std::string url = serviceBaseUrl(kTestOption, kDefaultUrl);

   error = r::exec::executeString(std::string("options(") + kTestOption + " = NULL)");
   EXPECT_FALSE(error);

   return url;
}

} // anonymous namespace

// An unset or empty option must leave lookups on the live service, not send
// them to an empty host; the e2e suite always sets the option, so only this
// test covers the fallback.
TEST(SessionPanmirrorUtilsTest, ServiceBaseUrlFallsBackToDefault)
{
   EXPECT_EQ(kDefaultUrl, baseUrlWithOption("NULL"));
   EXPECT_EQ(kDefaultUrl, baseUrlWithOption("''"));
   EXPECT_EQ(kDefaultUrl, baseUrlWithOption("'/'"));
}

TEST(SessionPanmirrorUtilsTest, ServiceBaseUrlReadsOption)
{
   EXPECT_EQ("http://127.0.0.1:8080/doi", baseUrlWithOption("'http://127.0.0.1:8080/doi'"));
}

// Callers append "/<resource>", so a trailing slash would double it.
TEST(SessionPanmirrorUtilsTest, ServiceBaseUrlDropsTrailingSlash)
{
   EXPECT_EQ("http://127.0.0.1:8080/doi", baseUrlWithOption("'http://127.0.0.1:8080/doi/'"));
   EXPECT_EQ("http://127.0.0.1:8080/doi", baseUrlWithOption("'http://127.0.0.1:8080/doi//'"));
}

} // namespace panmirror
} // namespace modules
} // namespace session
} // namespace rstudio
