/*
 * DesktopBrowserWindow.cpp
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

#include "DesktopBrowserWindow.hpp"
#include <QWebFrame>
#include <QToolBar>
#include <QShortcut>

#include <QtPrintSupport/QPrinter>
#include <QtPrintSupport/QPrintPreviewDialog>

#include "DesktopWebView.hpp"

#include "DesktopUtils.hpp"
#include "DesktopOptions.hpp"

namespace rstudio {
namespace desktop {

BrowserWindow::BrowserWindow(bool showToolbar,
                             bool adjustTitle,
                             QString name,
                             QUrl baseUrl,
                             QWidget* pParent,
                             WebPage* pOpener,
                             bool allowExternalNavigate) :
   QMainWindow(pParent),
   name_(name),
   pOpener_(pOpener)
{
   adjustTitle_ = adjustTitle;
   progress_ = 0;

   pView_ = new WebView(baseUrl, this, allowExternalNavigate);
   QWebFrame* mainFrame = pView_->page()->mainFrame();
   connect(mainFrame, SIGNAL(javaScriptWindowObjectCleared()),
           this, SLOT(onJavaScriptWindowObjectCleared()));
   connect(pView_, SIGNAL(titleChanged(QString)), SLOT(adjustTitle()));
   connect(pView_, SIGNAL(loadProgress(int)), SLOT(setProgress(int)));
   connect(pView_, SIGNAL(loadFinished(bool)), SLOT(finishLoading(bool)));
   connect(pView_->page(), SIGNAL(printRequested(QWebFrame*)),
           this, SLOT(printRequested(QWebFrame*)));

   // set zoom factor
   double zoomLevel = options().zoomLevel();
   if (zoomLevel != pView_->dpiAwareZoomFactor())
      pView_->setDpiAwareZoomFactor(options().zoomLevel());

   // Kind of a hack to new up a toolbar and not attach it to anything.
   // Once it is clear what secondary browser windows look like we can
   // decide whether to keep this.
   pToolbar_ = showToolbar ? addToolBar(tr("Navigation")) : new QToolBar();
   pToolbar_->setMovable(false);

   setCentralWidget(pView_);
   setUnifiedTitleAndToolBarOnMac(true);

   desktop::enableFullscreenMode(this, false);
}

void BrowserWindow::printRequested(QWebFrame* frame)
{
   QPrinter printer;
   printer.setOutputFormat(QPrinter::NativeFormat);
   QPrintPreviewDialog dialog(&printer, window());
   QSize size = printDialogMinimumSize();
   if (!size.isNull())
      dialog.setMinimumSize(size);
   dialog.setWindowModality(Qt::WindowModal);
   connect(&dialog, SIGNAL(paintRequested(QPrinter*)),
           frame, SLOT(print(QPrinter*)));
   dialog.exec();
}

void BrowserWindow::closeEvent(QCloseEvent *event)
{
   if (pOpener_ == NULL)
   {
      // if we don't know where we were opened from, check window.opener
      // (note that this could also be empty)
      QString cmd = QString::fromUtf8("if (window.opener && "
         "window.opener.unregisterDesktopChildWindow))"
         "   window.opener.unregisterDesktopChildWindow('");
      cmd.append(name_);
      cmd.append(QString::fromUtf8("');"));
      webView()->page()->mainFrame()->evaluateJavaScript(cmd);
   }
   else if (pOpener_->mainFrame())
   {
      // if we do know where we were opened from and it has the appropriate
      // handlers, let it know we're closing
      QString cmd = QString::fromUtf8(
                  "if (window.unregisterDesktopChildWindow) "
                  "   window.unregisterDesktopChildWindow('");
      cmd.append(name_);
      cmd.append(QString::fromUtf8("');"));
      pOpener_->mainFrame()->evaluateJavaScript(cmd);
   }

   // forward the close event to the web view
   webView()->event(event);
}

void BrowserWindow::adjustTitle()
{
   if (adjustTitle_)
      setWindowTitle(pView_->title());
}

void BrowserWindow::setProgress(int p)
{
   progress_ = p;
   adjustTitle();
}

void BrowserWindow::finishLoading(bool)
{
   progress_ = 100;
   adjustTitle();
}

WebView* BrowserWindow::webView()
{
   return pView_;
}

void BrowserWindow::avoidMoveCursorIfNecessary()
{
#ifdef Q_OS_MAC
   webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromUtf8("document.body.className = document.body.className + ' avoid-move-cursor'"));
#endif
}

QWidget* BrowserWindow::asWidget()
{
   return this;
}

WebPage* BrowserWindow::webPage()
{
   return webView()->webPage();
}

void BrowserWindow::postWebViewEvent(QEvent *keyEvent)
{
   QCoreApplication::postEvent(webView(), keyEvent);
}

void BrowserWindow::triggerPageAction(QWebPage::WebAction action)
{
   webView()->triggerPageAction(action);
}

void BrowserWindow::onJavaScriptWindowObjectCleared()
{
   QString cmd = QString::fromUtf8("if (window.opener && "
      "window.opener.registerDesktopChildWindow))"
      "   window.opener.registerDesktopChildWindow('");
   cmd.append(name_);
   cmd.append(QString::fromUtf8("', window);"));

   webView()->page()->mainFrame()->evaluateJavaScript(cmd);
}

QString BrowserWindow::getName()
{
   return name_;
}

} // namespace desktop
} // namespace rstudio
