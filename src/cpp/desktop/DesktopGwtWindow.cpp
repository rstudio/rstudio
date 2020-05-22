/*
 * DesktopGwtWindow.cpp
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

#include "DesktopGwtWindow.hpp"
#include "DesktopOptions.hpp"


namespace rstudio {
namespace desktop {

GwtWindow::GwtWindow(bool showToolbar,
                     bool adjustTitle,
                     QString name,
                     QUrl baseUrl,
                     QWidget* pParent,
                     WebPage* opener,
                     bool isRemoteDesktop) :
   BrowserWindow(showToolbar, adjustTitle, name, baseUrl, pParent, opener, isRemoteDesktop)
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
   options().setZoomLevel(1);
   webView()->setZoomFactor(1);
}

void GwtWindow::setZoomLevel(double zoomLevel)
{
   options().setZoomLevel(zoomLevel);
   webView()->setZoomFactor(zoomLevel);
}

void GwtWindow::zoomIn()
{
   // get next greatest value
   double zoomLevel = options().zoomLevel();
   auto it = std::upper_bound(zoomLevels_.begin(), zoomLevels_.end(), zoomLevel);
   if (it != zoomLevels_.end())
   {
      options().setZoomLevel(*it);
      webView()->setZoomFactor(*it);
   }
}

void GwtWindow::zoomOut()
{
   // get next smallest value
   double zoomLevel = options().zoomLevel();
   auto it = std::lower_bound(zoomLevels_.begin(), zoomLevels_.end(), zoomLevel);
   if (it != zoomLevels_.begin() && it != zoomLevels_.end())
   {
      options().setZoomLevel(*(it - 1));
      webView()->setZoomFactor(*(it - 1));
   }
}

void GwtWindow::finishLoading(bool succeeded)
{
   BrowserWindow::finishLoading(succeeded);
}

bool GwtWindow::event(QEvent* pEvent)
{
   if (pEvent->type() == QEvent::WindowActivate)
      onActivated();

   return BrowserWindow::event(pEvent);
}

void GwtWindow::onCloseWindowShortcut()
{
   // check to see if the window has desktop hooks (not all GWT windows do); if it does, check to
   // see whether it has a closeSourceDoc() command we should be executing instead
   webPage()->runJavaScript(
            QStringLiteral(
               "if (window.desktopHooks) "
               " window.desktopHooks.isCommandEnabled('closeSourceDoc');"
               "else false"),
            [&](QVariant closeSourceDocEnabled)
   {
      if (!closeSourceDocEnabled.toBool())
         close();
   });
}


} // namespace desktop
} // namespace rstudio
