/*
 * DesktopDataUriNetworkReply.cpp
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

#include "DesktopDataUriNetworkReply.hpp"

#include <QtGlobal>
#include <QTimer>
#include <QDebug>

#include "DesktopUtils.hpp"

namespace desktop {

// NOTE: this does not yet actually work. PNGs served back through this
// class end up unreadable by the client (but it appears as if at least
// one GIF works). Note we actually tried returning just raw bytes
// without the headers and it worked. We should therefore do more
// research on what the expected behavior for data uris
//
// NOTE: if we get this work we still need to do some work around:
//
//  (1) Optimizing/eliminating data copies
//  (2) Return error conditions for malformed data
//  (3) Other assorted cleanup
//

void DataUriNetworkReply::createReply()
{
   // get the url as a byte array
   QByteArray urlBytes = url().toEncoded(QUrl::None);

   // get the delimiter locations
   int colonLoc = urlBytes.indexOf(':');
   int semicolonLoc = urlBytes.indexOf(';', colonLoc);
   int commaLoc = urlBytes.indexOf(',', semicolonLoc);

   // get the content type
   QByteArray contentTypeBytes = urlBytes.mid(colonLoc + 1,
                                              semicolonLoc - colonLoc - 1);
   QString contentType = QString::fromAscii(contentTypeBytes.data(),
                                            contentTypeBytes.size());
   setHeader(QNetworkRequest::ContentTypeHeader, contentType);

   // get the base64 encoded data
   QByteArray base64Content = urlBytes.right(urlBytes.size() - commaLoc - 1);
   content_ = QByteArray::fromBase64(base64Content);
   offset_ = 0;

   // set status OK
   setAttribute(QNetworkRequest::HttpStatusCodeAttribute, 200);
   setAttribute(QNetworkRequest::HttpReasonPhraseAttribute,
                QString::fromAscii("OK"));

   open(ReadOnly | Unbuffered);
   setHeader(QNetworkRequest::ContentLengthHeader, QVariant(content_.size()));

   QTimer::singleShot(0, this, SIGNAL(readyRead()) );
   QTimer::singleShot(0, this, SIGNAL(finished()) );
}

qint64 DataUriNetworkReply::bytesAvailable() const
{
   return content_.size() - offset_;
}

qint64 DataUriNetworkReply::readData(char *data, qint64 maxSize)
{
    if (offset_ >= content_.size())
        return -1;

    qint64 bytes = qMin(maxSize, (qint64)(content_.size() - offset_));
    ::memcpy(data, content_.constData() + offset_, bytes);
    offset_ += bytes;

    return bytes;
}


} // namespace desktop
