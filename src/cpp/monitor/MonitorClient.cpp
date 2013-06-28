/*
 * MonitorClient.cpp
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

#include <monitor/MonitorClient.hpp>

#include "MonitorClientImpl.hpp"

namespace monitor {

namespace {

class MonitorLogWriter : public core::LogWriter
{
public:
   MonitorLogWriter(const std::string& programIdentity)
      : programIdentity_(programIdentity)
   {
   }

   virtual void log(core::system::LogLevel level, const std::string& message)
   {
      log(programIdentity_, level, message);
   }

   virtual void log(const std::string& programIdentity,
                    core::system::LogLevel level,
                    const std::string& message)
   {
      client().logMessage(programIdentity, level, message);
   }

private:
   std::string programIdentity_;
};

// single global instance of the monitor client (allocate it on the heap
// and never free it so that there are no order of destruction surprises)
Client* s_pClient = NULL;

} // anonymous namespace

boost::shared_ptr<core::LogWriter> Client::createLogWriter(
                                    const std::string& programIdentity)
{
   return boost::shared_ptr<core::LogWriter>(
                                 new MonitorLogWriter(programIdentity));
}


void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& sharedSecret)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new SyncClient(metricsSocket, sharedSecret);
}

void initializeMonitorClient(const std::string& metricsSocket,
                             const std::string& sharedSecret,
                             boost::asio::io_service& ioService)
{
   BOOST_ASSERT(s_pClient == NULL);
   s_pClient = new AsyncClient(metricsSocket, sharedSecret, ioService);
}

Client& client()
{
   BOOST_ASSERT(s_pClient != NULL);
   return *s_pClient;
}

} // namespace monitor


