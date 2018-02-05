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

namespace {

bool isDuplicateZoomRequest(QElapsedTimer* pTimer)
{
   qint64 elapsed = pTimer->restart();
   return elapsed < 10;
}

} // end anonymous namespace

GwtWindow::GwtWindow(bool showToolbar,
                     bool adjustTitle,
                     QString name,
                     QUrl baseUrl,
                     QWidget* pParent) :
   BrowserWindow(showToolbar, adjustTitle, name, baseUrl, pParent),
   zoomLevel_(options().zoomLevel())
{
   // initialize zoom levels (synchronize with AppearancePreferencesPane.java)
   double levels[] = {
      0.25, 0.50, 0.75, 0.80, 0.90,
      1.00, 1.10, 1.25, 1.50, 1.75,
      2.00, 2.50, 3.00, 4.00, 5.00
   };
   
   for (double level : levels)
      zoomLevels_.push_back(level);
   
   lastZoomTimer_.start();
}

void GwtWindow::zoomActualSize()
{
   if (isDuplicateZoomRequest(&lastZoomTimer_))
      return;
   setZoomLevel(1);
   webView()->setZoomFactor(1);
}

void GwtWindow::zoomIn()
{
   if (isDuplicateZoomRequest(&lastZoomTimer_))
      return;
   
   // get next greatest value
   std::vector<double>::const_iterator it = std::upper_bound(
            zoomLevels_.begin(), zoomLevels_.end(), getZoomLevel());
   if (it != zoomLevels_.end())
   {
      setZoomLevel(*it);
      webView()->setZoomFactor(*it);
   }
}

void GwtWindow::zoomOut()
{
   if (isDuplicateZoomRequest(&lastZoomTimer_))
      return;
   
   // get next smallest value
   std::vector<double>::const_iterator it = std::lower_bound(
            zoomLevels_.begin(), zoomLevels_.end(), getZoomLevel());
   if (it != zoomLevels_.begin() && it != zoomLevels_.end())
   {
      setZoomLevel(*(it - 1));
      webView()->setZoomFactor(*(it - 1));
   }
}

void GwtWindow::finishLoading(bool succeeded)
{
   BrowserWindow::finishLoading(succeeded);

   if (!succeeded)
       return;

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
