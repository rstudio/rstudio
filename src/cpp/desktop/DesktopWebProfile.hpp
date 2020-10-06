/*
 * DesktopWebProfile.hpp
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

#ifndef RSTUDIO_DESKTOP_WEB_PROFILE_HPP
#define RSTUDIO_DESKTOP_WEB_PROFILE_HPP

#include <QWebEngineProfile>
#include <QWebEngineUrlRequestInterceptor>
#include <QWebEngineUrlRequestInfo>

namespace rstudio {
namespace desktop {

class WebProfile : public QWebEngineProfile
{
   Q_OBJECT

public:
   explicit WebProfile(const QUrl& baseUrl, QObject* parent = nullptr);
   void setBaseUrl(const QUrl& baseUrl);
   void onInterceptRequest(QWebEngineUrlRequestInfo& info);
   
Q_SIGNALS:
   void urlIntercepted(QUrl url, int type);

private:
   QScopedPointer<QWebEngineUrlRequestInterceptor> interceptor_;
   std::string sharedSecret_;
};

} // end namespace desktop
} // end namespace rstudio

#endif /* RSTUDIO_DESKTOP_WEB_PROFILE_HPP */
