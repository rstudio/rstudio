/*
 * DesktopNetworkReply.cpp
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

#include "DesktopNetworkReply.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/asio/io_service.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/Response.hpp>

#ifdef _WIN32
#include <core/http/NamedPipeAsyncClient.hpp>
#else
#include <core/http/LocalStreamAsyncClient.hpp>
#endif

#include <QTimer>

#include "DesktopNetworkIOService.hpp"

using namespace core;

namespace desktop {

struct NetworkReply::Impl
{
   Impl(const std::string& localPeer)
 #ifdef _WIN32
      : pClient(new http::NamedPipeAsyncClient(ioService(),
                                               localPeer,
                                               retryProfile())),
 #else
      : pClient(new http::LocalStreamAsyncClient(ioService(),
                                                 FilePath(localPeer),
                                                 retryProfile())),
 #endif
        replyReadOffset(0)
   {
   }
 #ifdef _WIN32
    boost::shared_ptr<http::NamedPipeAsyncClient> pClient;
 #else
    boost::shared_ptr<http::LocalStreamAsyncClient> pClient;
 #endif
   QByteArray replyData;
   qint64 replyReadOffset;

private:
   static http::ConnectionRetryProfile retryProfile()
   {
      return http::ConnectionRetryProfile(
               boost::posix_time::seconds(10),
               boost::posix_time::milliseconds(50));
   }
};


NetworkReply::NetworkReply(const std::string& localPeer,
                           QNetworkAccessManager::Operation op,
                           const QNetworkRequest& req,
                           QIODevice* outgoingData,
                           QObject *parent)
   : QNetworkReply(parent), pImpl_(new Impl(localPeer))
{   
   // set our attributes
   setOperation(op);
   setRequest(req);
   setUrl(req.url());

   // build http request
   http::Request request;
   switch(op)
   {
   case QNetworkAccessManager::HeadOperation:
      request.setMethod("HEAD");
      break;
   case QNetworkAccessManager::GetOperation:
      request.setMethod("GET");
      break;
   case QNetworkAccessManager::PutOperation:
      request.setMethod("PUT");
      break;
   case QNetworkAccessManager::PostOperation:
      request.setMethod("POST");
      break;
   case QNetworkAccessManager::DeleteOperation:
      request.setMethod("DELETE");
      break;
   case QNetworkAccessManager::CustomOperation:
      {
      QVariant custom = req.attribute(QNetworkRequest::CustomVerbAttribute);
      if (!custom.isNull())
         request.setMethod(custom.toString().toStdString());
      break;
      }
    case QNetworkAccessManager::UnknownOperation:
      LOG_WARNING_MESSAGE("Unknown operation passed to createRequest");
      break;
   }

   // uri
   std::string uri = req.url().path().toStdString();
   if (req.url().hasQuery())
   {
      uri.append("?");
      QByteArray queryString = req.url().encodedQuery();
      uri.append(queryString.begin(), queryString.end());
   }
   request.setUri(uri);

   // host
   request.setHost("127.0.0.1");

   // headers
   QList<QByteArray> headers = req.rawHeaderList();
   for (int i=0; i<headers.size(); i++)
   {
      QByteArray name = headers.at(i);
      QByteArray value = req.rawHeader(name);
      request.setHeader(std::string(name.begin(), name.end()),
                        std::string(value.begin(), value.end()));
   }

   // body
   if (outgoingData != NULL)
   {
      QByteArray postData = outgoingData->readAll();
      request.setBody(std::string(postData.begin(), postData.end()));
   }

   // set the request
   pImpl_->pClient->request().assign(request);

   // execute and bind to response handlers
   pImpl_->pClient->execute(boost::bind(&NetworkReply::onResponse, this, _1),
                            boost::bind(&NetworkReply::onError, _1));
}

NetworkReply::~NetworkReply()
{
   try
   {
      pImpl_->pClient->close();
   }
   catch(...)
   {
   }
}


qint64 NetworkReply::bytesAvailable() const
{
   // check for bytes available
   qint64 avail = pImpl_->replyData.size() - pImpl_->replyReadOffset;

   // Qt will never call readData unless you tell it that at least
   // 512 bytes are available
   return std::max(avail, (qint64)512);
}

bool NetworkReply::isSequential() const
{
   return true;
}

void NetworkReply::abort()
{
}


qint64 NetworkReply::readData(char *data, qint64 maxSize)
{  
   if (pImpl_->replyReadOffset >= pImpl_->replyData.size())
      return -1;

   qint64 bytesToRead = qMin(maxSize, pImpl_->replyData.size() -
                                      pImpl_->replyReadOffset);

   ::memcpy(data,
            pImpl_->replyData.constData() + pImpl_->replyReadOffset,
            bytesToRead);

   pImpl_->replyReadOffset += bytesToRead;

   if (pImpl_->replyReadOffset >= pImpl_->replyData.size())
      QTimer::singleShot(0, this, SIGNAL(finished()));
   else
      QTimer::singleShot(0, this, SIGNAL(readyRead()));

   return bytesToRead;
}


void NetworkReply::onResponse(const http::Response& response)
{
   // call open on the QIODevice
   open(ReadOnly | Unbuffered);

   // set http status and reason codes
   setAttribute(QNetworkRequest::HttpStatusCodeAttribute,
                response.statusCode());
   setAttribute(QNetworkRequest::HttpReasonPhraseAttribute,
                QString::fromStdString(response.statusMessage()));

   // set headers
   BOOST_FOREACH(const http::Header& header, response.headers())
   {
      QByteArray name = QByteArray(header.name.c_str());
      QByteArray value = QByteArray(header.value.c_str());
      setRawHeader(name, value);
   }

   // set body / content-length
   const std::string& body = response.body();
   if (!body.empty())
   {
      setHeader(QNetworkRequest::ContentLengthHeader, (uint)body.length());
      pImpl_->replyData.append(body.data(), body.length());
      pImpl_->replyReadOffset = 0;
   }

   // notify listeners that data is ready
   QTimer::singleShot(0, this, SIGNAL(readyRead()));

   // set finished flag
   setFinished(true);
}

void NetworkReply::onError(const Error& error)
{
   if (error.code() != boost::asio::error::operation_aborted)
   {
      LOG_ERROR(error);
   }
}

} // namespace desktop
