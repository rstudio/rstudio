/*
 * DesktopMainWindow.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOP_MAIN_WINDOW_HPP
#define DESKTOP_MAIN_WINDOW_HPP

#include <QProcess>
#include <QtGui>
#include "DesktopGwtCallback.hpp"
#include "DesktopGwtWindow.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopUpdateChecker.hpp"

namespace desktop {

class MainWindow : public GwtWindow
{
   Q_OBJECT

public:
   MainWindow(QUrl url=QUrl());

public:
   QString getSumatraPdfExePath();

public slots:
   void quit();
   void loadUrl(const QUrl& url);
   void setMenuBar(QMenuBar *pMenuBar);
   void invokeCommand(QString commandId);
   void manageCommand(QString cmdId, QAction* pAction);
   void openFileInRStudio(QString path);
   void checkForUpdates();
   void onPdfViewerClosed(QString pdfPath);
   void onPdfViewerSyncSource(QString srcFile, int line, int column);

signals:
   void firstWorkbenchInitialized();

protected slots:
   void onCloseWindowShortcut();
   void onJavaScriptWindowObjectCleared();
   void onWorkbenchInitialized();
   void resetMargins();

protected:
   virtual void closeEvent(QCloseEvent*);

// private interface for SessionLauncher
private:
   friend class SessionLauncher;

   // allow SessionLauncher to give us a reference to the currently
   // active rsession process so that we can use it in closeEvent handling
   void setSessionProcess(QProcess* pSessionProcess);

   // allow SessionLauncher to collect restart requests from GwtCallback
   int collectPendingRestartRequest();

   bool desktopHooksAvailable();

   virtual void onActivated();

private:
   bool quitConfirmed_;
   MenuCallback menuCallback_;
   GwtCallback gwtCallback_;
   UpdateChecker updateChecker_;
   QProcess* pCurrentSessionProcess_;
};

} // namespace desktop

#endif // DESKTOP_MAIN_WINDOW_HPP
