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

void sendMetric(const std::string& metricsSocket,
                const std::string& sharedSecret,
                const metrics::Metric& metric);

void sendMultiMetric(const std::string& metricsSocket,
                     const std::string& sharedSecret,
                     const metrics::MultiMetric& metric);

void sendMetricAsync(boost::asio::io_service& ioService,
                     const std::string& metricsSocket,
                     const std::string& sharedSecret,
                     const metrics::Metric& metric);


void sendMultiMetricAsync(boost::asio::io_service& ioService,
                          const std::string& metricsSocket,
                          const std::string& sharedSecret,
                          const metrics::MultiMetric& metric);


} // namespace http
} // namespace monitor

#endif // MONITOR_HTTP_CLIENT_HPP

