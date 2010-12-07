/*
 * DesktopMainWindow.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

#include <QtGui>
#include "DesktopGwtCallback.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopBrowserWindow.hpp"
#include "DesktopUpdateChecker.hpp"

namespace desktop {

class MainWindow : public BrowserWindow
{
   Q_OBJECT

public:
   MainWindow(QUrl url=QUrl());

public slots:
   void quit();
   void loadUrl(const QUrl& url);
   void setMenuBar(QMenuBar *pMenuBar);
   void invokeCommand(QString commandId);
   void manageCommand(QString cmdId, QAction* pAction);
   void openFileInRStudio(QString path);
   void setSaveWorkspace(int value);
   void checkForUpdates();
signals:
   void workbenchInitialized();

protected slots:
   void onJavaScriptWindowObjectCleared();
   void onWorkbenchInitialized();
   void resetMargins();

protected:
   virtual void closeEvent(QCloseEvent*);

private:
   bool quitConfirmed_;
   MenuCallback menuCallback_;
   GwtCallback gwtCallback_;
   UpdateChecker updateChecker_;
};

} // namespace desktop

#endif // DESKTOP_MAIN_WINDOW_HPP
