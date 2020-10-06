/*
 * MonitorClient.hpp
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

#ifndef MONITOR_MONITOR_CLIENT_HPP
#define MONITOR_MONITOR_CLIENT_HPP

#include <string>

#include <boost/asio/io_service.hpp>

#include <core/system/System.hpp>

#include <monitor/audit/ConsoleAction.hpp>
#include <monitor/events/Event.hpp>
#include <monitor/metrics/Metric.hpp>

#include "MonitorConstants.hpp"

namespace rstudio {
namespace core {
namespace log {

class ILogDestination;

} // namespace log
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace monitor {

class Client : boost::noncopyable
{
protected:
   Client(const std::string& metricsSocket,
          const std::string& auth,
          bool useSharedSecret)
      : metricsSocket_(metricsSocket),
        auth_(auth),
        useSharedSecret_(useSharedSecret)
   {
   }

   Client(const std::string& tcpAddress,
          const std::string& tcpPort,
          bool useSsl,
          bool verifySslCerts,
          const std::string& prefixUri,
          const std::string& auth,
          bool useSharedSecret)
      : address_(tcpAddress),
        port_(tcpPort),
        useSsl_(useSsl),
        verifySslCerts_(verifySslCerts),
        prefixUri_(prefixUri),
        auth_(auth),
        useSharedSecret_(useSharedSecret)
   {
   }

public:
   virtual ~Client() {}

   virtual void logMessage(const std::string& programIdentity,
                           core::log::LogLevel level,
                           const std::string& message) = 0;

   static std::shared_ptr<core::log::ILogDestination> createLogDestination(core::log::LogLevel logLevel,
                                                                           const std::string& programIdentity);

   virtual void sendMetrics(const std::vector<metrics::Metric>& metrics) = 0;

   virtual void sendMultiMetrics(
                        const std::vector<metrics::MultiMetric>& metrics) = 0;

   virtual void logEvent(const Event& event) = 0;

   virtual void logConsoleAction(const audit::ConsoleAction& action) = 0;

   const std::string& metricsSocket() const { return metricsSocket_; }

   const std::string& tcpAddress() const { return address_; }
   const std::string& tcpPort() const { return port_; }
   bool useSsl() const { return useSsl_; }
   bool verifySslCerts() const { return verifySslCerts_; }
   const std::string& prefixUri() const { return prefixUri_; }

   bool useSharedSecret() const { return useSharedSecret_; }
   const std::string& auth() const { return auth_; }

private:
   // local connections
   std::string metricsSocket_;

   // remote connections
   std::string address_;
   std::string port_;
   bool useSsl_;
   bool verifySslCerts_;
   std::string prefixUri_;

   std::string auth_;
   bool useSharedSecret_;
};

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& auth,
                             bool useSharedSecret = true);

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& auth,
                             boost::asio::io_service& ioService,
                             bool useSharedSecret = true);

void initializeMonitorClient(const std::string& tcpAddress,
                             const std::string& tcpPort,
                             bool useSsl,
                             bool verifySslCerts,
                             const std::string& prefixUri,
                             const std::string& auth,
                             bool useSharedSecret = false);

void initializeMonitorClient(const std::string& tcpAddress,
                             const std::string& tcpPort,
                             bool useSsl,
                             bool verifySslCerts,
                             const std::string& prefixUri,
                             const std::string& auth,
                             boost::asio::io_service& ioService,
                             bool useSharedSecret = false);

Client& client();

} // namespace monitor
} // namespace rstudio

#endif // MONITOR_MONITOR_CLIENT_HPP

