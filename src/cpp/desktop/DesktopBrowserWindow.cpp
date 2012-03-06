/*
 * DesktopBrowserWindow.cpp
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

#include "DesktopBrowserWindow.hpp"
#include <QWebFrame>
#include "DesktopWebView.hpp"

namespace desktop {

BrowserWindow::BrowserWindow(bool showToolbar,
                             bool adjustTitle,
                             QUrl baseUrl,
                             QWidget* pParent) :
   QMainWindow(pParent)
{
   adjustTitle_ = adjustTitle;
   progress_ = 0;

   pView_ = new WebView(baseUrl, this);
   QWebFrame* mainFrame = pView_->page()->mainFrame();
   connect(mainFrame, SIGNAL(javaScriptWindowObjectCleared()),
           this, SLOT(onJavaScriptWindowObjectCleared()));
   connect(pView_, SIGNAL(titleChanged(QString)), SLOT(adjustTitle()));
   connect(pView_, SIGNAL(loadProgress(int)), SLOT(setProgress(int)));
   connect(pView_, SIGNAL(loadFinished(bool)), SLOT(finishLoading(bool)));
   connect(pView_->page(), SIGNAL(printRequested(QWebFrame*)),
           this, SLOT(printRequested(QWebFrame*)));

   // Kind of a hack to new up a toolbar and not attach it to anything.
   // Once it is clear what secondary browser windows look like we can
   // decide whether to keep this.
   pToolbar_ = showToolbar ? addToolBar(tr("Navigation")) : new QToolBar();
   pToolbar_->setMovable(false);

   QGraphicsScene* pScene = new QGraphicsScene(this);
   pScene->addItem(pView_);
   pGraphicsView_ = new QGraphicsView(pScene, this);
   pGraphicsView_->setFrameShape(QFrame::NoFrame);
   pGraphicsView_->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
   pGraphicsView_->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
   setCentralWidget(pGraphicsView_);

   setUnifiedTitleAndToolBarOnMac(true);

   QShortcut* copyShortcut = new QShortcut(QKeySequence::Copy, this);
   connect(copyShortcut, SIGNAL(activated()),
           pView_->pageAction(QWebPage::Copy), SLOT(trigger()));
}

void BrowserWindow::resizeEvent (QResizeEvent *event)
{
   QMainWindow::resizeEvent(event);

   pView_->resize(event->size().width(), event->size().height());
}

void BrowserWindow::printRequested(QWebFrame* frame)
{
   QPrintPreviewDialog dialog(window());
   dialog.setWindowModality(Qt::WindowModal);
   connect(&dialog, SIGNAL(paintRequested(QPrinter*)),
           frame, SLOT(print(QPrinter*)));
   dialog.exec();
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
#ifdef Q_WS_MACX
   webView()->page()->mainFrame()->evaluateJavaScript(
         QString::fromAscii("document.body.className = document.body.className + ' avoid-move-cursor'"));
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

} // namespace desktop
