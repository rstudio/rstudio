/*
 * DesktopGwtWindow.cpp
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

#include "DesktopGwtWindow.hpp"
#include "DesktopOptions.hpp"


namespace rstudio {
namespace desktop {

GwtWindow::GwtWindow(bool showToolbar,
                     bool adjustTitle,
                     QString name,
                     QUrl baseUrl,
                     QWidget* pParent) :
   BrowserWindow(showToolbar, adjustTitle, name, baseUrl, pParent),
   zoomLevel_(options().zoomLevel())
{
   // initialize zoom levels
   zoomLevels_.push_back(1.0);
   zoomLevels_.push_back(1.1);
   zoomLevels_.push_back(1.20);
   zoomLevels_.push_back(1.30);
   zoomLevels_.push_back(1.40);
   zoomLevels_.push_back(1.50);
   zoomLevels_.push_back(1.75);
   zoomLevels_.push_back(2.00);
}

void GwtWindow::zoomIn()
{
   // get next greatest value
   std::vector<double>::const_iterator it = std::upper_bound(
            zoomLevels_.begin(), zoomLevels_.end(), getZoomLevel());
   if (it != zoomLevels_.end())
   {
      setZoomLevel(*it);
      webView()->reload();
   }
}

void GwtWindow::zoomOut()
{
   // get next smallest value
   std::vector<double>::const_iterator it = std::lower_bound(
            zoomLevels_.begin(), zoomLevels_.end(), getZoomLevel());
   if (it != zoomLevels_.begin() && it != zoomLevels_.end())
   {
      setZoomLevel(*(it-1));
      webView()->reload();
   }
}

void GwtWindow::onJavaScriptWindowObjectCleared()
{
   if (getZoomLevel() != webView()->dpiAwareZoomFactor())
      webView()->setDpiAwareZoomFactor(getZoomLevel());
}

double GwtWindow::getZoomLevel()
{
   return zoomLevel_;
}

void GwtWindow::setZoomLevel(double zoomLevel)
{
   zoomLevel_ = zoomLevel;
}

bool GwtWindow::event(QEvent* pEvent)
{
   if (pEvent->type() == QEvent::WindowActivate)
      onActivated();

   return BrowserWindow::event(pEvent);
}


} // namespace desktop
} // namespace rstudio
