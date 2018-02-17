/*
 * DesktopWebProfile.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
         QObject* parent,
         const std::string& sharedSecret)
      : QWebEngineUrlRequestInterceptor(parent),
        sharedSecret_(sharedSecret)
   {
   }

   void interceptRequest(QWebEngineUrlRequestInfo& info) override
   {
      // TODO: do we want to set the shared secret on all requests?
      info.setHttpHeader(
               QByteArrayLiteral("X-Shared-Secret"),
               QByteArray::fromStdString(sharedSecret_));
   }

private:
   std::string sharedSecret_;
};

} // end anonymous namespace

WebProfile::WebProfile(QObject* parent)
   : QWebEngineProfile(parent)
{
   std::string sharedSecret = core::system::getenv("RS_SHARED_SECRET");
   setRequestInterceptor(new Interceptor(this, sharedSecret));
}

} // end namespace desktop
} // end namespace rstudio
