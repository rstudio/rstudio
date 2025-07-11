/*
 * ServerDatabaseOverlay.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#ifndef SERVER_CORE_DATABASE_UTILS_OVERLAY_HPP
#define SERVER_CORE_DATABASE_UTILS_OVERLAY_HPP

namespace rstudio {
namespace core {
   class Error;

namespace database {
   struct PostgresqlConnectionOptions;
} // namespace database
} // namespace core
namespace server_core {
namespace database {
namespace utils {
namespace overlay {

core::Error readPostgresqlOptions(core::database::PostgresqlConnectionOptions& options);

} // namespace overlay
} // namespace utils
} // namespace database
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_DATABASE_UTILS_OVERLAY_HPP
