/*
 * SessionPPM.hpp
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

#ifndef SESSION_PPM_HPP
#define SESSION_PPM_HPP

#include <string>

namespace rstudio {
namespace core {
class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace ppm {

bool isPpmIntegrationEnabled();
bool isPpmMetadataColumnEnabled();
std::string getPpmRepositoryUrl();
std::string getPpmMetadataColumnLabel();

// Kick off an asynchronous refresh of package vulnerability data. The network
// request is performed off the main thread; results are delivered to the
// client via the kPackageVulnerabilitiesReady event. Safe to call from the
// main thread at any time -- it coalesces overlapping refreshes and is a no-op
// when PPM integration is disabled.
void refreshVulnerabilitiesAsync();

core::Error initialize();
                       
} // namespace ppm
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PPM_HPP

