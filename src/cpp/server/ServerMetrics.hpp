/*
 * ServerMetrics.hpp
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

#ifndef SERVER_METRICS_HPP
#define SERVER_METRICS_HPP

#include <boost/shared_ptr.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace core {
namespace http {

class Request;
class Response;
class AsyncServerStatsProvider;

} // namespace http
} // namespace core

namespace server {
namespace metrics {

constexpr const char* kEditorRStudio = "rstudio-pro";

void handle(const core::http::Request& request, core::http::Response* pResponse);

std::string prefix();

void initialize();

boost::shared_ptr<core::http::AsyncServerStatsProvider> statsProvider();

void sessionLaunch(const std::string& editor);

void sessionStart(const std::string& editor, const std::string& username, boost::posix_time::time_duration startDuration);

void sessionStartConnect(const std::string& editor, const std::string& username, boost::posix_time::time_duration startDuration);

void setActiveUserSessionCount(int count);

} // namespace metrics
} // namespace server
} // namespace rstudio

#endif // SERVER_METRICS_HPP

