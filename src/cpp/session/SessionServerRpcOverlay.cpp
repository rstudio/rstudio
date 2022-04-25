/*
 * SessionServerRpcOverlay.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <shared_core/Error.hpp>
#include <boost/asio/io_service.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace server_rpc {
namespace overlay {

bool useHttp()
{
    return false;
}

Error invokeServerRpc()
{
    // Never called because useHttp() is always false.
    return Success();
}

void invokeServerRpcAsync(const boost::asio::io_service&)
{
    // Never called because useHttp() is always false.
    // Do nothing.
}

} // namespace overlay
} // namespace server_rpc
} // namespace session
} // namespace rstudio
