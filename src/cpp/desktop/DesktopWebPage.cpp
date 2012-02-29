/*
 * DesktopWebPage.cpp
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

#include "DesktopWebPage.hpp"
#include <QWidget>
#include <QtDebug>
#include "DesktopNetworkAccessManager.hpp"

#include "DesktopUtils.hpp"

extern QString sharedSecret;

namespace desktop {

WebPage::WebPage(QUrl baseUrl, QWidget *parent) :
      QWebPage(parent),
      baseUrl_(baseUrl),
      navigated_(false)
{
   //settings()->setAttribute(QWebSettings::DeveloperExtrasEnabled, true);
   settings()->setAttribute(QWebSettings::JavascriptCanOpenWindows, true);
   setNetworkAccessManager(new NetworkAccessManager(sharedSecret, parent));
}

void WebPage::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
}

bool WebPage::shouldInterruptJavaScript()
{
   return false;
}

void WebPage::javaScriptConsoleMessage(const QString& message, int /*lineNumber*/, const QString& /*sourceID*/)
{
   qDebug() << message;
}

QString WebPage::userAgentForUrl(const QUrl &url) const
{
   return this->QWebPage::userAgentForUrl(url) + QString::fromAscii(" Qt/") + QString::fromAscii(qVersion());
}

bool WebPage::acceptNavigationRequest(QWebFrame*,
                                       const QNetworkRequest& request,
                                       NavigationType navType)
{
   QUrl url = request.url();

   if (url.toString() == QString::fromAscii("about:blank"))
      return true;

   if (url.scheme() != QString::fromAscii("http")
       && url.scheme() != QString::fromAscii("https")
       && url.scheme() != QString::fromAscii("mailto")
       && url.scheme() != QString::fromAscii("data"))
   {
      qDebug() << url.toString();
      return false;
   }

   if (baseUrl_.isEmpty() ||
       (url.scheme() == baseUrl_.scheme()
        && url.host() == baseUrl_.host()
        && url.port() == baseUrl_.port()))
   {
      navigated_ = true;
      return true;
   }
   else
   {
      desktop::openUrl(url);

      if (!navigated_)
         this->view()->window()->deleteLater();

      return false;
   }
}

}
