/*
 * ServerMetrics.cpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

#include "ServerMetrics.hpp"

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncServer.hpp>

#include <core/Log.hpp>

#include "server-config.h"

namespace rstudio {
namespace server {
namespace metrics {

std::string prefix()
{
   return "rserver_";
}

void sessionLaunch(const std::string& sessionEditor)
{
}

void sessionStart(const std::string& sessionEditor, const std::string& username, boost::posix_time::time_duration startDuration)
{
}

void sessionStartConnect(const std::string& sessionEditor, const std::string& username, boost::posix_time::time_duration startDuration)
{
}

void setActiveUserSessionCount(int count)
{
}

boost::shared_ptr<core::http::AsyncServerStatsProvider> statsProvider()
{
   return boost::shared_ptr<core::http::AsyncServerStatsProvider>();
}

void handle(const core::http::Request& request, core::http::Response* pResponse)
{
   pResponse->setBody("<html><body>No metrics available</body></html>");
   pResponse->setStatusCode(200);
}

void initialize()
{
}

} // namespace metrics
} // namespace server
} // namespace rstudio
