/*
 * DesktopBrowserWindow.hpp
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

#ifndef DESKTOP_BROWSER_WINDOW_HPP
#define DESKTOP_BROWSER_WINDOW_HPP

#include <QAction>
#include <QMainWindow>
#include <QWebView>
#include <QLineEdit>

#include "DesktopWebView.hpp"
#include "DesktopGwtCallbackOwner.hpp"

namespace desktop {

class BrowserWindow : public QMainWindow, public GwtCallbackOwner
{
    Q_OBJECT
public:
    explicit BrowserWindow(bool showToolbar,
                           bool adjustTitle,
                           QUrl baseUrl = QUrl(),
                           QWidget *parent = NULL);
    WebView* webView();

protected slots:

     void adjustTitle();
     void setProgress(int p);
     virtual void finishLoading(bool);
     virtual void onJavaScriptWindowObjectCleared() {}
     void printRequested(QWebFrame* frame);

protected:
     void avoidMoveCursorIfNecessary();

     // implement GwtCallbackOwner
     virtual QWidget* asWidget();
     virtual WebPage* webPage();
     virtual void postWebViewEvent(QEvent *event);
     virtual void triggerPageAction(QWebPage::WebAction action);

     // hooks for subclasses
     virtual QSize printDialogMinimumSize()
     {
         return QSize(0,0);
     }

protected:
     WebView* pView_;
     QToolBar* pToolbar_;

private:
     int progress_;
     bool adjustTitle_;
};

} // namespace desktop

#endif // DESKTOP_BROWSER_WINDOW_HPP
