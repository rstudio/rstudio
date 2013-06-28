/*
 * MonitorClientImpl.hpp
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

#ifndef MONITOR_MONITOR_CLIENT_IMPL_HPP
#define MONITOR_MONITOR_CLIENT_IMPL_HPP

#include <monitor/MonitorClient.hpp>

namespace monitor {

class SyncClient : public Client
{
public:
   SyncClient(const std::string& metricsSocket,
              const std::string& sharedSecret)
      : Client(metricsSocket, sharedSecret)
   {
   }

   void logMessage(const std::string& programIdentity,
                   core::system::LogLevel level,
                   const std::string& message);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);

   void logEvent(const Event& event);
};

class AsyncClient : public Client
{
public:
   AsyncClient(const std::string& metricsSocket,
               const std::string& sharedSecret,
               boost::asio::io_service& ioService)
      : Client(metricsSocket, sharedSecret),
        ioService_(ioService)
   {
   }

   void logMessage(const std::string& programIdentity,
                   core::system::LogLevel level,
                   const std::string& message);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);

   void logEvent(const Event& event);

protected:
   boost::asio::io_service& ioService() { return ioService_; }

private:
   boost::asio::io_service& ioService_;
};

} // namespace monitor

#endif // MONITOR_MONITOR_CLIENT_IMPL_HPP
