/*
 * MonitorClient.hpp
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

#ifndef MONITOR_MONITOR_CLIENT_HPP
#define MONITOR_MONITOR_CLIENT_HPP

#include <string>

#include <core/system/System.hpp>
#include <core/LogWriter.hpp>

#include <monitor/audit/ConsoleAction.hpp>
#include <monitor/events/Event.hpp>
#include <monitor/metrics/Metric.hpp>

#include "MonitorConstants.hpp"

// forward declaration; boost/asio/io_service may cause errors if included more
// than once (Boost 1.50 on Win x64 only)
namespace RSTUDIO_BOOST_NAMESPACE {
namespace asio {
   class io_service;
}
}

namespace rstudio {
namespace monitor {

class Client : boost::noncopyable
{
protected:
   Client(const std::string& metricsSocket,
          const std::string& sharedSecret)
      : metricsSocket_(metricsSocket),
        sharedSecret_(sharedSecret)
   {
   }

public:
   virtual ~Client() {}

   virtual void logMessage(const std::string& programIdentity,
                           core::system::LogLevel level,
                           const std::string& message) = 0;

   boost::shared_ptr<core::LogWriter> createLogWriter(
                                       const std::string& programIdentity);

   virtual void sendMetrics(const std::vector<metrics::Metric>& metrics) = 0;

   virtual void sendMultiMetrics(
                        const std::vector<metrics::MultiMetric>& metrics) = 0;

   virtual void logEvent(const Event& event) = 0;

   virtual void logConsoleAction(const audit::ConsoleAction& action) = 0;

protected:
   const std::string& metricsSocket() const { return metricsSocket_; }
   const std::string& sharedSecret() const { return sharedSecret_; }

private:
   std::string metricsSocket_;
   std::string sharedSecret_;
};

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& sharedSecret);

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& sharedSecret,
                             boost::asio::io_service& ioService);

Client& client();

} // namespace monitor
} // namespace rstudio

#endif // MONITOR_MONITOR_CLIENT_HPP

