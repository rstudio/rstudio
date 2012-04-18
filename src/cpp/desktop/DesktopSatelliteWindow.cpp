/*
 * DesktopSatelliteWindow.cpp
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

#include "DesktopSatelliteWindow.hpp"

#include "DesktopGwtCallback.hpp"

namespace desktop {


SatelliteWindow::SatelliteWindow(MainWindow* pMainWindow) :
    GwtWindow(false, true),
    gwtCallback_(pMainWindow, this)
{
   setAttribute(Qt::WA_QuitOnClose, false);
   setAttribute(Qt::WA_DeleteOnClose, true);

   setWindowIcon(QIcon(QString::fromAscii(":/icons/RStudio.ico")));
}

void SatelliteWindow::onJavaScriptWindowObjectCleared()
{
   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         QString::fromAscii("desktop"),
         &gwtCallback_,
         QScriptEngine::QtOwnership);

   connect(webView(), SIGNAL(onCloseWindowShortcut()),
           this, SLOT(onCloseWindowShortcut()));
}

void SatelliteWindow::onCloseWindowShortcut()
{
   close();
}

void SatelliteWindow::finishLoading(bool ok)
{
   BrowserWindow::finishLoading(ok);

   if (ok)
      avoidMoveCursorIfNecessary();
}

void SatelliteWindow::closeEvent(QCloseEvent *)
{
    webView()->page()->mainFrame()->evaluateJavaScript(QString::fromAscii(
         "if (window.notifyRStudioSatelliteClosing) "
         "   window.notifyRStudioSatelliteClosing();"));
}

void SatelliteWindow::onActivated()
{
   webView()->page()->mainFrame()->evaluateJavaScript(QString::fromAscii(
         "if (window.notifyRStudioSatelliteReactivated) "
         "   window.notifyRStudioSatelliteReactivated(null);"));
}


} // namespace desktop
