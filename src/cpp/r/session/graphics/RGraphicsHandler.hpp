/*
 * RGraphicsHandler.hpp
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

#ifndef R_GRAPHICS_HANDLER_HPP
#define R_GRAPHICS_HANDLER_HPP

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include "RGraphicsDevDesc.hpp"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {


void installShadowHandler();
void installCairoHandler();

struct DeviceContext
{
   DeviceContext(pDevDesc ownerDev) :
         pDeviceSpecific(nullptr),
         width(0),
         height(0),
         devicePixelRatio(1.0),
         dev(ownerDev) {}

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

extern DeviceContext* (*allocate)(pDevDesc dev);
extern void (*destroy)(DeviceContext* pDC);

extern bool (*initialize)(int width,
                          int height,
                          double devicePixelRatio,
                          DeviceContext* pDC);



extern void (*setSize)(pDevDesc pDev);
extern void (*setDeviceAttributes)(pDevDesc pDev);

extern void (*onBeforeAddDevice)(DeviceContext* pDC);
extern void (*onAfterAddDevice)(DeviceContext* pDC);

extern core::Error (*writeToPNG)(const core::FilePath& targetPath,
                                 DeviceContext* pDC);

extern void (*circle)(double x,
                      double y,
                      double r,
                      const pGEcontext gc,
                      pDevDesc dev);

extern void (*line)(double x1,
                    double y1,
                    double x2,
                    double y2,
                    const pGEcontext gc,
                    pDevDesc dev);

extern void (*polygon)(int n,
                       double *x,
                       double *y,
                       const pGEcontext gc,
                       pDevDesc dev);

extern void (*polyline)(int n,
                        double *x,
                        double *y,
                        const pGEcontext gc,
                        pDevDesc dev);

extern void (*rect)(double x0,
                    double y0,
                    double x1,
                    double y1,
                    const pGEcontext gc,
                    pDevDesc dev);

extern void (*path)(double *x,
                    double *y,
                    int npoly,
                    int *nper,
                    Rboolean winding,
                    const pGEcontext gc,
                    pDevDesc dd);

extern void (*raster)(unsigned int *raster,
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

extern SEXP (*cap)(pDevDesc dd);

extern void (*metricInfo)(int c,
                          const pGEcontext gc,
                          double* ascent,
                          double* descent,
                          double* width,
                          pDevDesc dev);

extern double (*strWidth)(const char *str, const pGEcontext gc, pDevDesc dev);

extern void (*text)(double x,
                    double y,
                    const char *str,
                    double rot,
                    double hadj,
                    const pGEcontext gc,
                    pDevDesc dev);

extern void (*clip)(double x0, double x1, double y0, double y1, pDevDesc dev);

extern void (*newPage)(const pGEcontext gc, pDevDesc dev);

extern void (*mode)(int mode, pDevDesc dev);

extern void (*onBeforeExecute)(DeviceContext* pDC);

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_GRAPHICS_HANDLER_HPP

