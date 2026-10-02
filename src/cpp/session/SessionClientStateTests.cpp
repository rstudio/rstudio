/*
 * SessionClientStateTests.cpp
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

#include <r/session/RClientState.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/tests/ScratchDir.hpp>

using namespace rstudio::core;
using rstudio::core::tests::scratchDir;

namespace rstudio {
namespace r {
namespace session {
namespace tests {

// Each commit replaces the state files in place of wiping the directories
// first, then removes the files that are no longer part of the state.
TEST(ClientStateTest, CommitRemovesStateThatIsNoLongerPresent)
{
   FilePath stateDir = scratchDir();
   FilePath projectStateDir = scratchDir();

   ClientState& state = clientState();
   state.clear();
   state.putPersistent("layout", "width", json::Value(100));
   state.putTemporary("editor", "cursor", json::Value(1));
   state.putProjectPersistent("build", "target", json::Value(std::string("all")));
   ASSERT_FALSE(state.commit(ClientStateCommitAll, stateDir, projectStateDir));

   EXPECT_TRUE(stateDir.completePath("layout.persistent").exists());
   EXPECT_TRUE(stateDir.completePath("editor.temporary").exists());
   EXPECT_TRUE(projectStateDir.completePath("build.pper").exists());

   // left over from an earlier version of the state
   ASSERT_FALSE(writeStringToFile(stateDir.completePath("old.persistent"), "{}"));

   // committing only the persistent state drops the temporary state
   ASSERT_FALSE(state.commit(ClientStateCommitPersistentOnly, stateDir, projectStateDir));

   EXPECT_TRUE(stateDir.completePath("layout.persistent").exists());
   EXPECT_TRUE(projectStateDir.completePath("build.pper").exists());
   EXPECT_FALSE(stateDir.completePath("editor.temporary").exists());
   EXPECT_FALSE(stateDir.completePath("old.persistent").exists());

   // and what was committed restores
   state.clear();
   ASSERT_FALSE(state.restore(stateDir, projectStateDir));
   EXPECT_EQ(100, state.getPersistent("layout", "width").getInt());
   EXPECT_EQ("all", state.getProjectPersistent("build", "target").getString());

   state.clear();
   stateDir.remove();
   projectStateDir.remove();
}

} // namespace tests
} // namespace session
} // namespace r
} // namespace rstudio
