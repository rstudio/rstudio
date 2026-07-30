/*
 * ServerSetupDbOverlay.hpp
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

#ifndef SERVER_SETUP_DB_OVERLAY_HPP
#define SERVER_SETUP_DB_OVERLAY_HPP

namespace rstudio {
namespace server {

// The subset of --setup-db's flags that apply to an overlay implementation.
// This lives in a public header because downstream builds implement
// overlay::setupDb() against it; the full SetupDbFlags struct is private to
// src/cpp/server and its remaining fields are either already consumed before
// the overlay runs or name the main database rather than anything an overlay
// provisions.
//
// Grouping the two flags in a struct rather than passing them as adjacent
// bools is deliberate: parameter names are not part of a C++ signature, so a
// transposed pair of bools would link cleanly and silently invert both
// behaviors.
struct SetupDbOverlayFlags
{
   bool showPassword = false;   // --setup-db-show-password
   bool printOnly = false;      // --setup-db-print-only
};

} // namespace server
} // namespace rstudio

#endif // SERVER_SETUP_DB_OVERLAY_HPP
