/*
 * DesktopNetworkAccessManager.cpp
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

#include "DesktopNetworkAccessManager.hpp"
#include <QNetworkDiskCache>
#include <QDesktopServices>

#include <core/FilePath.hpp>

using namespace core;

extern QString sharedSecret;

namespace desktop {

NetworkAccessManager* NetworkAccessManager::instance_ = NULL;

NetworkAccessManager* NetworkAccessManager::instance()
{
   if (instance_ == NULL)
      instance_ = new NetworkAccessManager(sharedSecret);
   return instance_;
}

NetworkAccessManager::NetworkAccessManager(QString secret) :
    QNetworkAccessManager(), secret_(secret)
{
   setProxy(QNetworkProxy::NoProxy);

   // initialize cache
   QString cacheDir =
         QDesktopServices::storageLocation(QDesktopServices::CacheLocation);
   FilePath cachePath(cacheDir.toUtf8().constData());
   FilePath browserCachePath = cachePath.complete("RStudioWebkit");
   Error error = browserCachePath.ensureDirectory();
   if (!error)
   {
      QNetworkDiskCache* pCache = new QNetworkDiskCache();
      QString browserCacheDir = QString::fromUtf8(
                                    browserCachePath.absolutePath().c_str());
      pCache->setCacheDirectory(browserCacheDir);
      setCache(pCache);
   }
   else
   {
      LOG_ERROR(error);
   }
}

QNetworkReply* NetworkAccessManager::createRequest(
      Operation op,
      const QNetworkRequest& req,
      QIODevice* outgoingData)
{
   QNetworkRequest req2 = req;
   req2.setRawHeader("X-Shared-Secret",
                     secret_.toAscii());
   return this->QNetworkAccessManager::createRequest(op, req2, outgoingData);
}

} // namespace desktop
