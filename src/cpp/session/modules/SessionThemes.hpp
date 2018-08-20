/*
 * SessionThemes.hpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include <string>

namespace rstudio {
namespace core {
   class Error;

namespace http {
   class Request;
   class Response;
}
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

// These constants and URI handlers need to be public so that they can be registered in
// DataViewer.cpp before the /grid_resource handler is registered or they won't be invoked (because
// their path is also a regex match for the less specific `/grid_resource` path). A better long term
// solution would be to order the set of URI handlers with a reverse lexographical order so that the
// most specific path always appears first (or use a lexographical order with a reverse search).

extern const std::string kDefaultThemeLocation;
extern const std::string kGlobalCustomThemeLocation;
extern const std::string kLocalCustomThemeLocation;

void handleDefaultThemeRequest(const core::http::Request& request,
                                     core::http::Response* pResponse);

void handleGlobalCustomThemeRequest(const core::http::Request& request,
                                          core::http::Response* pResponse);

void handleGlobalCustomThemeRequest(const core::http::Request& request,
                                          core::http::Response* pResponse);

core::Error initialize();

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
