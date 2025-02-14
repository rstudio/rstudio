/*
 * RGraphicsHandler.hpp
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

#ifndef R_GRAPHICS_HANDLER_HPP
#define R_GRAPHICS_HANDLER_HPP

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include "RGraphicsDeviceContext.hpp"

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

extern DeviceContext* (*allocate)(pDevDesc dev);
extern void (*destroy)(DeviceContext* pDC);

extern bool (*initialize)(int width,
                          int height,
                          double devicePixelRatio,
                          DeviceContext* pDC);

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

extern void (*size)(double* left,
                    double* right,
                    double* bottom,
                    double* top,
                    pDevDesc dev);

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

// below added in version 14 (R 4.1.0)
extern SEXP (*setPattern)(SEXP pattern, pDevDesc dd);
extern void (*releasePattern)(SEXP ref, pDevDesc dd);

extern SEXP (*setClipPath)(SEXP path, SEXP ref, pDevDesc dd);
extern void (*releaseClipPath)(SEXP ref, pDevDesc dd);

extern SEXP (*setMask)(SEXP path, SEXP ref, pDevDesc dd);
extern void (*releaseMask)(SEXP ref, pDevDesc dd);

extern SEXP (*defineGroup)(SEXP source, int op, SEXP destination, pDevDesc dd);
extern void (*useGroup)(SEXP ref, SEXP trans, pDevDesc dd);
extern void (*releaseGroup)(SEXP ref, pDevDesc dd);

extern void (*stroke)(SEXP path, const pGEcontext gc, pDevDesc dd);
extern void (*fill)(SEXP path, int rule, const pGEcontext gc, pDevDesc dd);
extern void (*fillStroke)(SEXP path, int rule, const pGEcontext gc, pDevDesc dd);

extern SEXP (*capabilities)(SEXP cap);

extern void (*glyph)(int n, int *glyphs, double *x, double *y, 
                     SEXP font, double size,
                     int colour, double rot, pDevDesc dd);

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_GRAPHICS_HANDLER_HPP

