/*
 * RDiscovery.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef R_SESSION_DISCOVERY_HPP
#define R_SESSION_DISCOVERY_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace r {
namespace session {

struct RLocations
{
   std::string homePath;
   std::string docPath;
};

core::Error discoverR(RLocations* pLocations);

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_DISCOVERY_HPP

