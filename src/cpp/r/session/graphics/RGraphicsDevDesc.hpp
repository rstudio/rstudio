/*
 * RGraphicsDevDesc.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef R_SESSION_GRAPHICS_DEV_DESC_HPP
#define R_SESSION_GRAPHICS_DEV_DESC_HPP

#include <Rinternals.h>

#include <R_ext/Boolean.h>

#define R_USE_PROTOTYPES 1
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>

#include "RGraphicsDevDescVersions.hpp"

typedef DevDescVersion15 RSDevDesc;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace dev_desc {

pDevDesc allocate(const RSDevDesc& devDesc);
void setSize(pDevDesc pDD);
void setDeviceAttributes(pDevDesc pDev, pDevDesc pShadow);

/* Wrapper methods for graphics engine */
void activate(const pDevDesc dd);
void circle(double x, double y, double r, const pGEcontext gc, pDevDesc dd);
void clip(double x0, double x1, double y0, double y1, pDevDesc dd);
void close(pDevDesc dd);
void deactivate(pDevDesc dd);
Rboolean locator(double *x, double *y, pDevDesc dd);
void line(double x1, double y1, double x2, double y2,
          const pGEcontext gc, pDevDesc dd);
void metricInfo(int c, const pGEcontext gc,
                double* ascent, double* descent, double* width,
                pDevDesc dd);
void mode(int mode, pDevDesc dd);
void newPage(const pGEcontext gc, pDevDesc dd);
void polygon(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);
void polyline(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);
void rect(double x0, double y0, double x1, double y1,
          const pGEcontext gc, pDevDesc dd);
void path(double *x, double *y, 
          int npoly, int *nper,
          Rboolean winding,
          const pGEcontext gc, pDevDesc dd);
void raster(unsigned int *raster, int w, int h,
            double x, double y, 
            double width, double height,
            double rot, 
            Rboolean interpolate,
            const pGEcontext gc, pDevDesc dd);
SEXP cap(pDevDesc dd);
void size(double *left, double *right, double *bottom, double *top,
          pDevDesc dd);
double strWidth(const char *str, const pGEcontext gc, pDevDesc dd);
void text(double x, double y, const char *str, double rot,
          double hadj, const pGEcontext gc, pDevDesc dd);

SEXP setPattern(SEXP pattern, pDevDesc dd);
void releasePattern(SEXP ref, pDevDesc dd);

SEXP setClipPath(SEXP path, SEXP ref, pDevDesc dd);
void releaseClipPath(SEXP ref, pDevDesc dd);

SEXP setMask(SEXP path, SEXP ref, pDevDesc dd);
void releaseMask(SEXP ref, pDevDesc dd);

SEXP defineGroup(SEXP source, int op, SEXP destination, pDevDesc dd);
void useGroup(SEXP ref, SEXP trans, pDevDesc dd);
void releaseGroup(SEXP ref, pDevDesc dd);

void stroke(SEXP path, const pGEcontext gc, pDevDesc dd);
void fill(SEXP path, int rule, const pGEcontext gc, pDevDesc dd);
void fillStroke(SEXP path, int rule, const pGEcontext gc, pDevDesc dd);

SEXP capabilities(SEXP cap);

} // namespace dev_desc
} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio



#endif // R_SESSION_GRAPHICS_DEV_DESC_HPP

