/*
 * MonitorClient.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <boost/asio/io_service.hpp>

#include <monitor/MonitorClient.hpp>

#include <shared_core/ILogDestination.hpp>

#include "MonitorClientImpl.hpp"

namespace rstudio {
namespace monitor {

namespace {

class MonitorLogDestination : public core::log::ILogDestination
{
public:
   MonitorLogDestination(const std::string& id, core::log::LogLevel logLevel, const std::string& programIdentity) :
      ILogDestination(id, logLevel, core::log::LogMessageFormatType::PRETTY, false),
      programIdentity_(programIdentity)
   {
   }

   void refresh(const core::log::RefreshParams&) override
   {
      // Nothing to do.
   }

   void writeLog(core::log::LogLevel logLevel, const std::string& message) override
   {
      // Don't log messages which are more detailed than the configured maximum.
      if (logLevel > m_logLevel)
         return;

      client().logMessage(programIdentity_, logLevel, message);
   }

private:
   std::string programIdentity_;
};

// single global instance of the monitor client (allocate it on the heap
// and never free it so that there are no order of destruction surprises)
Client* s_pClient = NULL;

} // anonymous namespace

std::shared_ptr<core::log::ILogDestination> Client::createLogDestination(
                                    const std::string& id,
                                    core::log::LogLevel logLevel,
                                    const std::string& programIdentity)
{
   return std::shared_ptr<core::log::ILogDestination>(new MonitorLogDestination(id, logLevel, programIdentity));
}

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& auth,
                             bool useSharedSecret)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new SyncClient(metricsSocket, auth, useSharedSecret);
}

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& auth,
                             boost::asio::io_service& ioService,
                             bool useSharedSecret)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new AsyncClient(metricsSocket, auth, ioService, useSharedSecret);
}

void initializeMonitorClient(const std::string& tcpAddress,
                             const std::string& tcpPort,
                             bool useSsl,
                             bool verifySslCerts,
                             const std::string& prefixUri,
                             const std::string& auth,
                             bool useSharedSecret)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new SyncClient(tcpAddress, tcpPort, useSsl, verifySslCerts, prefixUri, auth, useSharedSecret);
}

void initializeMonitorClient(const std::string& tcpAddress,
                             const std::string& tcpPort,
                             bool useSsl,
                             bool verifySslCerts,
                             const std::string& prefixUri,
                             const std::string& auth,
                             boost::asio::io_service& ioService,
                             bool useSharedSecret)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new AsyncClient(tcpAddress, tcpPort, useSsl, verifySslCerts, prefixUri, auth, ioService, useSharedSecret);
}

Client& client()
{
   BOOST_ASSERT(s_pClient != NULL);
   return *s_pClient;
}

} // namespace monitor
} // namespace rstudio


