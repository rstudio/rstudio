/*
 * DesktopDownloadHelper.hpp
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

#ifndef DESKTOP_DOWNLOAD_HELPER_HPP
#define DESKTOP_DOWNLOAD_HELPER_HPP

#include <QObject>
#include <QNetworkReply>

namespace desktop {

// DownloadHelper is self-freeing
class DownloadHelper : public QObject
{
    Q_OBJECT
public:
    DownloadHelper(QNetworkReply* pReply,
                   QString fileName);

    static void handleDownload(QNetworkReply* pReply, QString fileName);

protected slots:
    void onDownloadFinished();

signals:
    void downloadFinished(QString fileName);

private:
    QString fileName_;
};

} // namespace desktop

#endif // DESKTOP_DOWNLOAD_HELPER_HPP
