/*
 * RemoteDesktopSessionLauncherOverlay.cpp
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

#include "RemoteDesktopSessionLauncherOverlay.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

void RemoteDesktopSessionLauncher::launchFirstSession(const core::FilePath& installPath,
                                                      bool devMode,
                                                      const QStringList& arguments)
{
}

std::map<std::string, QNetworkCookie> RemoteDesktopSessionLauncher::getCookies()
{
   return std::map<std::string, QNetworkCookie>();
}

void RemoteDesktopSessionLauncher::onLaunchError(QString message)
{
}

void RemoteDesktopSessionLauncher::onLaunchFirstSession()
{
}

void RemoteDesktopSessionLauncher::onCookieAdded(const QNetworkCookie& cookie)
{
}

Error RemoteDesktopSessionLauncher::loadSession()
{
   return Success();
}

Error RemoteDesktopSessionLauncher::sendRequest(const http::Request& request,
                                                http::Response* pResponse)
{
   return Success();
}

void RemoteDesktopSessionLauncher::createRequest(const std::string& uri,
                                                 http::Request* pRequest)
{
}

void RemoteDesktopSessionLauncher::showUserSignInPage(const http::Response& response)
{
}

WorkspacesRequestState RemoteDesktopSessionLauncher::getWorkspacesUrl()
{
   return WorkspacesRequestState();
}

Error RemoteDesktopSessionLauncher::getSessionInfo(SessionInfo* pSessionInfo)
{
   return Success();
}

void RemoteDesktopSessionLauncher::handleLaunchError(const Error& error)
{
}

void RemoteDesktopSessionLauncher::closeAllSatellites()
{
}

void RemoteDesktopSessionLauncher::closeOnSignOut()
{
}

} // namespace desktop
} // namespace rstudio
