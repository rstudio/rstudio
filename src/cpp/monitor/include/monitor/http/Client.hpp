/*
 * Client.hpp
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

#ifndef MONITOR_HTTP_CLIENT_HPP
#define MONITOR_HTTP_CLIENT_HPP

#include <string>

#include <boost/asio/io_service.hpp>

namespace monitor {

namespace metrics {
   class Metric;
   class MultiMetric;
}

namespace http {

void sendMetrics(const std::string& metricsSocket,
                 const std::string& sharedSecret,
                 const std::vector<metrics::Metric>& metrics);

void sendMultiMetrics(const std::string& metricsSocket,
                      const std::string& sharedSecret,
                      const std::vector<metrics::MultiMetric>& metrics);

void sendMetricsAsync(boost::asio::io_service& ioService,
                      const std::string& metricsSocket,
                      const std::string& sharedSecret,
                      const std::vector<metrics::Metric>& metrics);


void sendMultiMetricsAsync(boost::asio::io_service& ioService,
                           const std::string& metricsSocket,
                           const std::string& sharedSecret,
                           const std::vector<metrics::MultiMetric>& metrics);


} // namespace http
} // namespace monitor

#endif // MONITOR_HTTP_CLIENT_HPP

