/*
 * SessionOverlay.cpp
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

#include "SessionOverlay.hpp"

#include <shared_core/Error.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {  
namespace modules {
namespace overlay {

Error initialize()
{
   return Success();
}

bool isSuspendable()
{
   return true;
}

bool launcherJobsFeatureDisplayed()
{
   return false;
}

void streamLauncherOutput(const std::string& jobId,
                          bool listening)
{
}

int verifyInstallation()
{
   return EXIT_SUCCESS;
}

void initMonitorClient(boost::asio::io_service& ioService)
{
}

std::string sessionNode()
{
   return std::string();
}

} // namespace overlay
} // namespace modules
} // namespace session
} // namespace rstudio
