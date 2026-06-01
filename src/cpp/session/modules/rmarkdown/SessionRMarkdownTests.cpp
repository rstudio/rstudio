/*
 * SessionRMarkdownTests.cpp
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

#include "SessionRMarkdown.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {

TEST(SessionRMarkdownTest, KnownSafeRenderFunctionsDoNotRequireConfirmation)
{
   // RStudio's own defaults and derived render functions are known-safe and
   // must not prompt the user on every ordinary knit/preview.
   EXPECT_FALSE(requiresRenderConfirmation(""));
   EXPECT_FALSE(requiresRenderConfirmation("rmarkdown::render"));
   EXPECT_FALSE(requiresRenderConfirmation("rmarkdown::run"));
   EXPECT_FALSE(requiresRenderConfirmation("rmarkdown::render_site"));
   EXPECT_FALSE(requiresRenderConfirmation("blogdown::serve_site"));
   EXPECT_FALSE(requiresRenderConfirmation("bookdown::render_book"));
   EXPECT_FALSE(requiresRenderConfirmation("bookdown::preview_chapter"));
   EXPECT_FALSE(requiresRenderConfirmation("pkgdown::build_site"));
   EXPECT_FALSE(requiresRenderConfirmation("xaringan::inf_mr"));
   EXPECT_FALSE(requiresRenderConfirmation("quarto serve"));
   EXPECT_FALSE(requiresRenderConfirmation("quarto render"));
}

TEST(SessionRMarkdownTest, CustomRenderFunctionsRequireConfirmation)
{
   // Anything originating verbatim from a document's `knit:` field is treated
   // as untrusted and must be confirmed before it is evaluated.
   EXPECT_TRUE(requiresRenderConfirmation("my_custom_render"));
   EXPECT_TRUE(requiresRenderConfirmation("source('evil.R')"));
   EXPECT_TRUE(requiresRenderConfirmation("function(input, ...) system('rm -rf /')"));

   // a value that merely resembles a known-safe entry is still not an exact
   // match, so it must be confirmed
   EXPECT_TRUE(requiresRenderConfirmation("rmarkdown::render2"));
   EXPECT_TRUE(requiresRenderConfirmation(" rmarkdown::render"));
}

} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
