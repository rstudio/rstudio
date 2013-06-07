/*
 * ServerMonitorClient.hpp
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

#ifndef SERVER_MONITOR_CLIENT_HPP
#define SERVER_MONITOR_CLIENT_HPP

#include <vector>

#include <monitor/metrics/Metric.hpp>

namespace server {

void sendMetrics(const std::vector<monitor::metrics::Metric>& metrics);

inline void sendMetric(const monitor::metrics::Metric& metric)
{
   std::vector<monitor::metrics::Metric> metrics;
   metrics.push_back(metric);
   sendMetrics(metrics);
}

void sendMetrics(const std::vector<monitor::metrics::MultiMetric>& metrics);

inline void sendMetric(const monitor::metrics::MultiMetric& metric)
{
   std::vector<monitor::metrics::MultiMetric> metrics;
   metrics.push_back(metric);
   sendMetrics(metrics);
}


} // namespace server

#endif // SERVER_MONITOR_CLIENT_HPP

