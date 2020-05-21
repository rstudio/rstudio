/*
 * DesktopWebProfile.cpp
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

#include "DesktopWebProfile.hpp"

#include <QWebEngineUrlRequestInterceptor>

#include <core/system/Environment.hpp>

namespace rstudio {
namespace desktop {

namespace {

class Interceptor : public QWebEngineUrlRequestInterceptor
{
public:
   explicit Interceptor(
         WebProfile* parent,
         const QUrl& baseUrl,
         const std::string& sharedSecret)
      : QWebEngineUrlRequestInterceptor(parent),
        parent_(parent),
        sharedSecret_(sharedSecret),
        baseUrl_(baseUrl)
   {
   }

   void interceptRequest(QWebEngineUrlRequestInfo& info) override
   {
      // notify the parent of the intercept -- this is primarily done to
      // communicate some extra information about the incoming request
      // to WebPage, for use in acceptNavigationRequest()
      parent_->onInterceptRequest(info);
      
      if (info.requestUrl().authority() == baseUrl_.authority())
      {
         // The shared secret helps the session authenticate that the request actually came from the
         // desktop frame and not from some other application. To reduce the odds of the shared
         // secret leaking out by tagging along on other HTTP requests (which are not destined for
         // the R session), we only set the header when communicating with the authority established
         // for the R session. 
         info.setHttpHeader(
                  QByteArrayLiteral("X-Shared-Secret"),
                  QByteArray::fromStdString(sharedSecret_));
      }
   }

private:
   WebProfile* parent_;
   std::string sharedSecret_;
   QUrl baseUrl_;
};

} // end anonymous namespace

WebProfile::WebProfile(const QUrl& baseUrl, QObject* parent)
   : QWebEngineProfile(parent)
{
   sharedSecret_ = core::system::getenv("RS_SHARED_SECRET");
   setBaseUrl(baseUrl);
}

void WebProfile::setBaseUrl(const QUrl& baseUrl)
{
   interceptor_.reset(new Interceptor(this, baseUrl, sharedSecret_));
#if QT_VERSION >= QT_VERSION_CHECK(5, 13, 0)
   setUrlRequestInterceptor(interceptor_.data());
#else
   setRequestInterceptor(interceptor_.data());
#endif
}

void WebProfile::onInterceptRequest(QWebEngineUrlRequestInfo& info)
{
   emit urlIntercepted(info.requestUrl(), info.resourceType());
}

} // end namespace desktop
} // end namespace rstudio
