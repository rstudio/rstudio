/*
 * DesktopWebView.hpp
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

#ifndef DESKTOP_WEB_VIEW_HPP
#define DESKTOP_WEB_VIEW_HPP

#include <QtGui>
#include <QWebView>

#include "DesktopWebPage.hpp"

namespace desktop {

class MainWindow;

class WebView : public ::QWebView
{
   Q_OBJECT

public:
   explicit WebView(QUrl baseUrl = QUrl(),
                    QWidget *parent = NULL);

   void setBaseUrl(const QUrl& baseUrl);

   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(const PendingSatelliteWindow& pendingWnd);
   void setDpiAwareZoomFactor(qreal factor);
   qreal dpiAwareZoomFactor();

   WebPage* webPage() const { return pWebPage_; }

signals:
  void onCloseWindowShortcut();

public slots:

protected:
   QString promptForFilename(const QNetworkRequest& request,
                             QNetworkReply* pReply);
   void keyPressEvent(QKeyEvent* pEv);

protected slots:
   void downloadRequested(const QNetworkRequest&);
   void unsupportedContent(QNetworkReply*);
   void openFile(QString file);

private:
   QUrl baseUrl_;
   WebPage* pWebPage_;
   double dpiZoomScaling_;
};

} // namespace desktop

#endif // DESKTOP_WEB_VIEW_HPP
