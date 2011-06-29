/*
 * DesktopBrowserWindow.hpp
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

#ifndef SESSION_BROWSER_WINDOW_HPP
#define SESSION_BROWSER_WINDOW_HPP

#include <QAction>
#include <QMainWindow>
#include <QWebView>
#include <QLineEdit>

#include "DesktopWebView.hpp"

namespace desktop {

class BrowserWindow : public QMainWindow
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
     void finishLoading(bool);
     virtual void onJavaScriptWindowObjectCleared() {}
     void printRequested(QWebFrame* frame);

protected:
     WebView* pView_;
     QToolBar* pToolbar_;

private:
     int progress_;
     bool adjustTitle_;
};

} // namespace desktop

#endif // SESSION_BROWSER_WINDOW_HPP
