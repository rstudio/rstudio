/*
 * Client.cpp
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

#include <monitor/http/Client.hpp>

#include <boost/asio/io_service.hpp>

#include <monitor/metrics/Metric.hpp>

using namespace core;

namespace monitor {
namespace http {

namespace {

class MonitorLogWriter : public core::LogWriter
{
public:
   MonitorLogWriter(const std::string& metricsSocket,
                    const std::string& sharedSecret,
                    const std::string& programIdentity)
      : metricsSocket_(metricsSocket),
        sharedSecret_(sharedSecret),
        programIdentity_(programIdentity)
   {
   }

   virtual void log(core::system::LogLevel level, const std::string& message)
   {
      monitor::http::log(metricsSocket_,
                         sharedSecret_,
                         programIdentity_,
                         level,
                         message);
   }

protected:
   std::string metricsSocket_;
   std::string sharedSecret_;
   std::string programIdentity_;
};

class MonitorAsyncLogWriter : public MonitorLogWriter
{
public:
   MonitorAsyncLogWriter(boost::asio::io_service& ioService,
                         const std::string& metricsSocket,
                         const std::string& sharedSecret,
                         const std::string& programIdentity)
      : MonitorLogWriter(metricsSocket, sharedSecret, programIdentity),
        ioService_(ioService)
   {
   }

   virtual void log(core::system::LogLevel level, const std::string& message)
   {
      monitor::http::logAsync(ioService_,
                              metricsSocket_,
                              sharedSecret_,
                              programIdentity_,
                              level,
                              message);
   }

private:
   boost::asio::io_service& ioService_;
};

} // anonymous namespace

boost::shared_ptr<core::LogWriter> monitorLogWriter(
                                       const std::string& metricsSocket,
                                       const std::string& sharedSecret,
                                       const std::string& programIdentity)
{
   return boost::shared_ptr<core::LogWriter>(new MonitorLogWriter(
                                                   metricsSocket,
                                                   sharedSecret,
                                                   programIdentity));
}

boost::shared_ptr<core::LogWriter> monitorAsyncLogWriter(
                                       boost::asio::io_service& ioService,
                                       const std::string& metricsSocket,
                                       const std::string& sharedSecret,
                                       const std::string& programIdentity)
{
   return boost::shared_ptr<core::LogWriter>(new MonitorAsyncLogWriter(
                                                   ioService,
                                                   metricsSocket,
                                                   sharedSecret,
                                                   programIdentity));
}

void log(const std::string& metricsSocket,
         const std::string& sharedSecret,
         const std::string& programIdentity,
         core::system::LogLevel level,
         const std::string& message)
{
}

void logAsync(boost::asio::io_service& ioService,
              const std::string& metricsSocket,
              const std::string& sharedSecret,
              const std::string& programIdentity,
              core::system::LogLevel level,
              const std::string& message)
{
}

void sendMetrics(const std::string& metricsSocket,
                 const std::string& sharedSecret,
                 const std::vector<metrics::Metric>& metrics)
{
}

void sendMultiMetrics(const std::string& metricsSocket,
                      const std::string& sharedSecret,
                      const std::vector<metrics::MultiMetric>& metrics)
{
}

void sendMetricsAsync(boost::asio::io_service& ioService,
                      const std::string& metricsSocket,
                      const std::string& sharedSecret,
                      const std::vector<metrics::Metric>& metrics)
{
}


void sendMultiMetricsAsync(boost::asio::io_service& ioService,
                           const std::string& metricsSocket,
                           const std::string& sharedSecret,
                           const std::vector<metrics::MultiMetric>& metrics)
{
}



} // namespace http
} // namespace monitor

