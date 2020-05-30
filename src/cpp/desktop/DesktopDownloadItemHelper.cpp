/*
 * DesktopDownloadItemHelper.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "DesktopDownloadItemHelper.hpp"

#include <QDebug>

namespace rstudio {
namespace desktop {

DownloadHelper* DownloadHelper::manageDownload(
      QWebEngineDownloadItem* item,
      const QString& path)
{
   DownloadHelper* helper = new DownloadHelper(item);
   item->setPath(path);
   item->accept();
   return helper;
}

DownloadHelper::DownloadHelper(QWebEngineDownloadItem* item)
{
   connect(item, &QWebEngineDownloadItem::downloadProgress,
           this,  &DownloadHelper::onDownloadProgress);
   
   connect(item, &QWebEngineDownloadItem::finished,
           this, &DownloadHelper::onFinished);
   
   connect(item, &QWebEngineDownloadItem::isPausedChanged,
           this, &DownloadHelper::onPausedChanged);
   
   connect(item, &QWebEngineDownloadItem::stateChanged,
           this, &DownloadHelper::onStateChanged);
}

void DownloadHelper::onDownloadProgress(qint64 bytesReceived, qint64 bytesTotal)
{
}

void DownloadHelper::onFinished()
{
   deleteLater();
}

void DownloadHelper::onPausedChanged(bool isPaused)
{
}

void DownloadHelper::onStateChanged(QWebEngineDownloadItem::DownloadState state)
{
}

} // namespace desktop
} // namespace rstudio
