/*
 * SessionActiveSessionsStorage.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_ACTIVE_SESSIONS_STORAGE_HPP
#define SESSION_ACTIVE_SESSIONS_STORAGE_HPP

#include <shared_core/Error.hpp>
#include <core/r_util/RActiveSessionsStorage.hpp>

namespace rstudio {
namespace session {
namespace storage {

core::r_util::InvokeRpc getSessionRpcInvoker();

core::Error activeSessionsStorage(std::shared_ptr<core::r_util::IActiveSessionsStorage>* pStorage);

} // namespace storage
} // namespace session
} // namespace rstudio

#endif
