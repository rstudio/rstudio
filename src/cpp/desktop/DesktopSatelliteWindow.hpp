/*
 * DesktopSatelliteWindow.hpp
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

#ifndef DESKTOP_SATELLITE_WINDOW_HPP
#define DESKTOP_SATELLITE_WINDOW_HPP

#include <QMainWindow>
#include "DesktopGwtWindow.hpp"
#include "DesktopGwtCallback.hpp"
#include "DesktopWebPage.hpp"

#define SOURCE_WINDOW_PREFIX "_rstudio_satellite_source_window_"

namespace rstudio {
namespace desktop {

enum CloseStage
{
   CloseStageOpen,
   CloseStagePending,
   CloseStageAccepted
};

class MainWindow;

class SatelliteWindow : public GwtWindow
{
    Q_OBJECT
public:
    SatelliteWindow(MainWindow* pMainWindow, QString name, WebPage* opener);

Q_SIGNALS:

public Q_SLOTS:


protected Q_SLOTS:
   void finishLoading(bool ok) override;

protected:
   void closeEvent(QCloseEvent *event) override;
   void closeSatellite(QCloseEvent *event);

private:
   void onActivated() override;

private:
   GwtCallback gwtCallback_;
   CloseStage close_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SATELLITE_WINDOW_HPP
