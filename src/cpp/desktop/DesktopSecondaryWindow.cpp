/*
 * DesktopSecondaryWindow.cpp
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

#include "DesktopSecondaryWindow.hpp"
#include "DesktopWebView.hpp"

namespace desktop {

namespace {

QIcon icon(const char* name)
{
#ifdef Q_WS_MACX
   static QString suffix("_mac");
#else
   static QString suffix("");
#endif
   return QIcon(QString(":/icons/") + name + suffix + ".png");
}

}

SecondaryWindow::SecondaryWindow(QUrl baseUrl, QWidget* pParent) :
    BrowserWindow(true, baseUrl, pParent)
{
   setAttribute(Qt::WA_QuitOnClose, false);
   setAttribute(Qt::WA_DeleteOnClose, true);

#ifdef Q_WS_MACX
   setIconSize(QSize(26, 22));
#else
   setIconSize(QSize(26, 20));
#endif

   back_ = pToolbar_->addAction(icon("back"), "Back");
   back_->setToolTip("Back");
   connect(back_, SIGNAL(triggered()),
           webView(), SLOT(back()));

   forward_ = pToolbar_->addAction(icon("forward"), "Forward");
   forward_->setToolTip("Forward");
   connect(forward_, SIGNAL(triggered()),
           webView(), SLOT(forward()));

   reload_ = pToolbar_->addAction(icon("reload"), "Reload");
   reload_->setToolTip("Reload");
   connect(reload_, SIGNAL(triggered()),
           webView(), SLOT(reload()));

   print_ = pToolbar_->addAction(icon("print"), "Print");
   print_->setToolTip("Print");
   connect(print_, SIGNAL(triggered()),
           this, SLOT(print()));

   history_ = webView()->history();

   connect(webView(), SIGNAL(loadStarted()),
           this, SLOT(manageCommandState()));
   connect(webView(), SIGNAL(urlChanged(QUrl)),
           this, SLOT(manageCommandState()));

   manageCommandState();
}

void SecondaryWindow::print()
{
   printRequested(webView()->page()->mainFrame());
}

void SecondaryWindow::manageCommandState()
{
   back_->setEnabled(history_->canGoBack());
   forward_->setEnabled(history_->canGoForward());
}

} // namespace desktop
