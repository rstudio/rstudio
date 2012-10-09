/*
 * DesktopNetworkAccessManager.cpp
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

#include "DesktopNetworkAccessManager.hpp"

#include <core/FilePath.hpp>

#include <QTimer>

#include "DesktopNetworkReply.hpp"
#include "DesktopOptions.hpp"

using namespace core;

using namespace desktop;

NetworkAccessManager::NetworkAccessManager(QString secret, QObject *parent) :
    QNetworkAccessManager(parent), secret_(secret)
{
   setProxy(QNetworkProxy::NoProxy);

   QTimer* pTimer = new QTimer(this);
   connect(pTimer, SIGNAL(timeout()), SLOT(pollForIO()));
   pTimer->start(25);
}

QNetworkReply* NetworkAccessManager::createRequest(
      Operation op,
      const QNetworkRequest& req,
      QIODevice* outgoingData)
{ 
   if (req.url().scheme() == QString::fromAscii("http") &&
       req.url().host() == QString::fromAscii("127.0.0.1"))
   {
      return new NetworkReply(
            desktop::options().localPeerPath(),
            op,
            req,
            outgoingData,
            this);
   }
   else
   {
      return QNetworkAccessManager::createRequest(op, req, outgoingData);
   }
}


void NetworkAccessManager::pollForIO()
{
   boost::system::error_code ec;
   NetworkReply::sharedIoService().poll(ec);
   if (ec)
      LOG_ERROR(Error(ec, ERROR_LOCATION));

   NetworkReply::sharedIoService().reset();
}
