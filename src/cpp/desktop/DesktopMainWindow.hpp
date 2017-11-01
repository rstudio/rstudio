/*
 * DesktopMainWindow.hpp
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

#ifndef DESKTOP_MAIN_WINDOW_HPP
#define DESKTOP_MAIN_WINDOW_HPP

#include <vector>

#include <QProcess>
#include <QtGui>
#include <QSessionManager>

#include "DesktopGwtCallback.hpp"
#include "DesktopGwtWindow.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopApplicationLaunch.hpp"

namespace rstudio {
namespace desktop {

class SessionLauncher;

class MainWindow : public GwtWindow
{
   Q_OBJECT

public:
   MainWindow(QUrl url=QUrl());

public:
   QString getSumatraPdfExePath();
   void launchSession(bool reload);
   void launchRStudio(const std::vector<std::string>& args = std::vector<std::string>(),
                      const std::string& initialDir = std::string());

public slots:
   void quit();
   void loadUrl(const QUrl& url);
   void setMenuBar(QMenuBar *pMenuBar);
   void invokeCommand(QString commandId);
   void manageCommand(QString cmdId, QAction* pAction);
   void manageCommandVisibility(QString cmdId, QAction* pAction);
   void openFileInRStudio(QString path);
   void onPdfViewerClosed(QString pdfPath);
   void onPdfViewerSyncSource(QString srcFile, int line, int column);
   void onLicenseLost(QString licenseMessage);

signals:
   void firstWorkbenchInitialized();

protected slots:
   void onCloseWindowShortcut();
   void onJavaScriptWindowObjectCleared();
   void onWorkbenchInitialized();
   void resetMargins();
   void commitDataRequest(QSessionManager &manager);

protected:
   virtual void closeEvent(QCloseEvent*);
   virtual double getZoomLevel();
   virtual void setZoomLevel(double zoomLevel);

// private interface for SessionLauncher
private:
   friend class SessionLauncher;

   // allow SessionLauncher to give us a reference to itself (so we can
   // call launchProcess back on it)
   void setSessionLauncher(SessionLauncher* pSessionLauncher);

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
   virtual void onActivated();

private:
   bool quitConfirmed_;
   MenuCallback menuCallback_;
   GwtCallback gwtCallback_;
   SessionLauncher* pSessionLauncher_;
   ApplicationLaunch *pAppLauncher_;
   QProcess* pCurrentSessionProcess_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_MAIN_WINDOW_HPP
