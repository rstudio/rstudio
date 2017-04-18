/*
 * PresentationState.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_PRESENTATION_STATE_HPP
#define SESSION_PRESENTATION_STATE_HPP

#include <core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {
namespace state {


void init(const core::FilePath& filePath,
          const std::string& caption = "Presentation",
          bool isTutorial = false);
void setSlideIndex(int index);
void setCaption(const std::string& caption);


bool isActive();

bool isTutorial();

core::FilePath filePath();

core::FilePath directory();

core::FilePath viewInBrowserPath();

void clear();


core::json::Value asJson();



core::Error initialize();
core::Error initializeOverlay();


} // namespace state
} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_STATE_HPP
