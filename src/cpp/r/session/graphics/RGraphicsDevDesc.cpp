/*
 * RGraphicsDevDesc.cpp
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

#include "RGraphicsDevDesc.hpp"

#include <cstdlib>

#include <R_ext/RS.h>

// compatability structs for previous graphics engine versions
extern "C" {

struct DevDescVersion5
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

   // end of devDescUniversal

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
   char reserved[64];
};

struct DevDescVersion6
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
   char reserved[64];
};

struct DevDescVersion7
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

   char reserved[64];
};

struct DevDescVersion8
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

   char reserved[64];
};

} // extern C

namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace dev_desc {

namespace {

template <typename T>
void copyCommonMembers(const DevDescVersion9& sourceDevDesc,
                          T* pTargetDevDesc)
{
   pTargetDevDesc->left = sourceDevDesc.left;
   pTargetDevDesc->right = sourceDevDesc.right;
   pTargetDevDesc->bottom = sourceDevDesc.bottom;
   pTargetDevDesc->top = sourceDevDesc.top;
   pTargetDevDesc->clipLeft = sourceDevDesc.clipLeft;
   pTargetDevDesc->clipRight = sourceDevDesc.clipRight;
   pTargetDevDesc->clipBottom = sourceDevDesc.clipBottom;
   pTargetDevDesc->clipTop = sourceDevDesc.clipTop;
   pTargetDevDesc->xCharOffset = sourceDevDesc.xCharOffset;
   pTargetDevDesc->yCharOffset = sourceDevDesc.yCharOffset;
   pTargetDevDesc->yLineBias = sourceDevDesc.yLineBias;
   pTargetDevDesc->ipr[0] = sourceDevDesc.ipr[0];
   pTargetDevDesc->ipr[1] = sourceDevDesc.ipr[1];
   pTargetDevDesc->cra[0] = sourceDevDesc.cra[0];
   pTargetDevDesc->cra[1] = sourceDevDesc.cra[1];
   pTargetDevDesc->gamma = sourceDevDesc.gamma;
   pTargetDevDesc->canClip = sourceDevDesc.canClip;
   pTargetDevDesc->canChangeGamma = sourceDevDesc.canChangeGamma;
   pTargetDevDesc->canHAdj = sourceDevDesc.canHAdj;
   pTargetDevDesc->startps = sourceDevDesc.startps;
   pTargetDevDesc->startcol = sourceDevDesc.startcol;
   pTargetDevDesc->startfill = sourceDevDesc.startfill;
   pTargetDevDesc->startlty = sourceDevDesc.startlty;
   pTargetDevDesc->startfont = sourceDevDesc.startfont;
   pTargetDevDesc->startgamma = sourceDevDesc.startgamma;
   pTargetDevDesc->deviceSpecific = sourceDevDesc.deviceSpecific;
   pTargetDevDesc->displayListOn = sourceDevDesc.displayListOn;
   pTargetDevDesc->canGenMouseDown = sourceDevDesc.canGenMouseDown;
   pTargetDevDesc->canGenMouseMove = sourceDevDesc.canGenMouseMove;
   pTargetDevDesc->canGenMouseUp = sourceDevDesc.canGenMouseUp;
   pTargetDevDesc->canGenKeybd = sourceDevDesc.canGenKeybd;
   pTargetDevDesc->gettingEvent = sourceDevDesc.gettingEvent;
   pTargetDevDesc->activate = sourceDevDesc.activate;
   pTargetDevDesc->circle = sourceDevDesc.circle;
   pTargetDevDesc->clip = sourceDevDesc.clip;
   pTargetDevDesc->close = sourceDevDesc.close;
   pTargetDevDesc->deactivate = sourceDevDesc.deactivate;
   pTargetDevDesc->locator = sourceDevDesc.locator;
   pTargetDevDesc->line = sourceDevDesc.line;
   pTargetDevDesc->metricInfo = sourceDevDesc.metricInfo;
   pTargetDevDesc->mode = sourceDevDesc.mode;
   pTargetDevDesc->newPage = sourceDevDesc.newPage;
   pTargetDevDesc->polygon = sourceDevDesc.polygon;
   pTargetDevDesc->polyline = sourceDevDesc.polyline;
   pTargetDevDesc->rect = sourceDevDesc.rect;
   pTargetDevDesc->size = sourceDevDesc.size;
   pTargetDevDesc->strWidth = sourceDevDesc.strWidth;
   pTargetDevDesc->text = sourceDevDesc.text;
   pTargetDevDesc->onExit = sourceDevDesc.onExit;
   pTargetDevDesc->getEvent = sourceDevDesc.getEvent;
   pTargetDevDesc->newFrameConfirm = sourceDevDesc.newFrameConfirm;
   pTargetDevDesc->hasTextUTF8 = sourceDevDesc.hasTextUTF8;
   pTargetDevDesc->textUTF8 = sourceDevDesc.textUTF8;
   pTargetDevDesc->strWidthUTF8 = sourceDevDesc.strWidthUTF8;
   pTargetDevDesc->wantSymbolUTF8 = sourceDevDesc.wantSymbolUTF8;
   pTargetDevDesc->useRotatedTextInContour = sourceDevDesc.useRotatedTextInContour;

   // zero out reserved
   ::memset(pTargetDevDesc->reserved, 0, 64);
}

template <typename T>
T* allocAndInitCommonMembers(const DevDescVersion9& devDescVersion9)
{
   T* pDevDesc = (T*) std::calloc(1, sizeof(T));
   copyCommonMembers(devDescVersion9, pDevDesc);
   return pDevDesc;
}

} // anonymous namespace

pDevDesc allocate(const DevDescVersion9& devDescVersion9)
{
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 5:
   {
      DevDescVersion5* pDD = allocAndInitCommonMembers<DevDescVersion5>(
                                                          devDescVersion9);
      return (pDevDesc)pDD;
   }

   case 6:
   {
      DevDescVersion6* pDD = allocAndInitCommonMembers<DevDescVersion6>(
                                                            devDescVersion9);

      pDD->raster = devDescVersion9.raster;
      pDD->cap = devDescVersion9.cap;

      return (pDevDesc)pDD;
   }

   case 7:
   {
      DevDescVersion7* pDD = allocAndInitCommonMembers<DevDescVersion7>(
                                                            devDescVersion9);

      pDD->raster = devDescVersion9.raster;
      pDD->cap = devDescVersion9.cap;
      pDD->eventEnv = devDescVersion9.eventEnv;
      pDD->eventHelper = devDescVersion9.eventHelper;

      return (pDevDesc)pDD;
   }


   case 8:
   {
      DevDescVersion8* pDD = allocAndInitCommonMembers<DevDescVersion8>(
                                                            devDescVersion9);

      pDD->path = devDescVersion9.path;
      pDD->raster = devDescVersion9.raster;
      pDD->cap = devDescVersion9.cap;
      pDD->eventEnv = devDescVersion9.eventEnv;
      pDD->eventHelper = devDescVersion9.eventHelper;

      return (pDevDesc)pDD;
   }

   // NOTE: graphics device won't be initialized unless we confirm
   // that the current graphics engine version is v9 compatible
   case 9:
   default:
   {
      DevDescVersion9* pDD = (DevDescVersion9*) std::calloc(
                                             1, sizeof(DevDescVersion9));
      *pDD = devDescVersion9;

      return (pDevDesc)pDD;
   }

   }
}

void setSize(pDevDesc pDD)
{
   // get pointer to size function
   void (*pSizeFn)(double*, double*, double*, double*, pDevDesc);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 5:
      pSizeFn = ((DevDescVersion5*)pDD)->size;
      break;
   case 6:
      pSizeFn = ((DevDescVersion6*)pDD)->size;
      break;
   case 7:
      pSizeFn = ((DevDescVersion7*)pDD)->size;
      break;
   case 8:
      pSizeFn = ((DevDescVersion8*)pDD)->size;
      break;
   case 9:
   default:
      pSizeFn = ((DevDescVersion9*)pDD)->size;
      break;
   }

   // set size
   pSizeFn(&(pDD->left),
           &(pDD->right),
           &(pDD->bottom),
           &(pDD->top),
           pDD);

   // set clip region
   pSizeFn(&(pDD->clipLeft),
           &(pDD->clipRight),
           &(pDD->clipBottom),
           &(pDD->clipTop),
           pDD);
}

void path(double *x,
          double *y,
          int npoly,
          int *nper,
          Rboolean winding,
          const pGEcontext gc,
          pDevDesc dd)
{
   // get pointer to path function
   void (*pPathFn)(double*, double*, int, int*, Rboolean, const pGEcontext,
                   pDevDesc);

   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 8:
      pPathFn = ((DevDescVersion8*)dd)->path;
      break;
   case 9:
   default:
      pPathFn = ((DevDescVersion9*)dd)->path;
      break;
   }

   // call it
   pPathFn(x, y, npoly, nper, winding, gc, dd);
}

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
            pDevDesc dd)
{
   // get pointer to raster function
   void (*pRasterFn)(unsigned int*, int, int, double, double, double,
                     double, double, Rboolean, const pGEcontext, pDevDesc);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 6:
      pRasterFn = ((DevDescVersion6*)dd)->raster;
      break;
   case 7:
      pRasterFn = ((DevDescVersion7*)dd)->raster;
      break;
   case 8:
      pRasterFn = ((DevDescVersion8*)dd)->raster;
      break;
   case 9:
   default:
      pRasterFn = ((DevDescVersion9*)dd)->raster;
      break;
   }

   // call it
   pRasterFn(raster, w, h, x, y, width, height, rot, interpolate, gc, dd);
}

double strWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   // get pointer to strWidth function
   double (*pStrWidthFn)(const char*, const pGEcontext, pDevDesc);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 5:
      pStrWidthFn = ((DevDescVersion5*)dev)->strWidth;
      break;
   case 6:
      pStrWidthFn = ((DevDescVersion6*)dev)->strWidth;
      break;
   case 7:
      pStrWidthFn = ((DevDescVersion7*)dev)->strWidth;
      break;
   case 8:
      pStrWidthFn = ((DevDescVersion8*)dev)->strWidth;
      break;
   case 9:
   default:
      pStrWidthFn = ((DevDescVersion9*)dev)->strWidth;
      break;
   }

   // call it
   return pStrWidthFn(str, gc, dev);
}

void text(double x,
          double y,
          const char *str,
          double rot,
          double hadj,
          const pGEcontext gc,
          pDevDesc dev)
{
   // get pointer to text function
   void (*pTextFn)(double, double, const char*, double, double,
                   const pGEcontext, pDevDesc);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 5:
      pTextFn = ((DevDescVersion5*)dev)->text;
      break;
   case 6:
      pTextFn = ((DevDescVersion6*)dev)->text;
      break;
   case 7:
      pTextFn = ((DevDescVersion7*)dev)->text;
      break;
   case 8:
      pTextFn = ((DevDescVersion8*)dev)->text;
      break;
   case 9:
   default:
      pTextFn = ((DevDescVersion9*)dev)->text;
      break;
   }

   // call it
   pTextFn(x, y, str, rot, hadj, gc, dev);
}


} // namespace dev_desc
} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r

