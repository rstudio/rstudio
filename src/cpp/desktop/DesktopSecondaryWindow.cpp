/*
 * DesktopSecondaryWindow.cpp
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

#include "DesktopSecondaryWindow.hpp"

#include <QApplication>
#include <QToolBar>
#include <QDesktopWidget>

namespace rstudio {
namespace desktop {

namespace {

QIcon icon(const char* name)
{
#ifdef Q_OS_MAC
   static QString suffix(QString::fromUtf8("_mac"));
#else
   static QString suffix(QString::fromUtf8(""));
#endif
   return QIcon(QString::fromUtf8(":/icons/") + QString::fromUtf8(name) + suffix + QString::fromUtf8(".png"));
}

}

SecondaryWindow::SecondaryWindow(bool showToolbar, QString name, QUrl baseUrl,
                                 QWidget* pParent, WebPage* pOpener,
                                 bool allowExternalNavigate) :
    BrowserWindow(showToolbar, true, name, baseUrl, pParent, pOpener,
                  allowExternalNavigate)
{
   setAttribute(Qt::WA_QuitOnClose, true);
   setAttribute(Qt::WA_DeleteOnClose, true);

#ifdef Q_OS_MAC
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

   connect(webView(), SIGNAL(loadStarted()),
           this, SLOT(manageCommandState()));
   connect(webView(), SIGNAL(urlChanged(QUrl)),
           this, SLOT(manageCommandState()));

   manageCommandState();

   // Size it (use computed size if it seems sane; otherwise let Qt set it)
   QSize size = QSize(850, 1100).boundedTo(
         QApplication::primaryScreen()->availableGeometry().size());
   if (size.width() > 500 && size.height() > 500)
   {
      size.setHeight(size.height()-75);
      resize(size);
   }

   connect(webView(), SIGNAL(onCloseWindowShortcut()),
           this, SLOT(onCloseWindowShortcut()));
}

void SecondaryWindow::finishLoading(bool ok)
{
   BrowserWindow::finishLoading(ok);

   if (ok)
      connect(webView(), SIGNAL(onCloseWindowShortcut()), this,
              SLOT(onCloseWindowShortcut()));
}

void SecondaryWindow::onCloseWindowShortcut()
{
   close();
}

void SecondaryWindow::manageCommandState()
{
}

} // namespace desktop
} // namespace rstudio
