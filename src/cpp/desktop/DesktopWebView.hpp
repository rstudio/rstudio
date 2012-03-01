/*
 * DesktopWebView.hpp
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

#ifndef DESKTOP_WEB_VIEW_HPP
#define DESKTOP_WEB_VIEW_HPP

#include <QtGui>
#include <QWebView>

#include "DesktopWebPage.hpp"

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

class WebView : public ::QWebView
{
   Q_OBJECT

public:
   explicit WebView(QUrl baseUrl = QUrl(),
                    QWidget *parent = NULL);

   void setBaseUrl(const QUrl& baseUrl);

   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(const PendingSatelliteWindow& pendingWnd);

signals:
  void onCloseWindowShortcut();

public slots:

protected:
   QWebView* createWindow(QWebPage::WebWindowType type);
   QString promptForFilename(const QNetworkRequest& request,
                             QNetworkReply* pReply);
   void keyPressEvent(QKeyEvent* pEv);
   void wheelEvent (QWheelEvent* event);

protected slots:
   void downloadRequested(const QNetworkRequest&);
   void unsupportedContent(QNetworkReply*);
   void openFile(QString file);
   void mouseWheelTimerFired();

private:
   PendingSatelliteWindow pendingSatelliteWindow_;
   QUrl baseUrl_;
   QTimer* pMouseWheelTimer_;
   QList<QWheelEvent> mouseWheelEvents_;
   WebPage* pWebPage_;
};

} // namespace desktop

#endif // DESKTOP_WEB_VIEW_HPP
