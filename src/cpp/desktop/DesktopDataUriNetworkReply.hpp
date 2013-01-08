/*
 * DesktopDataUriNetworkReply.hpp
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

#ifndef DESKTOP_DATA_URI_NETWORK_REPLY_HPP
#define DESKTOP_DATA_URI_NETWORK_REPLY_HPP

#include <QNetworkReply>
#include <QNetworkRequest>

namespace desktop {

class DataUriNetworkReply : public QNetworkReply
{
   Q_OBJECT

public:
   DataUriNetworkReply(QObject* parent, const QNetworkRequest &req)
      : QNetworkReply(parent), offset_(0)
   {
      setRequest(req);
      setUrl(req.url());
      setOperation(QNetworkAccessManager::GetOperation);
   }

public:
   void createReply();

public:
   void abort()
   {
      close();
   }

   qint64 bytesAvailable() const;

   bool isSequential() const
   {
      return true;
   }

protected:
   qint64 readData(char *data, qint64 maxSize);

private:
   void setContent(const QByteArray& content);

private:
   QByteArray content_;
   int offset_;
};


} // namespace desktop

#endif // DESKTOP_DATA_URI_NETWORK_REPLY_HPP
