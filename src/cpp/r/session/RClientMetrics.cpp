/*
 * RClientMetrics.cpp
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

#include "RClientMetrics.hpp"

#include <iostream>

#include <core/Settings.hpp>

#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>

#include "graphics/RGraphicsDevice.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace client_metrics {

namespace {   
const char * const kConsoleWidth = "r.session.client_metrics.console-width";
const char * const kBuildConsoleWidth = "r.session.client_metrics.build-console-width";
const char * const kGraphicsWidth = "r.session.client_metrics.graphics-width";
const char * const kGraphicsHeight = "r.session.client_metrics.graphics-height";
const char * const kDevicePixelRatio = "r.session.client_metrics.device-pixel-ratio";

}   
   
RClientMetrics get()
{
   RClientMetrics metrics;
   metrics.consoleWidth = r::options::getOptionWidth();
   metrics.buildConsoleWidth = r::options::getBuildOptionWidth();
   metrics.graphicsWidth = graphics::device::getWidth();
   metrics.graphicsHeight = graphics::device::getHeight();
   metrics.devicePixelRatio = graphics::device::devicePixelRatio();
   return metrics;
}
   
void set(const RClientMetrics& metrics)
{
   // set console width (if it's within the valid range -- if it's not
   // then an error will be thrown)
   if (metrics.consoleWidth >= 10 && metrics.consoleWidth <= 10000)
      r::options::setOptionWidth(metrics.consoleWidth);
   if (metrics.buildConsoleWidth >= 10 && metrics.buildConsoleWidth <= 10000)
      r::options::setBuildOptionWidth(metrics.buildConsoleWidth);

   // set graphics size, however don't do anything if width or height is less
   // than or equal to 0) 
   // (means the graphics window is minimized)
   if (metrics.graphicsWidth > 0 && metrics.graphicsHeight > 0)
   {
      // enforce a minimum graphics size so we don't get display 
      // list redraw errors -- note that setting the device to a size 
      // which diverges from the actual client size will break locator
      // so we need to set the size small enough that there is no way 
      // it can reasonably be used for locator
      int width = std::max(metrics.graphicsWidth, 100);
      int height = std::max(metrics.graphicsHeight, 100);

      // enforce a maximum graphics size so we don't create a device
      // that is so large that it exhausts available memory
      width = std::min(width, 10000);
      height = std::min(height, 10000);

      // set device size
      graphics::device::setSize(width, height, metrics.devicePixelRatio);
   }
}
      
void save(Settings* pSettings)
{
   // get the client metrics
   RClientMetrics metrics = client_metrics::get();
   
   // save them
   pSettings->beginUpdate();
   pSettings->set(kConsoleWidth, metrics.consoleWidth);
   pSettings->set(kBuildConsoleWidth, metrics.buildConsoleWidth);
   pSettings->set(kGraphicsWidth, metrics.graphicsWidth);
   pSettings->set(kGraphicsHeight, metrics.graphicsHeight);
   pSettings->set(kDevicePixelRatio, metrics.devicePixelRatio);
   pSettings->endUpdate();
}
   
void restore(const Settings& settings)
{
   // read the client metrics (specify defaults to be defensive)
   RClientMetrics metrics;
   metrics.consoleWidth = settings.getInt(kConsoleWidth, 
                                          r::options::kDefaultWidth);
   metrics.buildConsoleWidth = settings.getInt(kBuildConsoleWidth,
                                          r::options::kDefaultWidth);
   metrics.graphicsWidth = settings.getInt(kGraphicsWidth,
                                           graphics::device::kDefaultWidth);
   
   metrics.graphicsHeight = settings.getInt(kGraphicsHeight,
                                            graphics::device::kDefaultHeight);
   metrics.devicePixelRatio = settings.getDouble(kDevicePixelRatio,
                                       graphics::device::kDefaultDevicePixelRatio);
   
   // set them
   set(metrics);
}
   

} // namespace client_metrics
} // namespace session
} // namespace r
} // namespace rstudio



