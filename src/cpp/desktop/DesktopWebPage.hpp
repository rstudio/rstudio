/*
 * DesktopWebPage.hpp
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

#ifndef DESKTOP_WEB_PAGE_HPP
#define DESKTOP_WEB_PAGE_HPP

#include <QtGui>
#include <QtWebKit>
#include <QWebPage>

namespace rstudio {
namespace desktop {

class MainWindow;

struct PendingWindow
{
   PendingWindow()
      : name(), pMainWindow(NULL), width(-1), height(-1),
        isSatellite(false), allowExternalNavigate(false), showToolbar(false)
   {
   }

   PendingWindow(QString name,
                 MainWindow* pMainWindow,
                 int width,
                 int height)
      : name(name), pMainWindow(pMainWindow), width(width), height(height),
        isSatellite(true), allowExternalNavigate(false), showToolbar(false)
   {
   }

   PendingWindow(QString name, bool allowExternalNavigation,
                 bool showDesktopToolbar)
      : name(name), pMainWindow(NULL), isSatellite(false),
        allowExternalNavigate(allowExternalNavigation),
        showToolbar(showDesktopToolbar)
   {
   }

   bool isEmpty() const { return name.isEmpty(); }

   QString name;

   MainWindow* pMainWindow;

   int width;
   int height;
   bool isSatellite;
   bool allowExternalNavigate;
   bool showToolbar;
};


class WebPage : public QWebPage
{
   Q_OBJECT

public:
   explicit WebPage(QUrl baseUrl = QUrl(), QWidget *parent = NULL,
                    bool allowExternalNavigate = false);

   void setBaseUrl(const QUrl& baseUrl);
   void setViewerUrl(const QString& viewerUrl);
   void prepareExternalNavigate(const QString& externalUrl);

   void activateWindow(QString name);
   void prepareForWindow(const PendingWindow& pendingWnd);
   void closeWindow(QString name);

public slots:
   bool shouldInterruptJavaScript();
   void closeRequested();

protected:
   QWebPage* createWindow(QWebPage::WebWindowType type);
   void javaScriptConsoleMessage(const QString& message, int lineNumber, const QString& sourceID);
   QString userAgentForUrl(const QUrl &url) const;
   bool acceptNavigationRequest(QWebFrame* frame,
                                const QNetworkRequest& request,
                                NavigationType type);

private:
   void handleBase64Download(QWebFrame* pWebFrame, QUrl url);

private:
   QUrl baseUrl_;
   QString viewerUrl_;
   bool navigated_;
   bool allowExternalNav_;
   PendingWindow pendingWindow_;
   QDir defaultSaveDir_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_WEB_PAGE_HPP
