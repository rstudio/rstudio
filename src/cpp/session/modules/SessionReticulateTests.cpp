/*
 * SessionReticulateTests.cpp
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

#include "SessionReticulate.hpp"

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace reticulate {
namespace tests {

using TerminalAction = PythonDiscoveryState::TerminalAction;

TEST(PythonDiscoveryState, TerminalProbesSynchronouslyWhileDiscoveryIsPending)
{
   PythonDiscoveryState state;
   EXPECT_FALSE(state.resolved());
   EXPECT_EQ(state.terminalAction(), TerminalAction::ProbeSynchronously);
}

TEST(PythonDiscoveryState, DiscoveredInterpreterIsReusedByTerminals)
{
   PythonDiscoveryState state;
   state.recordAnswer("/opt/python/bin/python3");
   EXPECT_TRUE(state.resolved());
   EXPECT_EQ(state.python(), "/opt/python/bin/python3");
   EXPECT_EQ(state.terminalAction(), TerminalAction::UseRecordedAnswer);
}

TEST(PythonDiscoveryState, FailedDiscoveryIsRecordedAsNoPython)
{
   PythonDiscoveryState state;
   state.recordFailure();
   EXPECT_TRUE(state.resolved());
   EXPECT_TRUE(state.python().empty());
   EXPECT_EQ(state.terminalAction(), TerminalAction::UseRecordedAnswer);
}

TEST(PythonDiscoveryState, TimedOutDiscoveryIsRetriedInsteadOfRecorded)
{
   PythonDiscoveryState state;
   state.recordTimeout();
   EXPECT_FALSE(state.resolved());
   EXPECT_TRUE(state.python().empty());
   EXPECT_EQ(state.terminalAction(), TerminalAction::RetryInBackground);

   // a later retry can still settle the answer either way
   state.recordAnswer("/usr/bin/python3");
   EXPECT_EQ(state.terminalAction(), TerminalAction::UseRecordedAnswer);
   EXPECT_EQ(state.python(), "/usr/bin/python3");

   PythonDiscoveryState failed;
   failed.recordTimeout();
   failed.recordFailure();
   EXPECT_EQ(failed.terminalAction(), TerminalAction::UseRecordedAnswer);
   EXPECT_TRUE(failed.python().empty());
}

} // namespace tests
} // namespace reticulate
} // namespace modules
} // namespace session
} // namespace rstudio
