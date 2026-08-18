/*
 * SessionAssistantTests.cpp
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

#include "SessionAssistant.hpp"

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace tests {

using modules::assistant::agentStderrTail;

TEST(SessionAssistant, StderrTailKeepsShortTextUnchanged)
{
   EXPECT_EQ(agentStderrTail("Error: boom", 100), "Error: boom");
}

TEST(SessionAssistant, StderrTailAtLimitUnchanged)
{
   std::string text(10, 'x');
   EXPECT_EQ(agentStderrTail(text, 10), text);
}

TEST(SessionAssistant, StderrTailKeepsTailAndNotesDroppedByteCount)
{
   // Node.js prints the (minified, huge) offending source line before the
   // error and stack trace, so the tail is the part worth keeping.
   std::string text = std::string(90, 'x') + "Error: boom";
   EXPECT_EQ(agentStderrTail(text, 11),
             "[... 90 bytes truncated ...] Error: boom");
}

TEST(SessionAssistant, StderrTailDoesNotStartMidUtf8Character)
{
   // U+00E9 is 0xC3 0xA9 in UTF-8. A limit of 3 would start the tail on the
   // continuation byte 0xA9; the tail must skip forward past it so the
   // result stays valid UTF-8 (it is later embedded in JSON).
   std::string text = "aa\xC3\xA9zz";
   EXPECT_EQ(agentStderrTail(text, 3), "[... 4 bytes truncated ...] zz");
}

} // end namespace tests
} // end namespace session
} // end namespace rstudio
