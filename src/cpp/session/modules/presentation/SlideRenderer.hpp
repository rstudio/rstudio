/*
 * SlideRenderer.hpp
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

#ifndef SESSION_PRESENTATION_SLIDE_RENDERER_HPP
#define SESSION_PRESENTATION_SLIDE_RENDERER_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
} // anonymous namespace
}

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

class SlideDeck;

core::Error renderSlides(const SlideDeck& slideDeck,
                         std::string* pSlidesHead,
                         std::string* pSlides,
                         std::string* pRevealConfig,
                         std::string* pInitActions,
                         std::string* pSlideActions);


} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_SLIDE_RENDERER_HPP
