/*
 * RemoteDesktopSessionLauncherOverlay.hpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

#ifndef REMOTE_DESKTOP_SESSION_LAUNCHER_HPP
#define REMOTE_DESKTOP_SESSION_LAUNCHER_HPP

#include <string>

#include <boost/utility.hpp>

#include <QNetworkCookie>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopMainWindow.hpp"

#include "DesktopSessionServersOverlay.hpp"

namespace rstudio {
namespace desktop {

class RemoteDesktopSessionLauncher : public QObject
{
   Q_OBJECT
public:
   RemoteDesktopSessionLauncher(const SessionServer& server,
                                ApplicationLaunch* pAppLaunch,
                                bool createNewSession)
      : server_(server),
        pAppLaunch_(pAppLaunch),
        pMainWindow_(nullptr),
        createNewSession_(createNewSession),
        signingIn_(false)
   {
   }

   RemoteDesktopSessionLauncher(const SessionServer& server,
                                ApplicationLaunch* pAppLaunch,
                                const std::string& sessionUrl)
      : server_(server),
        pAppLaunch_(pAppLaunch),
        pMainWindow_(nullptr),
        createNewSession_(false),
        sessionUrl_(sessionUrl),
        signingIn_(false)
   {
   }

   void launchFirstSession();

   const SessionServer& sessionServer() const { return server_; }

public Q_SLOTS:
   void onCookieAdded(const QNetworkCookie& cookie);

private:
   core::Error loadSession();

   void createRequest(const std::string& uri,
                      core::http::Request* pRequest);

   core::Error sendRequest(const core::http::Request& request,
                           core::http::Response* pResponse);

   void showUserSignInPage(const core::http::Response& response);

   core::Error getWorkspacesUrl(std::string* pUrl);

   core::Error getSessionUrl(std::string* pSessionUrl);

   void handleLaunchError(const core::Error& error);

   void closeAllSatellites();

private:
   SessionServer server_;
   ApplicationLaunch* pAppLaunch_;
   MainWindow* pMainWindow_;
   bool createNewSession_;
   std::string sessionUrl_;
   bool signingIn_;

   std::map<std::string, QNetworkCookie> authCookies_;
};

} // namespace desktop
} // namespace rstudio

#endif // REMOTE_DESKTOP_SESSION_LAUNCHER_HPP
