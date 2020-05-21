/*
 * DesktopSecondaryWindow.hpp
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

#ifndef DESKTOP_SECONDARY_WINDOW_HPP
#define DESKTOP_SECONDARY_WINDOW_HPP

#include <QMainWindow>
#include "DesktopBrowserWindow.hpp"

namespace rstudio {
namespace desktop {

class SecondaryWindow : public BrowserWindow
{
   Q_OBJECT
public:
   explicit SecondaryWindow(bool showToolbar, QString name, QUrl baseUrl,
                            QWidget* pParent = nullptr, WebPage *pOpener = nullptr,
                            bool allowExternalNavigate = false);
public Q_SLOTS:
   void onCloseWindowShortcut();

protected Q_SLOTS:
   void finishLoading(bool ok) override;
   virtual void manageCommandState();

private:
   QAction* back_;
   QAction* forward_;
   QAction* reload_;
   QAction* print_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SECONDARY_WINDOW_HPP
