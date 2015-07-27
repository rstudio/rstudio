/*
 * DesktopSatelliteWindow.cpp
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

#include "DesktopSatelliteWindow.hpp"

#include <QWebFrame>
#include <QShortcut>

#include "DesktopGwtCallback.hpp"

namespace rstudio {
namespace desktop {


SatelliteWindow::SatelliteWindow(MainWindow* pMainWindow, QString name) :
    GwtWindow(false, true, name),
    gwtCallback_(pMainWindow, this)
{
   setAttribute(Qt::WA_QuitOnClose, false);
   setAttribute(Qt::WA_DeleteOnClose, true);

   setWindowIcon(QIcon(QString::fromUtf8(":/icons/RStudio.ico")));

   // satellites don't have a menu, so connect zoom keyboard shortcuts
   // directly
   QShortcut* zoomInShortcut = new QShortcut(QKeySequence::ZoomIn, this);
   QShortcut* zoomOutShortcut = new QShortcut(QKeySequence::ZoomOut, this);
   connect(zoomInShortcut, SIGNAL(activated()), this, SLOT(zoomIn()));
   connect(zoomOutShortcut, SIGNAL(activated()), this, SLOT(zoomOut()));
}

void SatelliteWindow::onJavaScriptWindowObjectCleared()
{
   GwtWindow::onJavaScriptWindowObjectCleared();

   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         QString::fromUtf8("desktop"),
         &gwtCallback_,
         QWebFrame::QtOwnership);

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

void SatelliteWindow::closeEvent(QCloseEvent *event)
{
   QWebFrame* pFrame = webView()->page()->mainFrame();

   // the source window has special close semantics
   if (getName().startsWith(QString::fromUtf8(SOURCE_WINDOW_PREFIX)))
   {
     if (!pFrame->evaluateJavaScript(
            QString::fromUtf8("window.rstudioReadyToClose")).toBool())
     {
        pFrame->evaluateJavaScript(
            QString::fromUtf8("window.rstudioCloseSourceWindow();"));
        event->ignore();
        return;
     }
   }
   pFrame->evaluateJavaScript(QString::fromUtf8(
        "if (window.notifyRStudioSatelliteClosing) "
        "   window.notifyRStudioSatelliteClosing();"));

   // forward the close event to the web view
   webView()->event(event);
}

void SatelliteWindow::onActivated()
{
   webView()->page()->mainFrame()->evaluateJavaScript(QString::fromUtf8(
         "if (window.notifyRStudioSatelliteReactivated) "
         "   window.notifyRStudioSatelliteReactivated(null);"));
}


} // namespace desktop
} // namespace rstudio
