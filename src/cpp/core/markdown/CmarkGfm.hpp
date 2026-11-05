/*
 * CmarkGfm.hpp
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

#ifndef CORE_MARKDOWN_CMARK_GFM_HPP
#define CORE_MARKDOWN_CMARK_GFM_HPP

#include <core/markdown/Markdown.hpp>

namespace rstudio {
namespace core {
namespace markdown {

// Render after RStudio's metadata, math and html-preserve preprocessing.
Error renderCmarkGfm(const std::string& input,
                     const Extensions& extensions,
                     const HTMLOptions& options,
                     std::string* pOutput);

} // namespace markdown
} // namespace core
} // namespace rstudio

#endif
