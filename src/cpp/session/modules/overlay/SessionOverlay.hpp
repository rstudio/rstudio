/*
 * SessionOverlay.hpp
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

#ifndef SESSION_OVERLAY_HPP
#define SESSION_OVERLAY_HPP

#include <string>
#include <boost/asio/io_service.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace overlay {

bool isSuspendable();

bool launcherJobsFeatureDisplayed();

void streamLauncherOutput(const std::string& jobId,
                          bool listening);

int verifyInstallation();

void initMonitorClient(boost::asio::io_service& ioService);

core::Error initialize();

std::string sessionNode();
   
} // namespace overlay
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_OVERLAY_HPP

