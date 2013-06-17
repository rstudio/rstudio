/*
 * ServerMetrics.cpp
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



#include <core/Error.hpp>
#include <core/PeriodicCommand.hpp>

#include <monitor/metrics/Metric.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerScheduler.hpp>

#include "ServerSessionManager.hpp"

using namespace core;

namespace server {
namespace metrics {

namespace {

bool sendMetrics()
{



   return true;
}

} // anonymous namespace

Error initialize()
{
   boost::posix_time::time_duration monitorInterval =
       boost::posix_time::seconds(server::options().monitorIntervalSeconds());

   boost::shared_ptr<ScheduledCommand> pMetricsCommand(
                  new PeriodicCommand(monitorInterval, sendMetrics, false));

   server::scheduler::addCommand(pMetricsCommand);

   return Success();
}

} // namespace metrics
} // namespace server

