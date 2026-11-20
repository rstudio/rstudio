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

#include <boost/algorithm/string/replace.hpp>
#include <boost/make_shared.hpp>

#include <core/json/JsonRpc.hpp>

#include "NotebookDocQueue.hpp"
#include "NotebookQueueUnit.hpp"
#include "SessionRMarkdown.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {

using namespace rstudio::core;

TEST(SessionRMarkdownTest, NotebookQueuesRefuseDocumentIdsWithANul)
{
   // a queue or unit reads its document's files by ID, and an ID with a nul
   // would be read up to it, naming another document (#19697). a json::Value
   // assigned from a std::string stops at a nul, so the JSON is parsed
   const std::string unitTemplate = R"({
      "code": "1", "doc_id": "%DOC_ID%", "chunk_id": "%CHUNK_ID%", "doc_type": "%DOC_TYPE%",
      "completed": [], "pending": [], "exec_mode": 0, "exec_scope": 0
   })";

   auto unitText = [&](const std::string& docId, const std::string& docType, const std::string& chunkId)
   {
      std::string text = boost::algorithm::replace_all_copy(unitTemplate, "%DOC_ID%", docId);
      boost::algorithm::replace_all(text, "%DOC_TYPE%", docType);
      boost::algorithm::replace_all(text, "%CHUNK_ID%", chunkId);
      return text;
   };

   auto readUnit = [&](const std::string& docId, const std::string& docType, const std::string& chunkId = "c1")
   {
      json::Value value;
      EXPECT_FALSE(value.parse(unitText(docId, docType, chunkId)));

      auto pUnit = boost::make_shared<notebook::NotebookQueueUnit>();
      return notebook::NotebookQueueUnit::fromJson(value.getObject(), &pUnit);
   };

   EXPECT_FALSE(readUnit("ABCD1234", "rmarkdown"));
   EXPECT_EQ(static_cast<int>(json::errc::ParamInvalid), readUnit("ABCD1234\\u0000x", "rmarkdown").getCode());
   EXPECT_EQ(static_cast<int>(json::errc::ParamInvalid), readUnit("ABCD1234", "rmark\\u0000down").getCode());
   EXPECT_EQ(static_cast<int>(json::errc::ParamInvalid), readUnit("ABCD1234", "rmarkdown", "c1\\u0000x").getCode());

   const std::string queueTemplate = R"({
      "doc_id": "%DOC_ID%", "job_desc": "", "working_dir": "", "commit_mode": 0,
      "pixel_width": 0, "char_width": 0, "units": [%UNITS%], "max_units": 0
   })";

   auto readQueue = [&](const std::string& docId, const std::string& units = std::string())
   {
      std::string text = boost::algorithm::replace_all_copy(queueTemplate, "%DOC_ID%", docId);
      boost::algorithm::replace_all(text, "%UNITS%", units);
      json::Value value;
      EXPECT_FALSE(value.parse(text));

      boost::shared_ptr<notebook::NotebookDocQueue> pQueue;
      return notebook::NotebookDocQueue::fromJson(value.getObject(), &pQueue);
   };

   EXPECT_FALSE(readQueue("ABCD1234"));
   EXPECT_EQ(static_cast<int>(json::errc::ParamInvalid), readQueue("ABCD1234\\u0000x").getCode());

   // a unit the queue can't hold refuses the whole queue, rather than leave
   // it short of the units it counts (or with none, blocking those after it)
   std::string goodUnit = unitText("ABCD1234", "rmarkdown", "c1");
   std::string badUnit = unitText("ABCD1234", "rmarkdown", "c2\\u0000x");
   EXPECT_FALSE(readQueue("ABCD1234", goodUnit));
   EXPECT_EQ(static_cast<int>(json::errc::ParamInvalid), readQueue("ABCD1234", goodUnit + "," + badUnit).getCode());
}

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
