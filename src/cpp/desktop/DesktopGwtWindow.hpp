/*
 * DesktopGwtWindow.hpp
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

#ifndef DESKTOP_GWT_WINDOW_HPP
#define DESKTOP_GWT_WINDOW_HPP

#include "DesktopBrowserWindow.hpp"

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
                      QWidget *parent = NULL);

   const std::vector<double>& zoomLevels() const { return zoomLevels_; }

public slots:
   void zoomIn();
   void zoomOut();

protected slots:
   virtual void onJavaScriptWindowObjectCleared();

protected:
   virtual bool event(QEvent* pEvent);
   virtual double getZoomLevel();
   virtual void setZoomLevel(double zoomLevel);

private:
   virtual void onActivated()
   {
   }

   std::vector<double> zoomLevels_;
   double zoomLevel_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_GWT_WINDOW_HPP
