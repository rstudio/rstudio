/*
 * RGraphicsDeviceContext.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_GRAPHICS_DEVICE_CONTEXT_HPP
#define R_GRAPHICS_DEVICE_CONTEXT_HPP

#include <Rinternals.h>

#include <R_ext/Boolean.h>

#define R_USE_PROTOTYPES 1
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {

struct DeviceContext
{
   DeviceContext(pDevDesc ownerDev) :
      pDeviceSpecific(nullptr),
      width(0),
      height(0),
      devicePixelRatio(1.0),
      dev(ownerDev)
   {
   }

   // platform specific device info
   void* pDeviceSpecific;

   // file info
   core::FilePath targetPath;
   int width;
   int height;
   double devicePixelRatio;

   // back pointer to owning device
   pDevDesc dev;
};

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_GRAPHICS_DEVICE_CONTEXT_HPP


