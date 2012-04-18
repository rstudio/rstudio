/*
 * DesktopGwtCallbackOwner.hpp
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

#ifndef DESKTOP_GWT_CALLBACK_OWNER_HPP
#define DESKTOP_GWT_CALLBACK_OWNER_HPP

#include <QWidget>
#include <QString>
#include <QWebPage>

namespace desktop {

class WebPage;

class GwtCallbackOwner
{
public:
   virtual ~GwtCallbackOwner() {}

   virtual QWidget* asWidget() = 0;
   virtual WebPage* webPage() = 0;
   virtual void postWebViewEvent(QEvent *event) = 0;
   virtual void triggerPageAction(QWebPage::WebAction action) = 0;
};

} // namespace desktop

#endif // DESKTOP_GWT_CALLBACK_OWNER_HPP
