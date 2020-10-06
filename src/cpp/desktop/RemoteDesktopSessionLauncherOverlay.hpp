/*
 * RemoteDesktopSessionLauncherOverlay.hpp
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

#ifndef REMOTE_DESKTOP_SESSION_LAUNCHER_HPP
#define REMOTE_DESKTOP_SESSION_LAUNCHER_HPP

#include <string>

#include <boost/utility.hpp>

#include <QNetworkCookie>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/Thread.hpp>
#include <shared_core/json/Json.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopMainWindow.hpp"

#include "DesktopSessionServersOverlay.hpp"

namespace rstudio {
namespace desktop {

struct WorkspacesRequestState
{
   WorkspacesRequestState() :
      authenticating(false),
      workspacesEnabled(true)
   {
   }

   WorkspacesRequestState(const core::Error& error) :
      error(error),
      authenticating(false),
      workspacesEnabled(true)
   {
   }

   WorkspacesRequestState(bool authenticating) :
      authenticating(authenticating),
      workspacesEnabled(true)
   {
   }

   WorkspacesRequestState(bool workspacesEnabled,
                          std::string workspacesUrl) :
      authenticating(false),
      workspacesEnabled(workspacesEnabled),
      workspacesUrl(workspacesUrl)
   {
   }

   core::Error error;
   bool authenticating;
   bool workspacesEnabled;
   std::string workspacesUrl;
};

struct SessionInfo
{
   SessionInfo() :
      launcherSession(false),
      running(false)
   {
   }

   SessionInfo(const std::string& sessionUrl,
               const std::string& sessionId,
               bool launcherSession,
               bool running) :
      url(sessionUrl),
      sessionId(sessionId),
      launcherSession(launcherSession),
      running(running)
   {
   }

   std::string url;
   std::string sessionId;
   bool launcherSession;
   bool running;
};

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
        signingIn_(false),
        failedToLaunch_(false)
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
        signingIn_(false),
        failedToLaunch_(false)
   {
   }

   void launchFirstSession(const core::FilePath& installPath,
                           bool devMode,
                           const QStringList& arguments);

   std::map<std::string, QNetworkCookie> getCookies();

   const SessionServer& sessionServer() const { return server_; }

   bool failedToLaunch() const { return failedToLaunch_; }

   void closeOnSignOut();

public Q_SLOTS:
   void onCookieAdded(const QNetworkCookie& cookie);
   void onLaunchFirstSession();
   void onLaunchError(QString message);

private:
   core::Error loadSession();

   core::Error startLauncherSession(const SessionInfo& sessionInfo);

   void createRequest(const std::string& uri,
                      core::http::Request* pRequest);

   core::Error sendRequest(const core::http::Request& request,
                           core::http::Response* pResponse);

   void showUserSignInPage(const core::http::Response& response);

   WorkspacesRequestState getWorkspacesUrl();

   core::Error getSessionInfo(SessionInfo* pSessionInfo);

   void handleLaunchError(const core::Error& error);

   void closeAllSatellites();

private:
   SessionServer server_;
   ApplicationLaunch* pAppLaunch_;
   MainWindow* pMainWindow_;
   bool createNewSession_;
   std::string sessionUrl_;
   bool signingIn_;
   bool failedToLaunch_;

   std::string workspacesUrl_;

   boost::mutex mutex_;
   std::map<std::string, QNetworkCookie> authCookies_;
};

} // namespace desktop
} // namespace rstudio

#endif // REMOTE_DESKTOP_SESSION_LAUNCHER_HPP
