/*
 * RGraphicsHandler.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_GRAPHICS_HANDLER_HPP
#define R_GRAPHICS_HANDLER_HPP

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "RGraphicsDevDesc.hpp"

namespace core {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {
namespace handler {



struct DeviceContext
{
   DeviceContext(pDevDesc ownerDev) :
         pDeviceSpecific(NULL),
         width(0),
         height(0),
         holdlevel(0),
         dev(ownerDev) {}

   // platform specific device info
   void* pDeviceSpecific;

   // file info
   core::FilePath targetPath;
   int width;
   int height;

   // hold-level
   int holdlevel;

   // back pointer to owning device
   pDevDesc dev;
};

DeviceContext* allocate(pDevDesc dev);
void destroy(DeviceContext* pDC);


bool initialize(const core::FilePath& filePath,
                int width,
                int height,
                bool displayListOn,
                DeviceContext* pDC);

inline bool initialize(int width,
                       int height,
                       bool displayListOn,
                       DeviceContext* pDC)
{
   return initialize(core::FilePath(), width, height, displayListOn, pDC);
}


void setSize(pDevDesc pDev);
void setDeviceAttributes(pDevDesc pDev);

void onBeforeAddInteractiveDevice(DeviceContext* pDC);
void onAfterAddInteractiveDevice(DeviceContext* pDC);

bool resyncDisplayListBeforeWriteToPNG();

core::Error writeToPNG(const core::FilePath& targetPath,
                       DeviceContext* pDC,
                       bool keepContextAlive);

void circle(double x,
            double y,
            double r,
            const pGEcontext gc,
            pDevDesc dev);

void line(double x1,
          double y1,
          double x2,
          double y2,
          const pGEcontext gc,
          pDevDesc dev);


void polygon(int n,
             double *x,
             double *y,
             const pGEcontext gc,
             pDevDesc dev);

void polyline(int n,
              double *x,
              double *y,
              const pGEcontext gc,
              pDevDesc dev);

void rect(double x0,
          double y0,
          double x1,
          double y1,
          const pGEcontext gc,
          pDevDesc dev);

void path(double *x,
          double *y,
          int npoly,
          int *nper,
          Rboolean winding,
          const pGEcontext gc,
          pDevDesc dd);

void raster(unsigned int *raster,
            int w,
            int h,
            double x,
            double y,
            double width,
            double height,
            double rot,
            Rboolean interpolate,
            const pGEcontext gc,
            pDevDesc dd);

SEXP cap(pDevDesc dd);

void metricInfo(int c,
                const pGEcontext gc,
                double* ascent,
                double* descent,
                double* width,
                pDevDesc dev);

double strWidth(const char *str, const pGEcontext gc, pDevDesc dev);

void text(double x,
          double y,
          const char *str,
          double rot,
          double hadj,
          const pGEcontext gc,
          pDevDesc dev);

void clip(double x0, double x1, double y0, double y1, pDevDesc dev);

void newPage(const pGEcontext gc, pDevDesc dev);

void mode(int mode, pDevDesc dev);

void onBeforeExecute(DeviceContext* pDC);

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r


#endif // R_GRAPHICS_HANDLER_HPP

