/*
 * DesktopBrowserWindow.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef DESKTOP_BROWSER_WINDOW_HPP
#define DESKTOP_BROWSER_WINDOW_HPP

#include <QAction>
#include <QMainWindow>
#include <QLineEdit>

#include "DesktopWebView.hpp"
#include "DesktopGwtCallbackOwner.hpp"

namespace rstudio {
namespace desktop {

class BrowserWindow : public QMainWindow, public GwtCallbackOwner
{
    Q_OBJECT
public:
    explicit BrowserWindow(bool showToolbar,
                           bool adjustTitle,
                           QString name,
                           QUrl baseUrl = QUrl(),
                           QWidget *parent = nullptr,
                           WebPage *pOpener = nullptr,
                           bool allowExternalNavigate = false);
    WebView* webView();

protected Q_SLOTS:

     void adjustTitle();
     void setProgress(int p);
     virtual void finishLoading(bool);

protected:
     void avoidMoveCursorIfNecessary();

     // implement GwtCallbackOwner
     QWidget* asWidget() override;
     WebPage* webPage() override;
     void postWebViewEvent(QEvent *event) override;
     void triggerPageAction(QWebEnginePage::WebAction action) override;
     void closeEvent(QCloseEvent *event) override;

protected:
     WebView* pView_;
     QToolBar* pToolbar_;
     QString getName();

private:
     int progress_;
     bool adjustTitle_;
     QString name_;
     WebPage* pOpener_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_BROWSER_WINDOW_HPP
