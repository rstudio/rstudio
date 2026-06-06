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

} // anonymous namespace
} // namespace projects
} // namespace session
} // namespace rstudio
