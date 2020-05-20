/*
 * DesktopGwtWindow.hpp
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

#ifndef DESKTOP_GWT_WINDOW_HPP
#define DESKTOP_GWT_WINDOW_HPP

#include "DesktopBrowserWindow.hpp"
#include "DesktopWebPage.hpp"

namespace rstudio {
namespace desktop {

class GwtWindow : public BrowserWindow
{
    Q_OBJECT
public:
   explicit GwtWindow(bool showToolbar,
                      bool adjustTitle,
                      QString name,
                      QUrl baseUrl = QUrl(),
                      QWidget *parent = nullptr,
                      WebPage* opener = nullptr,
                      bool isRemoteDesktop = false);

   const std::vector<double>& zoomLevels() const { return zoomLevels_; }

public Q_SLOTS:
   void onCloseWindowShortcut();

   void zoomActualSize();
   void setZoomLevel(double zoomLevel);
   void zoomIn();
   void zoomOut();

protected Q_SLOTS:
   void finishLoading(bool) override;

protected:
   bool event(QEvent* pEvent) override;

private:
   virtual void onActivated()
   {
   }

   std::vector<double> zoomLevels_;
   double zoomLevel_;
   QElapsedTimer lastZoomTimer_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_GWT_WINDOW_HPP
