/*
 * DesktopMainWindow.hpp
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

#ifndef DESKTOP_MAIN_WINDOW_HPP
#define DESKTOP_MAIN_WINDOW_HPP

#include <vector>

#include <QProcess>
#include <QtGui>
#include <QSessionManager>

#include "DesktopInfo.hpp"
#include "DesktopGwtCallback.hpp"
#include "DesktopGwtWindow.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopApplicationLaunch.hpp"

namespace rstudio {
namespace desktop {

class SessionLauncher;
class JobLauncher;
class RemoteDesktopSessionLauncher;

class MainWindow : public GwtWindow
{
   Q_OBJECT

public:
   explicit MainWindow(QUrl url = QUrl(),
                       bool isRemoteDesktop = false);

public:
   QString getSumatraPdfExePath();
   void launchSession(bool reload);
   void launchRStudio(const std::vector<std::string>& args = std::vector<std::string>(),
                      const std::string& initialDir = std::string());
   void launchRemoteRStudio();
   void launchRemoteRStudioProject(const QString& projectUrl);

   RemoteDesktopSessionLauncher* getRemoteDesktopSessionLauncher();
   boost::shared_ptr<JobLauncher> getJobLauncher();

   QWebEngineProfile* getPageProfile();
   WebView* getWebView();
   bool workbenchInitialized();

public Q_SLOTS:
   void quit();
   void loadUrl(const QUrl& url);
   void loadRequest(const QWebEngineHttpRequest& request);
   void loadHtml(const QString& html);
   void setMenuBar(QMenuBar *pMenuBar);
   void invokeCommand(QString commandId);
   void runJavaScript(QString script);
   void openFileInRStudio(QString path);
   void onPdfViewerClosed(QString pdfPath);
   void onPdfViewerSyncSource(QString srcFile, int line, int column);
   void onLicenseLost(QString licenseMessage);
   void onUpdateLicenseWarningBar(QString message);

   bool isRemoteDesktop() const;

Q_SIGNALS:
   void firstWorkbenchInitialized();
   void urlChanged(QUrl url);

protected Q_SLOTS:
   void onWorkbenchInitialized();
   void onSessionQuit();
   void resetMargins();
   void commitDataRequest(QSessionManager &manager);

protected:
   void closeEvent(QCloseEvent*) override;

// private interface for SessionLauncher
private:
   friend class SessionLauncher;
   friend class RemoteDesktopSessionLauncher;

   // allow SessionLauncher to give us a reference to itself (so we can
   // call launchProcess back on it)
   void setSessionLauncher(SessionLauncher* pSessionLauncher);
   void setRemoteDesktopSessionLauncher(RemoteDesktopSessionLauncher* pSessionLauncher);

   // same for application launches
   void setAppLauncher(ApplicationLaunch* pAppLauncher);

   // allow SessionLauncher to give us a reference to the currently
   // active rsession process so that we can use it in closeEvent handling
   void setSessionProcess(QProcess* pSessionProcess);

   // allow SessionLauncher to collect restart requests from GwtCallback
   int collectPendingQuitRequest();

   // check whether desktop hooks have been initialized
   bool desktopHooksAvailable();

   // callback when window is activated
   void onActivated() override;

   void onUrlChanged(QUrl url);
   void onLoadFinished(bool ok);

   void saveRemoteAuthCookies(const boost::function<QList<QNetworkCookie>()>& loadCookies,
                              const boost::function<void(QList<QNetworkCookie>)>& saveCookies,
                              bool saveSessionCookies);

private:
   bool isRemoteDesktop_;
   bool quitConfirmed_ = false;
   bool geometrySaved_ = false;
   bool workbenchInitialized_ = false;
   MenuCallback menuCallback_;
   GwtCallback gwtCallback_;
   SessionLauncher* pSessionLauncher_;
   RemoteDesktopSessionLauncher* pRemoteSessionLauncher_;
   boost::shared_ptr<JobLauncher> pLauncher_;
   ApplicationLaunch *pAppLauncher_;
   QProcess* pCurrentSessionProcess_;

#ifdef _WIN32
   HWINEVENTHOOK eventHook_;
#endif

};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_MAIN_WINDOW_HPP
