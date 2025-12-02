/*
 * SessionRCompletionsTests.cpp
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

#include "SessionRCompletions.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

TEST(SessionRCompletionsTest, FinishExpressionWorks) {
   EXPECT_EQ("(abc)", finishExpression("(abc"));
   EXPECT_EQ(L"(abc)", finishExpression(L"(abc"));

   // https://github.com/rstudio/rstudio/issues/14625
}

TEST(SessionRCompletionsTest, FinishExpressionAcceptsNonAsciiInputs) {
   EXPECT_EQ(L"(你好)", finishExpression(L"(你好"));
   EXPECT_EQ(L"(こんにちは)", finishExpression(L"(こんにちは"));
}

} // namespace r_packages
} // namespace modules
} // namespace session
} // namespace rstudio
