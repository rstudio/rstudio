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

#include <boost/asio/io_service.hpp>

#include <core/system/System.hpp>
#include <core/LogWriter.hpp>

namespace monitor {

namespace metrics {
   class Metric;
   class MultiMetric;
}

class Client : boost::noncopyable
{
public:
   Client(const std::string& metricsSocket,
          const std::string& sharedSecret)
      : metricsSocket_(metricsSocket),
        sharedSecret_(sharedSecret)
   {
   }

   virtual ~Client() {}

public:
   virtual void logMessage(const std::string& programIdentity,
                           core::system::LogLevel level,
                           const std::string& message) = 0;

   virtual boost::shared_ptr<core::LogWriter> createLogWriter(
                                       const std::string& programIdentity) = 0;

   virtual void sendMetrics(const std::vector<metrics::Metric>& metrics) = 0;

   virtual void sendMultiMetrics(
                        const std::vector<metrics::MultiMetric>& metrics) = 0;

protected:
   const std::string& metricsSocket() const { return metricsSocket_; }
   const std::string& sharedSecret() const { return sharedSecret_; }

private:
   std::string metricsSocket_;
   std::string sharedSecret_;
};

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

   boost::shared_ptr<core::LogWriter> createLogWriter(
                                       const std::string& programIdentity);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);
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

   boost::shared_ptr<core::LogWriter> createLogWriter(
                                       const std::string& programIdentity);

   void sendMetrics(const std::vector<metrics::Metric>& metrics);

   void sendMultiMetrics(const std::vector<metrics::MultiMetric>& metrics);

private:
   boost::asio::io_service& ioService_;
};

} // namespace monitor

#endif // MONITOR_MONITOR_CLIENT_HPP

