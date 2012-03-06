/*
 * DesktopWebPage.hpp
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

#ifndef DESKTOP_WEB_PAGE_HPP
#define DESKTOP_WEB_PAGE_HPP

#include <QtGui>
#include <QtWebKit>

namespace desktop {

class MainWindow;

struct PendingSatelliteWindow
{
   PendingSatelliteWindow()
      : name(), pMainWindow(NULL), width(-1), height(-1)
   {
   }

   PendingSatelliteWindow(QString name,
                          MainWindow* pMainWindow,
                          int width,
                          int height)
      : name(name), pMainWindow(pMainWindow), width(width), height(height)
   {
   }

   bool isEmpty() const { return name.isEmpty(); }

   QString name;

   MainWindow* pMainWindow;

   int width;
   int height;
};


class WebPage : public QWebPage
{
   Q_OBJECT

public:
   explicit WebPage(QUrl baseUrl = QUrl(), QWidget *parent = NULL);

   void setBaseUrl(const QUrl& baseUrl);

   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(const PendingSatelliteWindow& pendingWnd);

public slots:
   bool shouldInterruptJavaScript();

protected:
   QWebPage* createWindow(QWebPage::WebWindowType type);
   void javaScriptConsoleMessage(const QString& message, int lineNumber, const QString& sourceID);
   QString userAgentForUrl(const QUrl &url) const;
   bool acceptNavigationRequest(QWebFrame* frame,
                                const QNetworkRequest& request,
                                NavigationType type);

private:
   QUrl baseUrl_;
   bool navigated_;
   PendingSatelliteWindow pendingSatelliteWindow_;
};

} // namespace desktop

#endif // DESKTOP_WEB_PAGE_HPP
