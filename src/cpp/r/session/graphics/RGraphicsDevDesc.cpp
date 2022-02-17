/*
 * RGraphicsDevDesc.cpp
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

#include "RGraphicsDevDesc.hpp"

#include <cstdlib>

#include <R_ext/RS.h>

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace dev_desc {

namespace {

// this template is used to copy graphics device parameters
// common to all graphics device versions; newly-added
// members should be initialized explicitly separately
template <typename T>
void copyCommonMembers(const RSDevDesc& sourceDevDesc, T* pTargetDevDesc)
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
T* allocAndInitCommonMembers(const RSDevDesc& devDesc)
{
   T* pDevDesc = (T*) std::calloc(1, sizeof(T));
   copyCommonMembers(devDesc, pDevDesc);
   return pDevDesc;
}

} // anonymous namespace

pDevDesc allocate(const RSDevDesc& devDesc)
{
   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   
   case 5:
   {
      DevDescVersion5* pDD = allocAndInitCommonMembers<DevDescVersion5>(
                                                          devDesc);
      return (pDevDesc)pDD;
   }

   case 6:
   {
      DevDescVersion6* pDD = allocAndInitCommonMembers<DevDescVersion6>(
                                                            devDesc);

      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;

      return (pDevDesc)pDD;
   }

   case 7:
   {
      DevDescVersion7* pDD = allocAndInitCommonMembers<DevDescVersion7>(
                                                            devDesc);

      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;
      pDD->eventEnv = devDesc.eventEnv;
      pDD->eventHelper = devDesc.eventHelper;

      return (pDevDesc)pDD;
   }

   case 8:
   {
      DevDescVersion8* pDD = allocAndInitCommonMembers<DevDescVersion8>(
                                                            devDesc);

      pDD->path = devDesc.path;
      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;
      pDD->eventEnv = devDesc.eventEnv;
      pDD->eventHelper = devDesc.eventHelper;

      return (pDevDesc)pDD;
   }

   case 9:
   case 10:
   case 11:
   {
      DevDescVersion9* pDD = allocAndInitCommonMembers<DevDescVersion9>(
                                                            devDesc);

      pDD->path = devDesc.path;
      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;
      pDD->eventEnv = devDesc.eventEnv;
      pDD->eventHelper = devDesc.eventHelper;
      
      pDD->holdflush = devDesc.holdflush;
      pDD->haveTransparency = devDesc.haveTransparency;
      pDD->haveTransparentBg = devDesc.haveTransparentBg;
      pDD->haveRaster = devDesc.haveRaster;
      pDD->haveCapture = devDesc.haveCapture;
      pDD->haveLocator = devDesc.haveLocator;

      return (pDevDesc)pDD;
   }
      
   case 12:
   case 13:
   {
      DevDescVersion12* pDD =
            allocAndInitCommonMembers<DevDescVersion12>(devDesc);

      pDD->path = devDesc.path;
      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;
      pDD->eventEnv = devDesc.eventEnv;
      pDD->eventHelper = devDesc.eventHelper;
      
      pDD->holdflush = devDesc.holdflush;
      pDD->haveTransparency = devDesc.haveTransparency;
      pDD->haveTransparentBg = devDesc.haveTransparentBg;
      pDD->haveRaster = devDesc.haveRaster;
      pDD->haveCapture = devDesc.haveCapture;
      pDD->haveLocator = devDesc.haveLocator;

      return (pDevDesc) pDD;
   }
      
   case 14:
   {
      DevDescVersion14* pDD =
            allocAndInitCommonMembers<DevDescVersion14>(devDesc);

      pDD->path = devDesc.path;
      pDD->raster = devDesc.raster;
      pDD->cap = devDesc.cap;
      pDD->eventEnv = devDesc.eventEnv;
      pDD->eventHelper = devDesc.eventHelper;
      
      pDD->holdflush = devDesc.holdflush;
      pDD->haveTransparency = devDesc.haveTransparency;
      pDD->haveTransparentBg = devDesc.haveTransparentBg;
      pDD->haveRaster = devDesc.haveRaster;
      pDD->haveCapture = devDesc.haveCapture;
      pDD->haveLocator = devDesc.haveLocator;
      
      pDD->setPattern = devDesc.setPattern;
      pDD->releasePattern = devDesc.releasePattern;
      pDD->setClipPath = devDesc.setClipPath;
      pDD->releaseClipPath = devDesc.releaseClipPath;
      pDD->setMask = devDesc.setMask;
      pDD->releaseMask = devDesc.releaseMask;

      return (pDevDesc) pDD;
   }

   case 15:
   {
       DevDescVersion15* pDD =
               allocAndInitCommonMembers<DevDescVersion15>(devDesc);

       pDD->path = devDesc.path;
       pDD->raster = devDesc.raster;
       pDD->cap = devDesc.cap;
       pDD->eventEnv = devDesc.eventEnv;
       pDD->eventHelper = devDesc.eventHelper;

       pDD->holdflush = devDesc.holdflush;
       pDD->haveTransparency = devDesc.haveTransparency;
       pDD->haveTransparentBg = devDesc.haveTransparentBg;
       pDD->haveRaster = devDesc.haveRaster;
       pDD->haveCapture = devDesc.haveCapture;
       pDD->haveLocator = devDesc.haveLocator;

       pDD->setPattern = devDesc.setPattern;
       pDD->releasePattern = devDesc.releasePattern;
       pDD->setClipPath = devDesc.setClipPath;
       pDD->releaseClipPath = devDesc.releaseClipPath;
       pDD->setMask = devDesc.setMask;
       pDD->releaseMask = devDesc.releaseMask;

       pDD->defineGroup = devDesc.defineGroup;
       pDD->useGroup = devDesc.useGroup;
       pDD->releaseGroup = devDesc.releaseGroup;
       pDD->stroke = devDesc.stroke;
       pDD->fill = devDesc.fill;
       pDD->fillStroke = devDesc.fillStroke;
       pDD->capabilities = devDesc.capabilities;

       return (pDevDesc) pDD;
   }
      
   default:
   {
      DevDescVersion15* pDD =
            (DevDescVersion15*) std::calloc(1, sizeof(DevDescVersion15));
      *pDD = devDesc;
      return (pDevDesc) pDD;
   }

   }
}

namespace {

template <typename T>
void setCommonDeviceAttributes(T pDev, T pShadow)
{
   pDev->left = pShadow->left;
   pDev->top = pShadow->top;
   pDev->right = pShadow->right;
   pDev->bottom = pShadow->bottom;
   pDev->clipLeft = pShadow->clipLeft;
   pDev->clipTop = pShadow->clipTop;
   pDev->clipRight = pShadow->clipRight;
   pDev->clipBottom = pShadow->clipBottom;
   
   pDev->cra[0] = pShadow->cra[0];
   pDev->cra[1] = pShadow->cra[1];
   pDev->startps = pShadow->startps;
   pDev->ipr[0] = pShadow->ipr[0];
   pDev->ipr[1] = pShadow->ipr[1];
   pDev->xCharOffset = pShadow->xCharOffset;
   pDev->yCharOffset = pShadow->yCharOffset;
   pDev->yLineBias = pShadow->yLineBias;

   pDev->canClip = pShadow->canClip;
   pDev->canHAdj = pShadow->canHAdj;
   pDev->canChangeGamma = pShadow->canChangeGamma;
   pDev->startcol = pShadow->startcol;
   pDev->startfill = pShadow->startfill;
   pDev->startlty = pShadow->startlty;
   pDev->startfont = pShadow->startfont;
   pDev->startps = pShadow->startps;
   pDev->startgamma = pShadow->startgamma;
   pDev->displayListOn = TRUE;

   // no support for events yet
   pDev->canGenMouseDown = FALSE;
   pDev->canGenMouseMove = FALSE;
   pDev->canGenMouseUp = FALSE;
   pDev->canGenKeybd = FALSE;
   pDev->gettingEvent = FALSE;
}

} // end anonymous namespace

void setDeviceAttributes(pDevDesc pDev, pDevDesc pShadow)
{
   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   
   case 5:
      setCommonDeviceAttributes((DevDescVersion5*) pDev, (DevDescVersion5*) pShadow);
      break;
      
   case 6:
      setCommonDeviceAttributes((DevDescVersion6*) pDev, (DevDescVersion6*) pShadow);
      break;
      
   case 7:
      setCommonDeviceAttributes((DevDescVersion7*) pDev, (DevDescVersion7*) pShadow);
      break;
      
   case 8:
      setCommonDeviceAttributes((DevDescVersion8*) pDev, (DevDescVersion8*) pShadow);
      break;
      
   case 9:
   case 10:
   case 11:
      setCommonDeviceAttributes((DevDescVersion9*) pDev, (DevDescVersion9*) pShadow);
      break;
      
   case 12:
   case 13:
   {
      setCommonDeviceAttributes((DevDescVersion12*) pDev, (DevDescVersion12*) pShadow);
      
      auto* pLhs = (DevDescVersion12*) pDev;
      auto* pRhs = (DevDescVersion12*) pShadow;
      
      pLhs->canGenIdle = pRhs->canGenIdle;
      
      break;
   }
      
   case 14:
   case 15:

   default:
   {
      setCommonDeviceAttributes((DevDescVersion15*) pDev, (DevDescVersion15*) pShadow);

      auto* pLhs = (DevDescVersion15*) pDev;
      auto* pRhs = (DevDescVersion15*) pShadow;
      
      pLhs->canGenIdle      = pRhs->canGenIdle;
      
      break;
   }
      
   }
}

void activate(const pDevDesc dd)
{
   // get pointer to activate function
   void (*pActivateFn)(const pDevDesc);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pActivateFn = ((DevDescVersion5*)dd)->activate;
      break;
   case 6:
      pActivateFn = ((DevDescVersion6*)dd)->activate;
      break;
   case 7:
      pActivateFn = ((DevDescVersion7*)dd)->activate;
      break;
   case 8:
      pActivateFn = ((DevDescVersion8*)dd)->activate;
      break;
   case 9:
   case 10:
   case 11:
      pActivateFn = ((DevDescVersion9*)dd)->activate;
      break;
   case 12:
   case 13:
      pActivateFn = ((DevDescVersion12*)dd)->activate;
      break;
   case 14:
   case 15:
   default:
      pActivateFn = ((DevDescVersion15*)dd)->activate;
      break;
   }

   // call it
   pActivateFn(dd);
}

void circle(double x, double y, double r, const pGEcontext gc, pDevDesc dd)
{
   // get pointer to circle function
   void (*pCircleFn)(double x, double y, double r, const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pCircleFn = ((DevDescVersion5*)dd)->circle;
      break;
   case 6:
      pCircleFn = ((DevDescVersion6*)dd)->circle;
      break;
   case 7:
      pCircleFn = ((DevDescVersion7*)dd)->circle;
      break;
   case 8:
      pCircleFn = ((DevDescVersion8*)dd)->circle;
      break;
   case 9:
   case 10:
   case 11:
      pCircleFn = ((DevDescVersion9*)dd)->circle;
      break;
   case 12:
   case 13:
      pCircleFn = ((DevDescVersion12*)dd)->circle;
      break;
   case 14:
   case 15:
   default:
      pCircleFn = ((DevDescVersion15*)dd)->circle;
      break;
   }

   // call it
   pCircleFn(x, y, r, gc, dd);
}

void clip(double x0, double x1, double y0, double y1, pDevDesc dd)
{
   // get pointer to clip function
   void (*pClipFn)(double x0, double x1, double y0, double y1, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pClipFn = ((DevDescVersion5*)dd)->clip;
      break;
   case 6:
      pClipFn = ((DevDescVersion6*)dd)->clip;
      break;
   case 7:
      pClipFn = ((DevDescVersion7*)dd)->clip;
      break;
   case 8:
      pClipFn = ((DevDescVersion8*)dd)->clip;
      break;
   case 9:
   case 10:
   case 11:
      pClipFn = ((DevDescVersion9*)dd)->clip;
      break;
   case 12:
   case 13:
      pClipFn = ((DevDescVersion12*)dd)->clip;
      break;
   case 14:
   case 15:
   default:
      pClipFn = ((DevDescVersion15*)dd)->clip;
      break;
   }

   // call it
   pClipFn(x0, x1, y0, y1, dd);
}

void close(pDevDesc dd)
{
   // get pointer to close function
   void (*pCloseFn)(pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pCloseFn = ((DevDescVersion5*)dd)->close;
      break;
   case 6:
      pCloseFn = ((DevDescVersion6*)dd)->close;
      break;
   case 7:
      pCloseFn = ((DevDescVersion7*)dd)->close;
      break;
   case 8:
      pCloseFn = ((DevDescVersion8*)dd)->close;
      break;
   case 9:
   case 10:
   case 11:
      pCloseFn = ((DevDescVersion9*)dd)->close;
      break;
   case 12:
   case 13:
      pCloseFn = ((DevDescVersion12*)dd)->close;
      break;
   case 14:
   case 15:
   default:
      pCloseFn = ((DevDescVersion15*)dd)->close;
      break;
      
   }

   // call it
   pCloseFn(dd);
}

void deactivate(pDevDesc dd)
{
   // get pointer to deactivate function
   void (*pDeactivateFn)(pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pDeactivateFn = ((DevDescVersion5*)dd)->deactivate;
      break;
   case 6:
      pDeactivateFn = ((DevDescVersion6*)dd)->deactivate;
      break;
   case 7:
      pDeactivateFn = ((DevDescVersion7*)dd)->deactivate;
      break;
   case 8:
      pDeactivateFn = ((DevDescVersion8*)dd)->deactivate;
      break;
   case 9:
   case 10:
   case 11:
      pDeactivateFn = ((DevDescVersion9*)dd)->deactivate;
      break;
   case 12:
   case 13:
      pDeactivateFn = ((DevDescVersion12*)dd)->deactivate;
      break;
   case 14:
   case 15:
   default:
      pDeactivateFn = ((DevDescVersion15*)dd)->deactivate;
      break;
   }

   // call it
   pDeactivateFn(dd);
}

Rboolean locator(double *x, double *y, pDevDesc dd)
{
   // get pointer to locator function
   Rboolean (*pLocatorFn)(double *x, double *y, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pLocatorFn = ((DevDescVersion5*)dd)->locator;
      break;
   case 6:
      pLocatorFn = ((DevDescVersion6*)dd)->locator;
      break;
   case 7:
      pLocatorFn = ((DevDescVersion7*)dd)->locator;
      break;
   case 8:
      pLocatorFn = ((DevDescVersion8*)dd)->locator;
      break;
   case 9:
   case 10:
   case 11:
      pLocatorFn = ((DevDescVersion9*)dd)->locator;
      break;
   case 12:
   case 13:
      pLocatorFn = ((DevDescVersion12*)dd)->locator;
      break;
   case 14:
   case 15:
   default:
      pLocatorFn = ((DevDescVersion15*)dd)->locator;
      break;
   }

   // call it
   return pLocatorFn(x, y, dd);
}

void line(double x1, double y1, double x2, double y2, const pGEcontext gc, pDevDesc dd)
{
   // get pointer to line function
   void (*pLineFn)(double x1, double y1, double x2, double y2, const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pLineFn = ((DevDescVersion5*)dd)->line;
      break;
   case 6:
      pLineFn = ((DevDescVersion6*)dd)->line;
      break;
   case 7:
      pLineFn = ((DevDescVersion7*)dd)->line;
      break;
   case 8:
      pLineFn = ((DevDescVersion8*)dd)->line;
      break;
   case 9:
   case 10:
   case 11:
      pLineFn = ((DevDescVersion9*)dd)->line;
      break;
   case 12:
   case 13:
      pLineFn = ((DevDescVersion12*)dd)->line;
      break;
   case 14:
   case 15:
   default:
      pLineFn = ((DevDescVersion15*)dd)->line;
      break;
   }

   // call it
   pLineFn(x1, y1, x2, y2, gc, dd);
}

void metricInfo(int c, const pGEcontext gc, double *ascent, double *descent, double *width, pDevDesc dd)
{
   // get pointer to metricInfo function
   void (*pMetricInfoFn)(int c, const pGEcontext gc, double *ascent, double *descent, double *width, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pMetricInfoFn = ((DevDescVersion5*)dd)->metricInfo;
      break;
   case 6:
      pMetricInfoFn = ((DevDescVersion6*)dd)->metricInfo;
      break;
   case 7:
      pMetricInfoFn = ((DevDescVersion7*)dd)->metricInfo;
      break;
   case 8:
      pMetricInfoFn = ((DevDescVersion8*)dd)->metricInfo;
      break;
   case 9:
   case 10:
   case 11:
      pMetricInfoFn = ((DevDescVersion9*)dd)->metricInfo;
      break;
   case 12:
   case 13:
      pMetricInfoFn = ((DevDescVersion12*)dd)->metricInfo;
      break;
   case 14:
   case 15:
   default:
      pMetricInfoFn = ((DevDescVersion15*)dd)->metricInfo;
      break;
   }

   // call it
   pMetricInfoFn(c, gc, ascent, descent, width, dd);
}

void mode(int mode, pDevDesc dd)
{
   // get pointer to mode function
   void (*pModeFn)(int mode, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pModeFn = ((DevDescVersion5*)dd)->mode;
      break;
   case 6:
      pModeFn = ((DevDescVersion6*)dd)->mode;
      break;
   case 7:
      pModeFn = ((DevDescVersion7*)dd)->mode;
      break;
   case 8:
      pModeFn = ((DevDescVersion8*)dd)->mode;
      break;
   case 9:
   case 10:
   case 11:
      pModeFn = ((DevDescVersion9*)dd)->mode;
      break;
   case 12:
   case 13:
      pModeFn = ((DevDescVersion12*)dd)->mode;
      break;
   case 14:
   case 15:
   default:
      pModeFn = ((DevDescVersion15*)dd)->mode;
      break;
   }

   // call it
   if (pModeFn != nullptr)
      pModeFn(mode, dd);
}

void newPage(const pGEcontext gc, pDevDesc dd)
{
   // get pointer to newPage function
   void (*pNewPageFn)(const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pNewPageFn = ((DevDescVersion5*)dd)->newPage;
      break;
   case 6:
      pNewPageFn = ((DevDescVersion6*)dd)->newPage;
      break;
   case 7:
      pNewPageFn = ((DevDescVersion7*)dd)->newPage;
      break;
   case 8:
      pNewPageFn = ((DevDescVersion8*)dd)->newPage;
      break;
   case 9:
   case 10:
   case 11:
      pNewPageFn = ((DevDescVersion9*)dd)->newPage;
      break;
   case 12:
   case 13:
      pNewPageFn = ((DevDescVersion12*)dd)->newPage;
      break;
   case 14:
   case 15:
   default:
      pNewPageFn = ((DevDescVersion15*)dd)->newPage;
      break;
   }

   // call it
   pNewPageFn(gc, dd);
}

void polygon(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd)
{
   // get pointer to polygon function
   void (*pPolygonFn)(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pPolygonFn = ((DevDescVersion5*)dd)->polygon;
      break;
   case 6:
      pPolygonFn = ((DevDescVersion6*)dd)->polygon;
      break;
   case 7:
      pPolygonFn = ((DevDescVersion7*)dd)->polygon;
      break;
   case 8:
      pPolygonFn = ((DevDescVersion8*)dd)->polygon;
      break;
   case 9:
   case 10:
   case 11:
      pPolygonFn = ((DevDescVersion9*)dd)->polygon;
      break;
   case 12:
   case 13:
      pPolygonFn = ((DevDescVersion12*)dd)->polygon;
      break;
   case 14:
   default:
      pPolygonFn = ((DevDescVersion14*)dd)->polygon;
      break;
   }

   // call it
   pPolygonFn(n, x, y, gc, dd);
}

void polyline(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd)
{
   // get pointer to polyline function
   void (*pPolylineFn)(int n, double *x, double *y, const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pPolylineFn = ((DevDescVersion5*)dd)->polyline;
      break;
   case 6:
      pPolylineFn = ((DevDescVersion6*)dd)->polyline;
      break;
   case 7:
      pPolylineFn = ((DevDescVersion7*)dd)->polyline;
      break;
   case 8:
      pPolylineFn = ((DevDescVersion8*)dd)->polyline;
      break;
   case 9:
   case 10:
   case 11:
      pPolylineFn = ((DevDescVersion9*)dd)->polyline;
      break;
   case 12:
   case 13:
      pPolylineFn = ((DevDescVersion12*)dd)->polyline;
      break;
   case 14:
   case 15:
   default:
      pPolylineFn = ((DevDescVersion15*)dd)->polyline;
      break;
   }

   // call it
   pPolylineFn(n, x, y, gc, dd);
}

void rect(double x0, double y0, double x1, double y1, const pGEcontext gc, pDevDesc dd)
{
   // get pointer to rect function
   void (*pRectFn)(double x0, double y0, double x1, double y1, const pGEcontext gc, pDevDesc dd);

   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {
   case 5:
      pRectFn = ((DevDescVersion5*)dd)->rect;
      break;
   case 6:
      pRectFn = ((DevDescVersion6*)dd)->rect;
      break;
   case 7:
      pRectFn = ((DevDescVersion7*)dd)->rect;
      break;
   case 8:
      pRectFn = ((DevDescVersion8*)dd)->rect;
      break;
   case 9:
   case 10:
   case 11:
      pRectFn = ((DevDescVersion9*)dd)->rect;
      break;
   case 12:
   case 13:
      pRectFn = ((DevDescVersion12*)dd)->rect;
      break;
   case 14:
   case 15:
   default:
      pRectFn = ((DevDescVersion15*)dd)->rect;
      break;
   }

   // call it
   pRectFn(x0, y0, x1, y1, gc, dd);
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
   switch (engineVersion)
   {
   case 8:
      pPathFn = ((DevDescVersion8*)dd)->path;
      break;
   case 9:
   case 10:
   case 11:
      pPathFn = ((DevDescVersion9*)dd)->path;
      break;
   case 12:
   case 13:
      pPathFn = ((DevDescVersion12*)dd)->path;
      break;
   case 14:
   case 15:
   default:
      pPathFn = ((DevDescVersion15*)dd)->path;
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
   case 10:
   case 11:
      pRasterFn = ((DevDescVersion9*)dd)->raster;
      break;
   case 12:
   case 13:
      pRasterFn = ((DevDescVersion12*)dd)->raster;
      break;
   case 14:
   case 15:
   default:
      pRasterFn = ((DevDescVersion15*)dd)->raster;
      break;
   }

   // call it
   pRasterFn(raster, w, h, x, y, width, height, rot, interpolate, gc, dd);
}

SEXP cap(pDevDesc dd)
{
   // get pointer to cap function
   SEXP (*pCapFn)(pDevDesc dd);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 6:
      pCapFn = ((DevDescVersion6*)dd)->cap;
      break;
   case 7:
      pCapFn = ((DevDescVersion7*)dd)->cap;
      break;
   case 8:
      pCapFn = ((DevDescVersion8*)dd)->cap;
      break;
   case 9:
   case 10:
   case 11:
      pCapFn = ((DevDescVersion9*)dd)->cap;
      break;
   case 12:
   case 13:
      pCapFn = ((DevDescVersion12*)dd)->cap;
      break;
   case 14:
   case 15:
   default:
      pCapFn = ((DevDescVersion15*)dd)->cap;
      break;
   }

   // call it
   return pCapFn(dd);
}

void size(double *left, double *right, double *bottom, double *top, pDevDesc dd)
{
   // get pointer to size function
   void (*pSizeFn)(double *left, double *right, double *bottom, double *top, pDevDesc dd);
   int engineVersion = ::R_GE_getVersion();
   switch(engineVersion)
   {
   case 5:
      pSizeFn = ((DevDescVersion5*)dd)->size;
      break;
   case 6:
      pSizeFn = ((DevDescVersion6*)dd)->size;
      break;
   case 7:
      pSizeFn = ((DevDescVersion7*)dd)->size;
      break;
   case 8:
      pSizeFn = ((DevDescVersion8*)dd)->size;
      break;
   case 9:
   case 10:
   case 11:
      pSizeFn = ((DevDescVersion9*)dd)->size;
      break;
   case 12:
   case 13:
      pSizeFn = ((DevDescVersion12*)dd)->size;
      break;
   case 14:
   case 15:
   default:
      pSizeFn = ((DevDescVersion15*)dd)->size;
      break;
   }

   // call it
   pSizeFn(left, right, bottom, top, dd);
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
   case 10:
   case 11:
      pStrWidthFn = ((DevDescVersion9*)dev)->strWidth;
      break;
   case 12:
   case 13:
      pStrWidthFn = ((DevDescVersion12*)dev)->strWidth;
      break;
   case 14:
   case 15:
   default:
      pStrWidthFn = ((DevDescVersion15*)dev)->strWidth;
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
   switch (engineVersion)
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
   case 10:
   case 11:
      pTextFn = ((DevDescVersion9*)dev)->text;
      break;
   case 12:
   case 13:
      pTextFn = ((DevDescVersion12*)dev)->text;
      break;
   case 14:
   case 15:
   default:
      pTextFn = ((DevDescVersion15*)dev)->text;
      break;
   }

   // call it
   pTextFn(x, y, str, rot, hadj, gc, dev);
}

SEXP setPattern(SEXP pattern, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   SEXP (*callback)(SEXP pattern, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->setPattern;
      break;
   }
   
   if (callback != nullptr)
      return callback(pattern, dd);
   
   return R_NilValue;
}

void releasePattern(SEXP ref, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   void (*callback)(SEXP ref, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->releasePattern;
      break;
   }
   
   if (callback != nullptr)
      callback(ref, dd);
}

SEXP setClipPath(SEXP path, SEXP ref, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   SEXP (*callback)(SEXP path, SEXP ref, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->setClipPath;
      break;
   }
   
   if (callback != nullptr)
      return callback(path, ref, dd);
   
   return R_NilValue;
}

void releaseClipPath(SEXP ref, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   void (*callback)(SEXP ref, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->releaseClipPath;
      break;
   }
   
   if (callback != nullptr)
      callback(ref, dd);
}

SEXP setMask(SEXP path, SEXP ref, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   SEXP (*callback)(SEXP path, SEXP ref, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->setMask;
      break;
   }
   
   if (callback != nullptr)
      return callback(path, ref, dd);
   
   return R_NilValue;
}

void releaseMask(SEXP ref, pDevDesc dd)
{
   int engineVersion = ::R_GE_getVersion();
   
   void (*callback)(SEXP ref, pDevDesc dd) = nullptr;
   
   switch (engineVersion)
   {
   case 14:
   case 15:
   default:
      callback = ((DevDescVersion15*)dd)->releaseMask;
      break;
   }
   
   if (callback != nullptr)
       callback(ref, dd);
}

SEXP defineGroup(SEXP source, int op, SEXP destination, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    SEXP (*callback)(SEXP source, int op, SEXP destination, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->defineGroup;
            break;
    }

    if (callback != nullptr)
        return callback(source, op, destination, dd);

    return R_NilValue;
}

void useGroup(SEXP ref, SEXP trans, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    void (*callback)(SEXP ref, SEXP trans, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->useGroup;
            break;
    }

    if (callback != nullptr)
        callback(ref, trans, dd);
}

void releaseGroup(SEXP ref, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    void (*callback)(SEXP ref, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->releaseGroup;
            break;
    }

    if (callback != nullptr)
        callback(ref, dd);
}

void stroke(SEXP path, const pGEcontext gc, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    void (*callback)(SEXP path, const pGEcontext gc, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->stroke;
            break;
    }

    if (callback != nullptr)
        callback(path, gc, dd);
}

void fill(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    void (*callback)(SEXP path, int rule, const pGEcontext gc, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->fill;
            break;
    }

    if (callback != nullptr)
        callback(path, rule, gc, dd);
}

void fillStroke(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
    int engineVersion = ::R_GE_getVersion();

    void (*callback)(SEXP path, int rule, const pGEcontext gc, pDevDesc dd) = nullptr;

    switch (engineVersion)
    {
        case 15:
        default:
            callback = ((DevDescVersion15*)dd)->fillStroke;
            break;
    }

    if (callback != nullptr)
        callback(path, rule, gc, dd);
}

SEXP capabilities(SEXP cap)
{
    SEXP (*callback)(SEXP cap) = nullptr;

    if (callback != nullptr)
        return callback(cap);

    return R_NilValue;
}


void setSize(pDevDesc pDD)
{
   // get pointer to size function
   void (*pSizeFn)(double*, double*, double*, double*, pDevDesc);
   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
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
   case 10:
   case 11:
      pSizeFn = ((DevDescVersion9*)pDD)->size;
      break;
   case 12:
   case 13:
      pSizeFn = ((DevDescVersion12*)pDD)->size;
      break;
   case 14:
   case 15:
   default:
      pSizeFn = ((DevDescVersion15*)pDD)->size;
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


} // namespace dev_desc
} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio

