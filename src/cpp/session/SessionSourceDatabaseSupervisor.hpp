/*
 * SessionSourceDatabaseSupervisor.hpp
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

#ifndef SESSION_SOURCE_DATABASE_SUPERVISOR_HPP
#define SESSION_SOURCE_DATABASE_SUPERVISOR_HPP

namespace core {
   class Error;
   class FilePath;
}

namespace session {
namespace source_database {
namespace supervisor {

core::Error attachToSourceDatabase(core::FilePath* pSessionDir);

core::Error detachFromSourceDatabase();

} // namespace supervisor
} // namespace source_database
} // namespace session


#endif // SESSION_SOURCE_DATABASE_SUPERVISOR_HPP
