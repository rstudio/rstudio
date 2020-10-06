/*
 * MonitorClientImpl.hpp
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

#ifndef MONITOR_MONITOR_CLIENT_IMPL_HPP
#define MONITOR_MONITOR_CLIENT_IMPL_HPP

#include <monitor/MonitorClient.hpp>

namespace rstudio {
namespace monitor {

class SyncClient : public Client
{
public:
   SyncClient(const std::string& metricsSocket,
              const std::string& auth,
              bool useSharedSecret = true)
      : Client(metricsSocket, auth, useSharedSecret)
   {
   }

   SyncClient(const std::string& tcpAddress,
              const std::string& tcpPort,
              bool useSsl,
              bool verifySslCerts,
              const std::string& prefixUri,
              const std::string& auth,
              bool useSharedSecret = false)
      : Client(tcpAddress, tcpPort, useSsl, verifySslCerts, prefixUri, auth, useSharedSecret)
   {
   }

   void logMessage(const std::string& programIdentity,
                   core::log::LogLevel level,
                   const std::string& message);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);

   void logEvent(const Event& event);

   void logConsoleAction(const audit::ConsoleAction& action);
};

class AsyncClient : public Client
{
public:
   AsyncClient(const std::string& metricsSocket,
               const std::string& auth,
               boost::asio::io_service& ioService,
               bool useSharedSecret = true)
      : Client(metricsSocket, auth, useSharedSecret),
        ioService_(ioService)
   {
   }

   AsyncClient(const std::string& tcpAddress,
               const std::string& tcpPort,
               bool useSsl,
               bool verifySslCerts,
               const std::string& prefixUri,
               const std::string& auth,
               boost::asio::io_service& ioService,
               bool useSharedSecret = false)
      : Client(tcpAddress, tcpPort, useSsl, verifySslCerts, prefixUri, auth, useSharedSecret),
        ioService_(ioService)
   {
   }

   void logMessage(const std::string& programIdentity,
                   core::log::LogLevel level,
                   const std::string& message);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);

   void logEvent(const Event& event);

   void logConsoleAction(const audit::ConsoleAction& action);

   boost::asio::io_service& ioService() const { return ioService_; }

private:
   boost::asio::io_service& ioService_;
};

} // namespace monitor
} // namespace rstudio

#endif // MONITOR_MONITOR_CLIENT_IMPL_HPP
