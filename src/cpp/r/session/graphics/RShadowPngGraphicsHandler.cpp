/*
 * RShadowPngGraphicsHandler.cpp
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

#define R_INTERNAL_FUNCTIONS  // Rf_warningcall

#include <gsl/gsl-lite.hpp>

#include <boost/format.hpp>
#include <boost/bind/bind.hpp>

#include <core/system/System.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/session/RSessionUtils.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/REventLoop.hpp>

#undef TRUE
#undef FALSE

#include "RGraphicsDevDesc.hpp"
#include "RGraphicsHandler.hpp"
#include "RGraphicsUtils.hpp"

#include <Rembedded.h>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace device {

void GD_Trace(const std::string&);

} // end namespace device
} // end namespace graphics
} // end namespace session
} // end namespace r
} // end namespace rstudio

#define TRACE_GD_CALL (::rstudio::r::session::graphics::device::GD_Trace(BOOST_CURRENT_FUNCTION))

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace shadow {

namespace {

class PreserveCurrentDeviceScope
{
public:

   PreserveCurrentDeviceScope()
      : previousDevice_(nullptr)
   {
      if (!Rf_NoDevices())
      {
         previousDevice_ = GEcurrentDevice();
      }
   }

   ~PreserveCurrentDeviceScope()
   {
      try
      {
         // always restore previous device
         if (previousDevice_ != nullptr)
         {
            int deviceNumber = Rf_ndevNumber(previousDevice_->dev);
            Rf_selectDevice(deviceNumber);
         }
      }
      catch (...)
      {
      }
   }
private:
   pGEDevDesc previousDevice_;
};

struct ShadowDeviceData
{
   ShadowDeviceData() : pShadowPngDevice(nullptr) {}
   pDevDesc pShadowPngDevice;
};

void shadowDevOff(DeviceContext* pDC)
{
   // check for null pointers
   if (pDC == nullptr)
   {
      LOG_WARNING_MESSAGE("unexpected null device context");
      return;
   }

   if (pDC->pDeviceSpecific == nullptr)
   {
      LOG_WARNING_MESSAGE("unexpected null device data");
      return;
   }

   // check and see if the device has already been turned off
   ShadowDeviceData* pDevData = (ShadowDeviceData*) pDC->pDeviceSpecific;
   if (pDevData->pShadowPngDevice == nullptr)
      return;

   // kill the device if it's still alive
   pGEDevDesc geDev = desc2GEDesc(pDevData->pShadowPngDevice);
   if (Rf_ndevNumber(pDevData->pShadowPngDevice) > 0)
   {
      // close the device -- don't log R errors because they can happen
      // in the ordinary course of things for invalid graphics staes
      Error error = r::exec::executeSafely(boost::bind(GEkillDevice, geDev));
      if (error && !r::isCodeExecutionError(error))
         LOG_ERROR(error);
   }

   // set to null
   pDevData->pShadowPngDevice = nullptr;
}

Error shadowDevDesc(DeviceContext* pDC, pDevDesc* pDev)
{
   ShadowDeviceData* pDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;

   // generate on demand
   if (pDevData->pShadowPngDevice == nullptr ||
       Rf_ndevNumber(pDevData->pShadowPngDevice) == 0)
   {
      pDevData->pShadowPngDevice = nullptr;

      PreserveCurrentDeviceScope preserveCurrentDeviceScope;

      // determine width, height, and res
      int width = gsl::narrow_cast<int>(pDC->width * pDC->devicePixelRatio);
      int height = gsl::narrow_cast<int>(pDC->height * pDC->devicePixelRatio);
      int res = gsl::narrow_cast<int>(96.0 * pDC->devicePixelRatio);

      // determine the appropriate device
      std::string backend = getDefaultBackend();

      // validate that the ragg package is available.
      // this is mostly a sanity-check against users who might set
      // the RStudioGD.backend option without explicitly installing the
      // 'ragg' package, or if 'ragg' was uninstalled or otherwise removed
      // from the library paths during a session.
      if (backend == "ragg")
      {
         bool installed = false;
         Error error = r::exec::RFunction(".rs.isPackageInstalled")
               .addParam("ragg")
               .call(&installed);

         if (error || !installed)
         {
            if (error)
               LOG_ERROR(error);

            const char* msg = "package 'ragg' is not available; using default graphics backend instead";
            Rf_warningcall(R_NilValue, "%s", msg);
            r::options::setOption(kGraphicsOptionBackend, "default");
            backend = "default";
         }
      }

      // ensure the directory hosting the plot is available
      // (plots are often created within the R session's temporary directory,
      // which seems to be opportunisitically deleted in some environments)
      //
      // https://github.com/rstudio/rstudio/issues/2214
      FilePath targetPath = pDC->targetPath;
      Error error = targetPath.getParent().ensureDirectory();
      if (error)
         return error;

      if (backend == "ragg")
      {
         Error error = r::exec::RFunction("ragg:::agg_png")
               .addParam("filename", string_utils::utf8ToSystem(targetPath.getAbsolutePath()))
               .addParam("width", width)
               .addParam("height", height)
               .addParam("res", res)
               .call();
         if (error)
            return error;
      }
      else
      {
         // create PNG device (completely bail on error)
         boost::format fmt("grDevices:::png(\"%1%\", %2%, %3%, res = %4% %5%)");
         std::string code = boost::str(fmt %
                                       string_utils::utf8ToSystem(targetPath.getAbsolutePath()) %
                                       width %
                                       height %
                                       res %
                                       r::session::graphics::extraBitmapParams());
         Error err = r::exec::executeString(code);
         if (err)
            return err;
      }

      // save reference to shadow device
      pDevData->pShadowPngDevice = GEcurrentDevice()->dev;
   }

   // return shadow device
   *pDev = pDevData->pShadowPngDevice;
   return Success();
}

void syncDevDesc(pDevDesc pDev, pDevDesc pShadowDev)
{
   int engineVersion = ::R_GE_getVersion();
   switch (engineVersion)
   {

   case 12:
   case 13:
   {
      DevDescVersion12* pDev12       = (DevDescVersion12*) pDev;
      DevDescVersion12* pShadowDev12 = (DevDescVersion12*) pShadowDev;

      pDev12->left   = pShadowDev12->left;
      pDev12->right  = pShadowDev12->right;
      pDev12->bottom = pShadowDev12->bottom;
      pDev12->top    = pShadowDev12->top;

      pDev12->clipLeft   = pShadowDev12->clipLeft;
      pDev12->clipRight  = pShadowDev12->clipRight;
      pDev12->clipBottom = pShadowDev12->clipBottom;
      pDev12->clipTop    = pShadowDev12->clipTop;

      pDev12->canClip                 = pShadowDev12->canClip;
      pDev12->canChangeGamma          = pShadowDev12->canChangeGamma;
      pDev12->canGenMouseDown         = pShadowDev12->canGenMouseDown;
      pDev12->canGenMouseMove         = pShadowDev12->canGenMouseMove;
      pDev12->canGenMouseUp           = pShadowDev12->canGenMouseUp;
      pDev12->canGenKeybd             = pShadowDev12->canGenKeybd;
      pDev12->hasTextUTF8             = pShadowDev12->hasTextUTF8;
      pDev12->wantSymbolUTF8          = pShadowDev12->wantSymbolUTF8;
      pDev12->useRotatedTextInContour = pShadowDev12->useRotatedTextInContour;

      pDev12->haveTransparency        = pShadowDev12->haveTransparency;
      pDev12->haveTransparentBg       = pShadowDev12->haveTransparentBg;
      pDev12->haveRaster              = pShadowDev12->haveRaster;
      pDev12->haveCapture             = pShadowDev12->haveCapture;

      break;
   }

   case 14:
   default:
   {
      DevDescVersion14* pDev14       = (DevDescVersion14*) pDev;
      DevDescVersion14* pShadowDev14 = (DevDescVersion14*) pShadowDev;

      pDev14->left   = pShadowDev14->left;
      pDev14->right  = pShadowDev14->right;
      pDev14->bottom = pShadowDev14->bottom;
      pDev14->top    = pShadowDev14->top;

      pDev14->clipLeft   = pShadowDev14->clipLeft;
      pDev14->clipRight  = pShadowDev14->clipRight;
      pDev14->clipBottom = pShadowDev14->clipBottom;
      pDev14->clipTop    = pShadowDev14->clipTop;

      pDev14->canClip                 = pShadowDev14->canClip;
      pDev14->canChangeGamma          = pShadowDev14->canChangeGamma;
      pDev14->canGenMouseDown         = pShadowDev14->canGenMouseDown;
      pDev14->canGenMouseMove         = pShadowDev14->canGenMouseMove;
      pDev14->canGenMouseUp           = pShadowDev14->canGenMouseUp;
      pDev14->canGenKeybd             = pShadowDev14->canGenKeybd;
      pDev14->hasTextUTF8             = pShadowDev14->hasTextUTF8;
      pDev14->wantSymbolUTF8          = pShadowDev14->wantSymbolUTF8;
      pDev14->useRotatedTextInContour = pShadowDev14->useRotatedTextInContour;

      pDev14->haveTransparency        = pShadowDev14->haveTransparency;
      pDev14->haveTransparentBg       = pShadowDev14->haveTransparentBg;
      pDev14->haveRaster              = pShadowDev14->haveRaster;
      pDev14->haveCapture             = pShadowDev14->haveCapture;

      pDev14->deviceVersion           = pShadowDev14->deviceVersion;
      pDev14->deviceClip              = pShadowDev14->deviceClip;

      break;
   }

   }

}

// this version of the function is called from R graphics primitives
// so can (and should) throw errors in R longjmp style
pDevDesc shadowDevDesc(pDevDesc pDev)
{
   try
   {
      DeviceContext* pDC = (DeviceContext*)pDev->deviceSpecific;

      pDevDesc pShadowDev = nullptr;
      Error error = shadowDevDesc(pDC, &pShadowDev);
      if (error)
      {
         LOG_ERROR(error);
         throw r::exec::RErrorException(error.getSummary());
      }

      syncDevDesc(pDev, pShadowDev);
      return pShadowDev;
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error("Shadow graphics device error: " +
                     std::string(e.message()));
   }

   // keep compiler happy
   return nullptr;
}

FilePath tempFile(const std::string& extension)
{
   FilePath tempFileDir(string_utils::systemToUtf8(R_TempDir));
   FilePath tempFilePath = tempFileDir.completePath(
      core::system::generateUuid(false) +
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

   pDevDesc dev = nullptr;
   Error error = shadowDevDesc(pDC, &dev);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // select the device
   int deviceNumber = Rf_ndevNumber(dev);
   Rf_selectDevice(deviceNumber);

   // copy display list (ignore R errors because they can happen in the normal
   // course of things for invalid graphics states). also suppress output
   // in scope because R 3.0 seems to sneak out error messages from within
   // the invalid name warning in checkValidSymbolId in dotcode.c
   {
      r::session::utils::SuppressOutputInScope scope;
      error = r::exec::RFunction(".rs.GEcopyDisplayList", rsDeviceNumber).call();
      if (error && !r::isCodeExecutionError(error))
         LOG_ERROR(error);
   }
}

} // anonymous namespace


bool initialize(int width, int height, double devicePixelRatio, DeviceContext* pDC)
{
   pDC->targetPath = tempFile("png");
   pDC->width = width;
   pDC->height = height;
   pDC->devicePixelRatio = devicePixelRatio;

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

void setDeviceAttributes(pDevDesc pDev)
{
   pDevDesc shadowDev = shadowDevDesc(pDev);
   if (shadowDev == nullptr)
      return;

   dev_desc::setDeviceAttributes(pDev, shadowDev);
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

Error writeToPNG(const FilePath& targetPath, DeviceContext* pDC)
{
   // disable polled events -- we don't want to manipulate
   // the graphics device (or destroy it!) while we're using it
   r::session::event_loop::DisablePolledEventHandlerScope scope;

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
            LOG_ERROR(deleteError);
      }
   }

   // regenerate the shadow device
   pDevDesc dev = pDC->dev;
   int width = pDC->width;
   int height = pDC->height;
   double devicePixelRatio = pDC->devicePixelRatio;
   handler::destroy(pDC);
   pDC = handler::allocate(dev);
   dev->deviceSpecific = pDC;

   // re-create with the correct size
   if (!handler::initialize(width, height, devicePixelRatio, pDC))
   {
      return systemError(boost::system::errc::not_connected, ERROR_LOCATION);
   }

   // update device attributes
   handler::setDeviceAttributes(dev);

   // replay the rstudio graphics device context onto the png
   shadowDevSync(pDC);

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
   if (pngDevDesc == nullptr)
      return;

   dev_desc::circle(x, y, r, gc, pngDevDesc);
}

void line(double x1,
          double y1,
          double x2,
          double y2,
          const pGEcontext gc,
          pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::line(x1, y1, x2, y2, gc, pngDevDesc);
}

void polygon(int n,
             double *x,
             double *y,
             const pGEcontext gc,
             pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::polygon(n, x, y, gc, pngDevDesc);
}

void polyline(int n,
              double *x,
              double *y,
              const pGEcontext gc,
              pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::polyline(n, x, y, gc, pngDevDesc);
}

void rect(double x0,
          double y0,
          double x1,
          double y1,
          const pGEcontext gc,
          pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::rect(x0, y0, x1, y1, gc, pngDevDesc);
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
   if (pngDevDesc == nullptr)
      return;

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
   if (pngDevDesc == nullptr)
      return;

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
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return R_NilValue;

   return dev_desc::cap(pngDevDesc);
}

void size(double* left,
          double* right,
          double* bottom,
          double* top,
          pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::size(left, right, bottom, top, pngDevDesc);
}

void metricInfo(int c,
                const pGEcontext gc,
                double* ascent,
                double* descent,
                double* width,
                pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::metricInfo(c, gc, ascent, descent, width, pngDevDesc);
}

double strWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return gsl::narrow_cast<double>(::strlen(str));

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
   if (pngDevDesc == nullptr)
      return;

   dev_desc::text(x, y, str, rot, hadj, gc, pngDevDesc);
}

void clip(double x0, double x1, double y0, double y1, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::clip(x0, x1, y0, y1, pngDevDesc);
}

void newPage(const pGEcontext gc, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::newPage(gc, pngDevDesc);
}

void mode(int mode, pDevDesc dev)
{
   pDevDesc pngDevDesc = shadowDevDesc(dev);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::mode(mode, pngDevDesc);
}

void onBeforeExecute(DeviceContext* pDC)
{
   // if the shadow device has somehow become the active device
   // then switch to the rstudio device. note this can occur if the
   // user creates another device such as windows() or postscript() and
   // then does a dev.off
   pGEDevDesc pCurrentDevice = NoDevices() ? nullptr : GEcurrentDevice();
   ShadowDeviceData* pShadowDevData = (ShadowDeviceData*)pDC->pDeviceSpecific;
   if (pCurrentDevice != nullptr && pShadowDevData != nullptr)
   {
      if (pCurrentDevice->dev == pShadowDevData->pShadowPngDevice)
      {
         // select the rstudio device
         int deviceNumber = Rf_ndevNumber(pDC->dev);
         Rf_selectDevice(deviceNumber);
      }
   }
}

SEXP setPattern(SEXP pattern, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return R_NilValue;

   return dev_desc::setPattern(pattern, pngDevDesc);
}

void releasePattern(SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::releasePattern(ref, pngDevDesc);
}

SEXP setClipPath(SEXP path, SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return R_NilValue;

   return dev_desc::setClipPath(path, ref, pngDevDesc);
}

void releaseClipPath(SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::releaseClipPath(ref, pngDevDesc);
}

SEXP setMask(SEXP path, SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return R_NilValue;

   return dev_desc::setMask(path, ref, pngDevDesc);
}

void releaseMask(SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::releaseMask(ref, pngDevDesc);
}

SEXP defineGroup(SEXP source, int op, SEXP destination, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return R_NilValue;

   return dev_desc::defineGroup(source, op, destination, pngDevDesc);
}

void useGroup(SEXP ref, SEXP trans, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::useGroup(ref, trans, pngDevDesc);
}

void releaseGroup(SEXP ref, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::releaseGroup(ref, pngDevDesc);
}

void stroke(SEXP path, const pGEcontext gc, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::stroke(path, gc, pngDevDesc);
}

void fill(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::fill(path, rule, gc, pngDevDesc);
}

void fillStroke(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::fillStroke(path, rule, gc, pngDevDesc);
}

SEXP capabilities(SEXP cap)
{
   return dev_desc::capabilities(cap);
}

void glyph(int n, int *glyphs, double *x, double *y,
           SEXP font, double size,
           int colour, double rot, pDevDesc dd)
{
   pDevDesc pngDevDesc = shadowDevDesc(dd);
   if (pngDevDesc == nullptr)
      return;

   dev_desc::glyph(n, glyphs, x, y, font, size, colour, rot, pngDevDesc);
}

} // namespace shadow

void installShadowHandler()
{
   handler::allocate = shadow::allocate;
   handler::initialize = shadow::initialize;
   handler::destroy = shadow::destroy;
   handler::setDeviceAttributes = shadow::setDeviceAttributes;
   handler::onBeforeAddDevice = shadow::onBeforeAddDevice;
   handler::onAfterAddDevice = shadow::onAfterAddDevice;
   handler::writeToPNG = shadow::writeToPNG;
   handler::circle = shadow::circle;
   handler::line = shadow::line;
   handler::polygon = shadow::polygon;
   handler::polyline = shadow::polyline;
   handler::rect = shadow::rect;
   handler::path = shadow::path;
   handler::raster = shadow::raster;
   handler::cap = shadow::cap;
   handler::size = shadow::size;
   handler::metricInfo = shadow::metricInfo;
   handler::strWidth = shadow::strWidth;
   handler::text = shadow::text;
   handler::clip = shadow::clip;
   handler::newPage = shadow::newPage;
   handler::mode = shadow::mode;
   handler::onBeforeExecute = shadow::onBeforeExecute;
   handler::setPattern = shadow::setPattern;
   handler::releasePattern = shadow::releasePattern;
   handler::setClipPath = shadow::setClipPath;
   handler::releaseClipPath = shadow::releaseClipPath;
   handler::setMask = shadow::setMask;
   handler::releaseMask = shadow::releaseMask;
   handler::defineGroup = shadow::defineGroup;
   handler::useGroup = shadow::useGroup;
   handler::releaseGroup = shadow::releaseGroup;
   handler::stroke = shadow::stroke;
   handler::fill = shadow::fill;
   handler::fillStroke = shadow::fillStroke;
   handler::capabilities = shadow::capabilities;
   handler::glyph = shadow::glyph;
}

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio



