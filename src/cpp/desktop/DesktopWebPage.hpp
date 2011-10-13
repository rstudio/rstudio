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

class WebPage : public QWebPage
{
   Q_OBJECT

public:
   explicit WebPage(QUrl baseUrl = QUrl(), QWidget *parent = NULL);

   void setBaseUrl(const QUrl& baseUrl);

public slots:
   bool shouldInterruptJavaScript();

protected:
   void javaScriptConsoleMessage(const QString& message, int lineNumber, const QString& sourceID);
   QString userAgentForUrl(const QUrl &url) const;
   bool acceptNavigationRequest(QWebFrame* frame,
                                const QNetworkRequest& request,
                                NavigationType type);

private:
   QUrl baseUrl_;
   bool navigated_;
};

} // namespace desktop

#endif // DESKTOP_WEB_PAGE_HPP
