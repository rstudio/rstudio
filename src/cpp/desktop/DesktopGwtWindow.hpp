/*
 * DesktopGwtWindow.hpp
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

#ifndef DESKTOP_GWT_WINDOW_HPP
#define DESKTOP_GWT_WINDOW_HPP

#include "DesktopBrowserWindow.hpp"

namespace desktop {

class GwtWindow : public BrowserWindow
{
    Q_OBJECT
public:
    explicit GwtWindow(bool showToolbar,
                       bool adjustTitle,
                       QUrl baseUrl = QUrl(),
                       QWidget *parent = NULL);

protected:
   virtual bool event(QEvent* pEvent);

private:
   virtual void onActivated()
   {
   }

};

} // namespace desktop

#endif // DESKTOP_GWT_WINDOW_HPP
