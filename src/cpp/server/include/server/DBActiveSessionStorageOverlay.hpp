/*
 * DBActiveSessionStorageOverlay.hpp
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

#ifndef DB_ACTIVE_SESSION_STORAGE_OVERLAY_HPP
#define DB_ACTIVE_SESSION_STORAGE_OVERLAY_HPP

#include <string>
#include <vector>

namespace rstudio {
namespace server {
namespace storage {
namespace overlay {

// Adds any properties stored in an active_session_metadata column of the same name
// that only this build's schema has
void addColumnProperties(std::vector<std::string>* pProperties);

} // namespace overlay
} // namespace storage
} // namespace server
} // namespace rstudio

#endif // DB_ACTIVE_SESSION_STORAGE_OVERLAY_HPP
