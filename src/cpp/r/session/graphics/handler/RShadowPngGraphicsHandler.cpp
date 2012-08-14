/*
 * RShadowPngGraphicsHandler.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iostream>

#include <boost/bind.hpp>
#include <boost/format.hpp>

#include <core/system/System.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>

#undef TRUE
#undef FALSE

#include "RGraphicsHandler.hpp"

#include <Rembedded.h>

// import r::exec::executeString for creating the PNG device
// (we do this with a direct declaration because generally
// speaking we don't want back-depencies on the r library
// from within this "sub-library"
namespace r {
namespace exec {
core::Error executeString(const std::string& str);
}
}

using namespace core ;

namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace shadow {

namespace {

class PreserveCurrentDeviceScope
{
public:
   PreserveCurrentDeviceScope() : previousDevice_(NULL)
   {
      if (!NoDevices())
         previousDevice_ = GEcurrentDevice();
   }
   ~PreserveCurrentDeviceScope()
   {
      try
      {
         // always restore previous device
         if (previousDevice_ != NULL)
            selectDevice(ndevNumber(previousDevice_->dev));
      }
      catch(...)
      {
      }
   }
private:
   pGEDevDesc previousDevice_;
};

struct ShadowDeviceData
{
   ShadowDeviceData() : pShadowPngDevice(NULL) {}
   pDevDesc pShadowPngDevice;
};

void shadowDevOff(DeviceContext* pDC)
{
   ShadowDeviceData* pDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;
   if (pDevData->pShadowPngDevice != NULL)
   {
      // kill the deviceF
      pGEDevDesc geDev = desc2GEDesc(pDevData->pShadowPngDevice);

      // only kill it is if is still alive
      if (ndevNumber(pDevData->pShadowPngDevice) > 0)
      {
         // close the device -- don't log R errors because they can happen
         // in the ordinary course of things for invalid graphics staes
         Error error = r::exec::executeSafely(boost::bind(GEkillDevice, geDev));
         if (error && !r::isCodeExecutionError(error))
            LOG_ERROR(error);
      }
      // set to null
      pDevData->pShadowPngDevice = NULL;
   }
}

Error shadowDevDesc(DeviceContext* pDC, pDevDesc* pDev)
{
   ShadowDeviceData* pDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;

   // generate on demand
   if (pDevData->pShadowPngDevice == NULL ||
       ndevNumber(pDevData->pShadowPngDevice) == 0)
   {
      pDevData->pShadowPngDevice = NULL;

      PreserveCurrentDeviceScope preserveCurrentDeviceScope;

      // create PNG device (completely bail on error)
      boost::format fmt("grDevices:::png(\"%1%\", %2%, %3%, pointsize = %4%)");
      std::string code = boost::str(fmt %
                                    string_utils::utf8ToSystem(pDC->targetPath.absolutePath()) %
                                    pDC->width %
                                    pDC->height %
                                    pDC->pointSize);
      Error err = r::exec::executeString(code);
      if (err)
         return err;

      // save reference to shadow device
      pDevData->pShadowPngDevice = GEcurrentDevice()->dev;
   }

   // return shadow device
   *pDev = pDevData->pShadowPngDevice;
   return Success();
}

// this version of the function is called from R graphics primitives
// so can (and should) throw errors in R longjmp style
pDevDesc shadowDevDesc(pDevDesc dev)
{
   try
   {
      DeviceContext* pDC = (DeviceContext*)dev->deviceSpecific;

      pDevDesc shadowDev;
      Error error = shadowDevDesc(pDC, &shadowDev);
      if (error)
      {
         LOG_ERROR(error);
         throw r::exec::RErrorException(error.summary());
      }

      return shadowDev;
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error("Shadow graphics device error: " +
                     std::string(e.message()));
   }

   // keep compiler happy
   return NULL;
}

FilePath tempFile(const std::string& extension)
{
   FilePath tempFileDir(string_utils::systemToUtf8(R_TempDir));
   FilePath tempFilePath = tempFileDir.complete(core::system::generateUuid(false) +
                                                "." + extension);
   return tempFilePath;
}


void shadowDevSync(DeviceContext* pDC)
{
   // get the rstudio device number
   pGEDevDesc rsGEDevDesc = desc2GEDesc(pDC->dev);
   int rsDeviceNumber = GEdeviceNumber(rsGEDevDesc);

   // copy the rstudio device's display list onto the shadow device
   PreserveCurrentDeviceScope preserveCurrentDevice;

   pDevDesc dev;
   Error error = shadowDevDesc(pDC, &dev);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   selectDevice(ndevNumber(dev));

   // copy display list (ignore R errors because they can happen in the normal
   // course of things for invalid graphics states)
   error = r::exec::executeSafely(boost::bind(GEcopyDisplayList,
                                              rsDeviceNumber));
   if (error && !r::isCodeExecutionError(error))
      LOG_ERROR(error);
}

} // anonymous namespace


bool initializeWithFile(const FilePath& filePath,
                        int width,
                        int height,
                        int pointSize,
                        bool displayListon,
                        DeviceContext* pDC)
{
   // initialize file info
   if (filePath.empty())
      pDC->targetPath = tempFile("png");
   else
      pDC->targetPath = filePath;
   pDC->width = width;
   pDC->height = height;
   pDC->pointSize = pointSize;

   return true;
}

DeviceContext* allocate(pDevDesc dev)
{
   // create device context
   DeviceContext* pDC = new DeviceContext(dev);

   // create device specific
   pDC->pDeviceSpecific = new ShadowDeviceData();
   return pDC;
}

void destroy(DeviceContext* pDC)
{
   // nix the shadow device
   shadowDevOff(pDC);

   // delete pointers
   ShadowDeviceData* pDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;
   delete pDevData;
   delete pDC;
}

void setSize(pDevDesc pDev)
{
   dev_desc::setSize(pDev);
   dev_desc::setSize(shadowDevDesc(pDev));
}

void setDeviceAttributes(pDevDesc pDev)
{
   pDevDesc shadowDev = shadowDevDesc(pDev);

   pDev->cra[0] = shadowDev->cra[0];
   pDev->cra[1] = shadowDev->cra[1];
   pDev->startps = shadowDev->startps;
   pDev->ipr[0] = shadowDev->ipr[0];
   pDev->ipr[1] = shadowDev->ipr[1];
   pDev->xCharOffset = shadowDev->xCharOffset;
   pDev->yCharOffset = shadowDev->yCharOffset;
   pDev->yLineBias = shadowDev->yLineBias;

   pDev->canClip = shadowDev->canClip;
   pDev->canHAdj = shadowDev->canHAdj;
   pDev->canChangeGamma = shadowDev->canChangeGamma;
   pDev->startcol = shadowDev->startcol;
   pDev->startfill = shadowDev->startfill;
   pDev->startlty = shadowDev->startlty;
   pDev->startfont = shadowDev->startfont;
   pDev->startps = shadowDev->startps;
   pDev->startgamma = shadowDev->startgamma;
   pDev->displayListOn = TRUE;

   // no support for events yet
   pDev->canGenMouseDown = FALSE;
   pDev->canGenMouseMove = FALSE;
   pDev->canGenMouseUp = FALSE;
   pDev->canGenKeybd = FALSE;
   pDev->gettingEvent = FALSE;
}

// the shadow device is created during creation of the main RStudio
// interactive graphics device (so we can copy its underlying device
// attributes into the RStudio device) however if we don't turn the
// shadow device off before adding the RStudio device then it shows
// up in the display list AHEAD of the RStudio device. This is a very
// bad state because it leaves the shadow device as the default
// device whenever another device (e.g. png, pdf, x11, etc.) is closed
void onBeforeAddDevice(DeviceContext* pDC)
{
   shadowDevOff(pDC);
}
void onAfterAddDevice(DeviceContext* pDC)
{
   pDevDesc dev;
   Error error = shadowDevDesc(pDC, &dev);
   if (error)
      LOG_ERROR(error);
}

// we do our own internal re-sync via shadowDevSync so we don't
// need the graphics device controlling code to do it for us
bool resyncDisplayListBeforeWriteToPNG()
{
   return false;
}

Error writeToPNG(const FilePath& targetPath,
                 DeviceContext* pDC,
                 bool keepContextAlive)
{
   // sync the shadow device to ensure we have the full playlist,
   shadowDevSync(pDC);

   // turn the shadow device off to write the file
   shadowDevOff(pDC);

   // if the targetPath != the bitmap path then copy it
   Error error;
   if (targetPath != pDC->targetPath)
   {
      // the target path would not exist if R failed to write the PNG
      // (e.g. because the graphics device was too small for the content)
      if (!pDC->targetPath.exists())
      {
         error = pathNotFoundError(ERROR_LOCATION);
      }
      else
      {
         error = pDC->targetPath.copy(targetPath);

         Error deleteError = pDC->targetPath.remove();
         if (deleteError)
            LOG_ERROR(error);
      }
   }

   if (keepContextAlive)
   {
      // regenerate the shadow device
      pDevDesc dev = pDC->dev;
      int width = pDC->width;
      int height = pDC->height;
      int pointSize = pDC->pointSize;
      handler::destroy(pDC);
      pDC = handler::allocate(dev);
      dev->deviceSpecific = pDC;

      // re-create with the correct size (don't set a file path)
      if (!handler::initialize(width, height, pointSize, true, pDC))
         return systemError(boost::system::errc::not_connected, ERROR_LOCATION);

      // now update the device structure
      handler::setSize(dev);

      // replay the rstudio graphics device context onto the png
      shadowDevSync(pDC);
   }

   // return status
   return error;
}


void circle(double x,
            double y,
            double r,
            const pGEcontext gc,
            pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->circle(x, y, r, gc, pngDevDesc);
}

void line(double x1,
          double y1,
          double x2,
          double y2,
          const pGEcontext gc,
          pDevDesc dev)
{
    pDevDesc pngDevDesc = shadowDevDesc(dev);
    pngDevDesc->line(x1, y1, x2, y2, gc, pngDevDesc);
}

void polygon(int n,
             double *x,
             double *y,
             const pGEcontext gc,
             pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->polygon(n, x, y, gc, pngDevDesc);
}

void polyline(int n,
              double *x,
              double *y,
              const pGEcontext gc,
              pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->polyline(n, x, y, gc, pngDevDesc);
}

void rect(double x0,
          double y0,
          double x1,
          double y1,
          const pGEcontext gc,
          pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->rect(x0, y0, x1, y1, gc, pngDevDesc);
}

void path(double *x,
          double *y,
          int npoly,
          int *nper,
          Rboolean winding,
          const pGEcontext gc,
          pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   dev_desc::path(x, y, npoly, nper, winding, gc, pngDevDesc);
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
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   dev_desc::raster(raster,
                    w,
                    h,
                    x,
                    y,
                    width,
                    height,
                    rot,
                    interpolate,
                    gc,
                    pngDevDesc);
}

SEXP cap(pDevDesc dd)
{
   return R_NilValue;
}

   

void metricInfo(int c,
                const pGEcontext gc,
                double* ascent,
                double* descent,
                double* width,
                pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->metricInfo(c, gc, ascent, descent, width, pngDevDesc);
}

double strWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   return dev_desc::strWidth(str, gc, pngDevDesc);
}
   
void text(double x,
          double y,
          const char *str,
          double rot,
          double hadj,
          const pGEcontext gc,
          pDevDesc dev)
{   
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   dev_desc::text(x, y, str, rot, hadj, gc, pngDevDesc);
}
   
void clip(double x0, double x1, double y0, double y1, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->clip(x0, x1, y0, y1, pngDevDesc);
}
   
void newPage(const pGEcontext gc, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   pngDevDesc->newPage(gc, pngDevDesc);
}

void mode(int mode, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc->mode != NULL)
      pngDevDesc->mode(mode, pngDevDesc);
}

void onBeforeExecute(DeviceContext* pDC)
{
   // if the shadow device has somehow become the active device
   // then switch to the rstudio device. note this can occur if the
   // user creates another device such as windows() or postscript() and
   // then does a dev.off
   pGEDevDesc pCurrentDevice = NoDevices() ? NULL : GEcurrentDevice();
   ShadowDeviceData* pShadowDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;
   if (pCurrentDevice != NULL && pShadowDevData != NULL)
   {
      if (pCurrentDevice->dev == pShadowDevData->pShadowPngDevice)
      {
         // select the rstudio device
         selectDevice(ndevNumber(pDC->dev));
      }
   }
}

} // namespace shadow

void installShadowHandler()
{
   handler::allocate = shadow::allocate;
   handler::destroy = shadow::destroy;
   handler::initializeWithFile = shadow::initializeWithFile;
   handler::setSize = shadow::setSize;
   handler::setDeviceAttributes = shadow::setDeviceAttributes;
   handler::onBeforeAddDevice = shadow::onBeforeAddDevice;
   handler::onAfterAddDevice = shadow::onAfterAddDevice;
   handler::resyncDisplayListBeforeWriteToPNG = shadow::resyncDisplayListBeforeWriteToPNG;
   handler::writeToPNG = shadow::writeToPNG;
   handler::circle = shadow::circle;
   handler::line = shadow::line;
   handler::polygon = shadow::polygon;
   handler::polyline = shadow::polyline;
   handler::rect = shadow::rect;
   handler::path = shadow::path;
   handler::raster = shadow::raster;
   handler::cap = shadow::cap;
   handler::metricInfo = shadow::metricInfo;
   handler::strWidth = shadow::strWidth;
   handler::text = shadow::text;
   handler::clip = shadow::clip;
   handler::newPage = shadow::newPage;
   handler::mode = shadow::mode;
   handler::onBeforeExecute = shadow::onBeforeExecute;
}
   
} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r



