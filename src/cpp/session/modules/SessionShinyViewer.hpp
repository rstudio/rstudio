/*
 * SessionShinyViewer.hpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#ifndef SESSION_SHINY_VIEWER_HPP
#define SESSION_SHINY_VIEWER_HPP

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace shiny_viewer {

const int SHINY_VIEWER_USER = 0;
const int SHINY_VIEWER_NONE = 1;
const int SHINY_VIEWER_PANE = 2;
const int SHINY_VIEWER_WINDOW = 3;
const int SHINY_VIEWER_BROWSER = 4;

core::Error initialize();
                       
} // namespace shiny_viewer
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SHINY_VIEWER_HPP
