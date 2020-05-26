/*
 * DesktopDownloadItemHelper.hpp
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

#ifndef DESKTOP_DOWNLOAD_ITEM_HELPER_HPP
#define DESKTOP_DOWNLOAD_ITEM_HELPER_HPP

#include <QObject>

#include <QWebEngineDownloadItem>

namespace rstudio {
namespace desktop {

class DownloadHelper : public QObject
{
   Q_OBJECT
   
public:
   
   // NOTE: DownloadHelper automatically frees itself after the download
   // is finished, in response to &QWebEngineDownloadItem::finished() signal
   static DownloadHelper* manageDownload(
         QWebEngineDownloadItem* item,
         const QString& path);
   
public Q_SLOTS:
   void onDownloadProgress(qint64 bytesReceived, qint64 bytesTotal);
   void onFinished();
   void onPausedChanged(bool isPaused);
   void onStateChanged(QWebEngineDownloadItem::DownloadState state);
   
private:
   DownloadHelper(QWebEngineDownloadItem* item);
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_DOWNLOAD_ITEM_HELPER_HPP
