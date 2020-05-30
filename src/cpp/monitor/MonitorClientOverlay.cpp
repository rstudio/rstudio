/*
 * MonitorClientOverlay.cpp
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

#include <monitor/MonitorClient.hpp>
#include "MonitorClientImpl.hpp"

namespace rstudio {
namespace monitor {

void SyncClient::logMessage(const std::string& programIdentity,
                            core::log::LogLevel level,
                            const std::string& message)
{
}

void SyncClient::sendMetrics(const std::vector<metrics::Metric>& metrics)
{
}

void SyncClient::sendMultiMetrics(
                        const std::vector<metrics::MultiMetric>& metrics)
{
}

void AsyncClient::logMessage(const std::string& programIdentity,
                             core::log::LogLevel level,
                             const std::string& message)
{
}

void AsyncClient::sendMetrics(const std::vector<metrics::Metric>& metrics)
{
}

void AsyncClient::sendMultiMetrics(
                              const std::vector<metrics::MultiMetric>& metrics)
{
}

void SyncClient::logEvent(const Event& event)
{
}

void AsyncClient::logEvent(const Event& event)
{
}

void SyncClient::logConsoleAction(const audit::ConsoleAction& action)
{
}

void AsyncClient::logConsoleAction(const audit::ConsoleAction& action)
{
}

} // namespace monitor
} // namespace rstudio


