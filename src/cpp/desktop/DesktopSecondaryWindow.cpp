/*
 * DesktopSecondaryWindow.cpp
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

#include "DesktopSecondaryWindow.hpp"
#include "DesktopWebView.hpp"

namespace desktop {

namespace {

QIcon icon(const char* name)
{
#ifdef Q_WS_MACX
   static QString suffix(QString::fromAscii("_mac"));
#else
   static QString suffix(QString::fromAscii(""));
#endif
   return QIcon(QString::fromAscii(":/icons/") + QString::fromAscii(name) + suffix + QString::fromAscii(".png"));
}

}

SecondaryWindow::SecondaryWindow(QUrl baseUrl, QWidget* pParent) :
    BrowserWindow(true, true, baseUrl, pParent)
{
   setAttribute(Qt::WA_QuitOnClose, false);
   setAttribute(Qt::WA_DeleteOnClose, true);

#ifdef Q_WS_MACX
   setIconSize(QSize(26, 22));
#else
   setIconSize(QSize(26, 20));
#endif

   back_ = pToolbar_->addAction(icon("back"), QString::fromUtf8("Back"));
   back_->setToolTip(QString::fromUtf8("Back"));
   connect(back_, SIGNAL(triggered()),
           webView(), SLOT(back()));

   forward_ = pToolbar_->addAction(icon("forward"), QString::fromUtf8("Forward"));
   forward_->setToolTip(QString::fromUtf8("Forward"));
   connect(forward_, SIGNAL(triggered()),
           webView(), SLOT(forward()));

   reload_ = pToolbar_->addAction(icon("reload"), QString::fromUtf8("Reload"));
   reload_->setToolTip(QString::fromUtf8("Reload"));
   connect(reload_, SIGNAL(triggered()),
           webView(), SLOT(reload()));

   print_ = pToolbar_->addAction(icon("print"), QString::fromUtf8("Print"));
   print_->setToolTip(QString::fromUtf8("Print"));
   connect(print_, SIGNAL(triggered()),
           this, SLOT(print()));

   history_ = webView()->history();

   connect(webView(), SIGNAL(loadStarted()),
           this, SLOT(manageCommandState()));
   connect(webView(), SIGNAL(urlChanged(QUrl)),
           this, SLOT(manageCommandState()));

   manageCommandState();

   // Size it (use computed size if it seems sane; otherwise let Qt set it)
   QSize size = QSize(850, 1100).boundedTo(
         QApplication::desktop()->availableGeometry().size());
   if (size.width() > 500 && size.height() > 500)
   {
      size.setHeight(size.height()-75);
      resize(size);
   }
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
