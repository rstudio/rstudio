/*
 * DesktopURLDownloader.hpp
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

#ifndef DESKTOP_URL_DOWNLOADER_HPP
#define DESKTOP_URL_DOWNLOADER_HPP

#include <string>
#include <vector>

#include <QUrl>
#include <QNetworkReply>
#include <QTimer>

namespace desktop {

// URLDownloader is self-freeing
class URLDownloader : public QObject
{
    Q_OBJECT

 public:
    URLDownloader(const QUrl& url,
                  int timeoutMs,
                  bool manuallyInvoked,
                  QObject* pParent);
    ~URLDownloader() {}

    bool manuallyInvoked() { return manuallyInvoked_; }

 signals:
    void downloadComplete(const QByteArray& data);
    void downloadError(const QString& message);
    void downloadTimeout();

 protected slots:
    void complete();
    void error(QNetworkReply::NetworkError);
    void timeout();

private:
    QNetworkReply* pReply_;
    QTimer* pRequestTimeoutTimer_;
    bool manuallyInvoked_;
};

} // namespace desktop

#endif // DESKTOP_URL_DOWNLOADER_HPP
