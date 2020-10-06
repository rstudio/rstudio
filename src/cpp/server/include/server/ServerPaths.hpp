/*
 * ServerPaths.hpp
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

#ifndef SERVER_PATHS_HPP
#define SERVER_PATHS_HPP

#include <shared_core/FilePath.hpp>

#include <monitor/MonitorConstants.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerOptions.hpp>

#include <session/SessionConstants.hpp>

namespace rstudio {
namespace server {

using namespace core;

inline FilePath serverTmpDir() { return options().serverDataDir().completeChildPath(kServerTmpDir); }
inline FilePath serverLocalSocketPath() { return serverTmpDir().completeChildPath(kServerLocalSocket); }
inline FilePath monitorSocketPath() { return serverTmpDir().completeChildPath(kMonitorSocket); }
inline FilePath sessionTmpDir() { return options().serverDataDir().completeChildPath(kSessionTmpDir); }

} // namespace server
} // namespace rstudio


#endif // SERVER_PATHS_HPP

