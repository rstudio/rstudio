/*
 * DesktopJobLauncherOverlay.cpp
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

#include "DesktopJobLauncherOverlay.hpp"

namespace rstudio {
namespace desktop {

using namespace rstudio::core;

class JobLauncherImpl
{
};

JobLauncher::JobLauncher(MainWindow* pMainWindow)
{
}

Error JobLauncher::initialize()
{
   return Success();
}

void JobLauncher::startLauncherJobStatusStream(const std::string& jobId)
{
}

void JobLauncher::stopLauncherJobStatusStream(const std::string& jobId)
{
}

void JobLauncher::startLauncherJobOutputStream(const std::string& jobId)
{
}

void JobLauncher::stopLauncherJobOutputStream(const std::string& jobId)
{
}

void JobLauncher::controlLauncherJob(const std::string& jobId,
                                     const std::string& operation)
{
}

void JobLauncher::submitLauncherJob(const json::Object& jobObj)
{
}

void JobLauncher::validateJobsConfig()
{
}

void JobLauncher::getJobContainerUser()
{
}

bool JobLauncher::setSessionServer(const SessionServer& sessionServer)
{
   return true;
}

void JobLauncher::signIn()
{
}

int JobLauncher::getProxyPortNumber()
{
   return 0;
}

SessionServer JobLauncher::getLauncherServer()
{
   return SessionServer("", "");
}

std::map<std::string, QNetworkCookie> JobLauncher::getCookies()
{
   return std::map<std::string, QNetworkCookie>();
}

} // namespace desktop
} // namespace rstudio
