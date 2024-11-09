/*
 * SessionCRANOverlay.hpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#ifndef SESSION_CRAN_OVERLAY
#define SESSION_CRAN_OVERLAY

#include <string>

namespace rstudio {
namespace session {
namespace overlay {

/**
 * @breif Returns the default CRAN mirror URL. For use when no other mirror is specified.
 */
std::string getDefaultCRANMirror();

/**
 * @brief Determines if the provided cranMirror URL requires further processing, and if so,
 * returns the processed URL.
 */
std::string mapCRANMirrorUrl(const std::string& cranMirror);

} // namespace overlay
} // namespace session
} // namespace rstudio

#endif // SESSION_CRAN_OVERLAY
