/*
 * SessionProjectsTests.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <boost/optional.hpp>

#include <shared_core/json/Json.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace projects {
namespace {

TEST(SessionProjectsTests, ResolveWrittenEditorThemePreservesWhenKeyOmitted)
{
   // No editor_theme key in the request -> keep the existing on-disk value.
   core::json::Object configJson;
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Cobalt");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemeEmptyStringClearsOverride)
{
   // Explicit "" from the Appearance pane's (Default) -> clear the override.
   core::json::Object configJson;
   configJson["editor_theme"] = "";
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemeNonEmptySetsValue)
{
   // Non-empty value present -> use it.
   core::json::Object configJson;
   configJson["editor_theme"] = "Monokai";
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Monokai");
}

TEST(SessionProjectsTests, ResolveWrittenEditorThemePreservesWhenValueNotString)
{
   // Present but not a string (malformed request) -> preserve existing, like a
   // missing key (and a warning is logged).
   core::json::Object configJson;
   configJson["editor_theme"] = 42;
   EXPECT_EQ(resolveWrittenEditorTheme("Cobalt", configJson), "Cobalt");
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsLocalOverrideWins)
{
   // An explicit per-project override wins over the global preference and
   // detection, in both directions.
   EXPECT_TRUE(resolveReduceRemoteFilesystemOperations(true, false, false));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(false, true, true));
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsGlobalOffShortCircuits)
{
   // No override + global preference disabled -> never reduce, regardless of
   // whether a remote filesystem was detected.
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, false, false));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, false, true));
}

TEST(SessionProjectsTests, ResolveReduceRemoteFilesystemOperationsGlobalOnGatedByDetection)
{
   // No override + global preference enabled (automatic) -> reduce only when a
   // remote filesystem was detected.
   EXPECT_TRUE(resolveReduceRemoteFilesystemOperations(boost::none, true, true));
   EXPECT_FALSE(resolveReduceRemoteFilesystemOperations(boost::none, true, false));
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideReadsBool)
{
   boost::optional<bool> enabled = parseReduceRemoteFilesystemOperationsOverride(
      "{\"reduce_remote_filesystem_operations\": true}");
   ASSERT_TRUE(enabled.has_value());
   EXPECT_TRUE(*enabled);

   boost::optional<bool> disabled = parseReduceRemoteFilesystemOperationsOverride(
      "{\"reduce_remote_filesystem_operations\": false}");
   ASSERT_TRUE(disabled.has_value());
   EXPECT_FALSE(*disabled);
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideAbsentKey)
{
   // Key omitted -> no override (fall back to the global preference).
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"some_other_pref\": true}").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideWrongType)
{
   // Present but not a boolean -> no override.
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"reduce_remote_filesystem_operations\": \"yes\"}").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideNonObject)
{
   // Valid JSON but not an object -> no override.
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride("[1, 2, 3]").has_value());
}

TEST(SessionProjectsTests, ParseReduceRemoteFilesystemOperationsOverrideMalformed)
{
   // Corrupt/truncated JSON -> no override (and the parse error is logged).
   EXPECT_FALSE(parseReduceRemoteFilesystemOperationsOverride(
                   "{\"reduce_remote_filesystem_operations\":").has_value());
}

} // anonymous namespace
} // namespace projects
} // namespace session
} // namespace rstudio
