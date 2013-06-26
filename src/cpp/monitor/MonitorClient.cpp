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

namespace monitor {

namespace {

class MonitorLogWriter : public core::LogWriter
{
public:
   MonitorLogWriter(const std::string& programIdentity,
                    Client* pClient)
      : programIdentity_(programIdentity),
        pClient_(pClient)
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
      pClient_->logMessage(programIdentity, level, message);
   }

private:
   std::string programIdentity_;
   Client* pClient_;
};

} // anonymous namespace


boost::shared_ptr<core::LogWriter> SyncClient::createLogWriter(
                                           const std::string& programIdentity)
{
   return boost::shared_ptr<core::LogWriter>(
                        new MonitorLogWriter(programIdentity, this));
}

boost::shared_ptr<core::LogWriter> AsyncClient::createLogWriter(
                                           const std::string& programIdentity)
{
   return boost::shared_ptr<core::LogWriter>(
                        new MonitorLogWriter(programIdentity, this));

}

} // namespace monitor


