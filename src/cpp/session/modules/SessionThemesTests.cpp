/*
 * SessionThemesTests.cpp
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

#include "SessionThemes.hpp"

#include <set>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace themes {
namespace {

TEST(SessionThemesTests, ChooseAppliedThemeNamePrefersEffectiveWhenInstalled)
{
   std::set<std::string> installed = { "Cobalt", "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "Textmate (default)", installed,
                                    "Textmate (default)"),
             "Cobalt");
}

TEST(SessionThemesTests, ChooseAppliedThemeNameFallsBackToGlobalWhenEffectiveMissing)
{
   std::set<std::string> installed = { "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "Textmate (default)", installed,
                                    "Textmate (default)"),
             "Textmate (default)");
}

TEST(SessionThemesTests, ChooseAppliedThemeNameFallsBackToDefaultWhenBothMissing)
{
   std::set<std::string> installed = { "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "AlsoMissing", installed,
                                    "Textmate (default)"),
             "Textmate (default)");
}

TEST(SessionThemesTests, ResolveGlobalThemeNameChecksLayersBeyondUser)
{
   // user layer empty, system layer set -> must return the system value
   auto readLayer = [](const std::string& layer) -> boost::optional<std::string> {
      if (layer == "system") return std::string("Ambiance");
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "Ambiance");
}

TEST(SessionThemesTests, ResolveGlobalThemeNamePrefersUserOverSystem)
{
   auto readLayer = [](const std::string& layer) -> boost::optional<std::string> {
      if (layer == "user") return std::string("Cobalt");
      if (layer == "system") return std::string("Ambiance");
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "Cobalt");
}

TEST(SessionThemesTests, ResolveGlobalThemeNameEmptyWhenNonePresent)
{
   auto readLayer = [](const std::string&) -> boost::optional<std::string> {
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "");
}

} // anonymous namespace
} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
