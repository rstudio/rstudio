/*
 * RGraphicsDevDesc.hpp
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

#ifndef R_SESSION_GRAPHICS_DEV_DESC_HPP
#define R_SESSION_GRAPHICS_DEV_DESC_HPP

#include <Rinternals.h>

#include <R_ext/Boolean.h>
#define R_USE_PROTOTYPES 1
#include <R_ext/GraphicsEngine.h>

extern "C" {

struct DevDescVersion12
{
   double left;
   double right;
   double bottom;
   double top;
   double clipLeft;
   double clipRight;
   double clipBottom;
   double clipTop;
   double xCharOffset;
   double yCharOffset;
   double yLineBias;
   double ipr[2];
   double cra[2];
   double gamma;
   Rboolean canClip;
   Rboolean canChangeGamma;
   int canHAdj;
   double startps;
   int startcol;
   int startfill;
   int startlty;
   int startfont;
   double startgamma;
   void *deviceSpecific;
   Rboolean displayListOn;
   Rboolean canGenMouseDown;
   Rboolean canGenMouseMove;
   Rboolean canGenMouseUp;
   Rboolean canGenKeybd;
   Rboolean canGenIdle; // version 12
   Rboolean gettingEvent;

   void (*activate)(const pDevDesc );
   void (*circle)(double x, double y, double r, const pGEcontext gc, pDevDesc dd);
   void (*clip)(double x0, double x1, double y0, double y1, pDevDesc dd);
   void (*close)(pDevDesc dd);
   void (*deactivate)(pDevDesc );
   Rboolean (*locator)(double *x, double *y, pDevDesc dd);
   void (*line)(double x1, double y1, double x2, double y2,
       const pGEcontext gc, pDevDesc dd);
   void (*metricInfo)(int c, const pGEcontext gc,
             double* ascent, double* descent, double* width,
             pDevDesc dd);
   void (*mode)(int mode, pDevDesc dd);
   void (*newPage)(const pGEcontext gc, pDevDesc dd);
   void (*polygon)(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);
   void (*polyline)(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);
   void (*rect)(double x0, double y0, double x1, double y1,
       const pGEcontext gc, pDevDesc dd);


   // dev_Path added in version 8
   void (*path)(double *x, double *y,
                  int npoly, int *nper,
                  Rboolean winding,
                  const pGEcontext gc, pDevDesc dd);

   // dev_Raster and dev_Cap added in version 6
   void (*raster)(unsigned int *raster, int w, int h,
                double x, double y,
                double width, double height,
                double rot,
                Rboolean interpolate,
                const pGEcontext gc, pDevDesc dd);
   SEXP (*cap)(pDevDesc dd);

   void (*size)(double *left, double *right, double *bottom, double *top,
    pDevDesc dd);
   double (*strWidth)(const char *str, const pGEcontext gc, pDevDesc dd);
   void (*text)(double x, double y, const char *str, double rot,
    double hadj, const pGEcontext gc, pDevDesc dd);
   void (*onExit)(pDevDesc dd);
   SEXP (*getEvent)(SEXP, const char *);
   Rboolean (*newFrameConfirm)(pDevDesc dd);

   Rboolean hasTextUTF8; /* and strWidthUTF8 */
   void (*textUTF8)(double x, double y, const char *str, double rot,
        double hadj, const pGEcontext gc, pDevDesc dd);
   double (*strWidthUTF8)(const char *str, const pGEcontext gc, pDevDesc dd);
   Rboolean wantSymbolUTF8;
   Rboolean useRotatedTextInContour;

   // eventEnv and eventHelper added in version 7
   SEXP eventEnv;
   void (*eventHelper)(pDevDesc dd, int code);

   // holdFlush and have* added in version 9 (R 2.14)
   int (*holdflush)(pDevDesc dd, int level);
   int haveTransparency;
   int haveTransparentBg;
   int haveRaster;
   int haveCapture, haveLocator;

   char reserved[64];
};

} // extern "C"

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace dev_desc {

pDevDesc allocate(const DevDescVersion12& devDescVersion12);
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

} // namespace dev_desc
} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio



#endif // R_SESSION_GRAPHICS_DEV_DESC_HPP

