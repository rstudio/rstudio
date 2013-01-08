/*
 * DesktopURLDownloader.cpp
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

#include "DesktopURLDownloader.hpp"

#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>

namespace desktop {


URLDownloader::URLDownloader(const QUrl& url,
                             int timeoutMs,
                             bool manuallyInvoked,
                             QObject* pParent)
   : QObject(pParent),
     pRequestTimeoutTimer_(new QTimer(this)),
     manuallyInvoked_(manuallyInvoked)
{
   QNetworkAccessManager* pNetman = new QNetworkAccessManager(this);
   pReply_ = pNetman->get(QNetworkRequest(url));
   connect(pReply_, SIGNAL(finished()),
           this, SLOT(complete()));
   connect(pReply_, SIGNAL(error(QNetworkReply::NetworkError)),
           this, SLOT(error(QNetworkReply::NetworkError)));

   if (timeoutMs != -1)
   {
      pRequestTimeoutTimer_->setInterval(timeoutMs);
      pRequestTimeoutTimer_->setSingleShot(true);
      connect(pRequestTimeoutTimer_, SIGNAL(timeout()),
              this, SLOT(timeout()));
      pRequestTimeoutTimer_->start();
   }
}

void URLDownloader::complete()
{
   if (pRequestTimeoutTimer_->isActive())
      pRequestTimeoutTimer_->stop();

   QNetworkReply* pReply = qobject_cast<QNetworkReply*>(sender());

   if (pReply->error() == QNetworkReply::NoError)
   {
      QByteArray data = pReply->readAll();
      downloadComplete(data);
   }

   deleteLater();
}

void URLDownloader::error(QNetworkReply::NetworkError error)
{
   // ignore cancelled error (since it came from timeout)
   if (error == QNetworkReply::OperationCanceledError)
      return;

   if (pRequestTimeoutTimer_->isActive())
      pRequestTimeoutTimer_->stop();

   QNetworkReply* pReply = qobject_cast<QNetworkReply*>(sender());

   downloadError(pReply->errorString());

   deleteLater();
}

void URLDownloader::timeout()
{
   pReply_->abort();

   downloadTimeout();

   deleteLater();
}


}
