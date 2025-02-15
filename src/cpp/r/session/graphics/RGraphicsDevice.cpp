/*
 * RGraphicsDevice.cpp
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

#include "RGraphicsDevice.hpp"

#include <cstdlib>

#include <boost/thread.hpp>
#include <boost/bind/bind.hpp>
#include <boost/utility/string_view.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RUtil.hpp>

#include <r/session/RSessionUtils.hpp>

#include "RGraphicsDevDesc.hpp"
#include "RGraphicsUtils.hpp"
#include "RGraphicsPlotManager.hpp"
#include "RGraphicsHandler.hpp"

#include "config.h"

// nix windows definitions
#undef TRUE
#undef FALSE
#undef ERROR

#define TRACE_GD_CALL (::rstudio::r::session::graphics::device::GD_Trace(BOOST_CURRENT_FUNCTION))

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace device {

// whether or not we should trace GD calls
// intentionally not in anonymous namespace so that it can
// be referenced and changed without explicit header export
bool s_gdTracingEnabled = false;

void GD_Trace(const std::string& func)
{
   if (s_gdTracingEnabled)
   {
      std::cerr << func.substr(func.find_last_of("::") + 1) << std::endl;
   }
}
   
namespace {

// name of our graphics device
const char * const kRStudioDevice = "RStudioGD";

// GE device description
pGEDevDesc s_pGEDevDesc = nullptr;

// externally provided locator function
boost::function<bool(double*, double*)> s_locatorFunction;

// global size attributes (used to initialize new devices)
DeviceType s_deviceType = DeviceTypeUnknown;
int s_width = 0;
int s_height = 0;
double s_devicePixelRatio = 1.0;
   
// provide GraphicsDeviceEvents for plot manager
GraphicsDeviceEvents s_graphicsDeviceEvents;
   
using namespace handler;

   
void GD_NewPage(const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL;

   // delegate
   handler::newPage(gc, dev);

   // fire event (pass previousPageSnapshot)
   SEXP previousPageSnapshot = s_pGEDevDesc->savedSnapshot;
   s_graphicsDeviceEvents.onNewPage(previousPageSnapshot);
}

Rboolean GD_NewFrameConfirm(pDevDesc dd)
{   
   TRACE_GD_CALL;

   // returning false causes the default implementation (printing a prompt
   // of "Hit <Return> to see next plot:" to the console) to be used. this 
   // seems ideal compared to any custom UI we could produce so we leave it be
   return FALSE;
}
   
   
void GD_Mode(int mode, pDevDesc dev) 
{
   TRACE_GD_CALL;

   // 0 = stop drawing
   // 1 = start drawing
   // 2 = input active

   handler::mode(mode, dev);

   s_graphicsDeviceEvents.onDrawing();
}

void GD_Size(double *left, 
             double *right,
             double *bottom, 
             double *top,
             pDevDesc dev) 
{
   TRACE_GD_CALL;

   handler::size(left, right, bottom, top, dev);
}

void GD_Clip(double x0, double x1, double y0, double y1, pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::clip(x0, x1, y0, y1, dev);
}


void GD_Rect(double x0,
             double y0,
             double x1,
             double y1,
             const pGEcontext gc,
             pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::rect(x0, y0, x1, y1, gc, dev);
}

void GD_Path(double *x,
             double *y,
             int npoly,
             int *nper,
             Rboolean winding,
             const pGEcontext gc,
             pDevDesc dd)
{
   TRACE_GD_CALL;

   handler::path(x, y, npoly, nper, winding, gc, dd);
}

void GD_Raster(unsigned int *raster,
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
   TRACE_GD_CALL;

   handler::raster(raster, w, h, x, y, width, height, rot, interpolate, gc, dd);
}

SEXP GD_Cap(pDevDesc dd)
{
   TRACE_GD_CALL;

   return handler::cap(dd);
}

void GD_Circle(double x,
               double y,
               double r,
               const pGEcontext gc,
               pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::circle(x, y, r, gc, dev);
}

void GD_Line(double x1,
             double y1,
             double x2,
             double y2,
             const pGEcontext gc,
             pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::line(x1, y1, x2, y2, gc, dev);
}

void GD_Polyline(int n,
                 double *x,
                 double *y,
                 const pGEcontext gc,
                 pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::polyline(n, x, y, gc, dev);
}

void GD_Polygon(int n,
                double *x,
                double *y,
                const pGEcontext gc,
                pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::polygon(n, x, y, gc, dev);
}

void GD_MetricInfo(int c,
                   const pGEcontext gc,
                   double* ascent,
                   double* descent,
                   double* width,
                   pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::metricInfo(c, gc, ascent, descent, width, dev);
   
   // in some rare cases, attempts to retrieve metric info can fail
   // (this seems to occur if the request occurs immediately after the
   // device is deactivated and then activated). detect these cases and
   // re-request metric info.
   //
   // https://github.com/tidyverse/ggplot2/issues/2252
#ifdef __APPLE__
   for (int i = 0; i < 10; i++)
   {
      if (*ascent != R_PosInf)
         break;

      boost::this_thread::sleep_for(boost::chrono::milliseconds(10));
      handler::metricInfo(c, gc, ascent, descent, width, dev);
   }
#endif
}

double GD_StrWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL;

   return handler::strWidth(str, gc, dev);
}

double GD_StrWidthUTF8(const char *str, const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL;

   return handler::strWidth(str, gc, dev);
}

void GD_Text(double x,
             double y,
             const char *str,
             double rot,
             double hadj,
             const pGEcontext gc,
             pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::text(x, y, str, rot, hadj, gc, dev);
}

void GD_TextUTF8(double x,
                 double y,
                 const char *str,
                 double rot,
                 double hadj,
                 const pGEcontext gc,
                 pDevDesc dev)
{
   TRACE_GD_CALL;

   handler::text(x, y, str, rot, hadj, gc, dev);
}


Rboolean GD_Locator(double *x, double *y, pDevDesc dev) 
{
   TRACE_GD_CALL;

   if (s_locatorFunction)
   {
      s_graphicsDeviceEvents.onDrawing();

      if (s_locatorFunction(x, y))
      {
         // if our graphics device went away while we were waiting 
         // for locator input then we need to return false
         if (s_pGEDevDesc != nullptr)
            return TRUE;
         else
            return FALSE;
      }
      else
      {
         s_graphicsDeviceEvents.onDrawing();
         return FALSE;
      }
   }
   else
   {
      return FALSE;
   }
}

void GD_Activate(pDevDesc dev) 
{
   TRACE_GD_CALL;

   s_deviceType = activeDeviceType();
}

void GD_Deactivate(pDevDesc dev) 
{
   TRACE_GD_CALL;

   s_deviceType = activeDeviceType();
}   
   
void GD_Close(pDevDesc dev) 
{
   TRACE_GD_CALL;

   if (s_pGEDevDesc != nullptr)
   {
      // destroy device specific struct
      DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
      handler::destroy(pDC);

      // explicitly free and then null out the dev pointer of the GEDevDesc
      // This is to avoid incompatibilities between the heap we are compiled with
      // and the heap R is compiled with (we observed this to a problem with
      // 64-bit R)
      std::free(s_pGEDevDesc->dev);
      s_pGEDevDesc->dev = nullptr;
      
      // set GDDevDesc to NULL so we don't reference it again
      s_pGEDevDesc = nullptr;
   }

   s_graphicsDeviceEvents.onClosed();
}
   
void GD_OnExit(pDevDesc dd)
{
   TRACE_GD_CALL;

   // NOTE: this may be called at various times including during error 
   // handling (jump_to_top_ex). therefore, do not place any process or device
   // final termination code here (even though the name of the function 
   // suggests you might want to do this!)
}

int GD_HoldFlush(pDevDesc dd, int level)
{
   TRACE_GD_CALL;

   // NOTE: holdflush does not apply to bitmap devices since they are
   // already "buffered" via the fact that they only do expensive operations
   // (write to file) on dev.off. We could in theory use dev.flush as
   // an indicator that we should detectChanges (e.g. when in a long
   // running piece of code which doesn't yield to the REPL -- in practice
   // however there are way too many flushes yielding lots of extra disk io
   // and http round trips. If anything perhaps we could introduce a
   // time-buffered variation where flush could set a flag that is checked
   // every e.g. 1-second during background processing.

   return 0;
}

// NOTE: pattern / clip / mask callbacks appear (as of R 4.1.0) to only
// be used by the Cairo and PDF devices; other implementations which
// do not make use of these features just return R_NilValue and otherwise
// do nothing

#define TRACE_GD_CALL (::rstudio::r::session::graphics::device::GD_Trace(BOOST_CURRENT_FUNCTION))

SEXP GD_SetPattern(SEXP pattern, pDevDesc dd)
{
   TRACE_GD_CALL;
   return handler::setPattern(pattern, dd);
}

void GD_ReleasePattern(SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::releasePattern(ref, dd);
}

SEXP GD_SetClipPath(SEXP path, SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   return handler::setClipPath(path, ref, dd);
}

void GD_ReleaseClipPath(SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::releaseClipPath(ref, dd);
}

SEXP GD_SetMask(SEXP path, SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   return handler::setMask(path, ref, dd);
}

void GD_ReleaseMask(SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::releaseMask(ref, dd);
}

SEXP GD_DefineGroup(SEXP source, int op, SEXP destination, pDevDesc dd)
{
   TRACE_GD_CALL;
   return handler::defineGroup(source, op, destination, dd);
}

void GD_UseGroup(SEXP ref, SEXP trans, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::useGroup(ref, trans, dd);
}

void GD_ReleaseGroup(SEXP ref, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::releaseGroup(ref, dd);
}

void GD_Stroke(SEXP path, const pGEcontext gc, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::stroke(path, gc, dd);
}

void GD_Fill(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::fill(path, rule, gc, dd);
}

void GD_FillStroke(SEXP path, int rule, const pGEcontext gc, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::fillStroke(path, rule, gc, dd);
}

SEXP GD_Capabilities(SEXP cap)
{
   TRACE_GD_CALL;
   return handler::capabilities(cap);
}


void GD_Glyph(int n, int *glyphs, double *x, double *y, 
              SEXP font, double size,
              int colour, double rot, pDevDesc dd)
{
   TRACE_GD_CALL;
   handler::glyph(n, glyphs, x, y, font, size, colour, rot, dd);
}


void resyncDisplayList()
{
   // get pointers to device desc and cairo data
   pDevDesc pDev = s_pGEDevDesc->dev;
   DeviceContext* pDC = (DeviceContext*)pDev->deviceSpecific;

   // destroy existing device context
   handler::destroy(pDC);

   // allocate a new one and set it to be the device specific ptr
   pDC = handler::allocate(pDev);
   pDev->deviceSpecific = pDC;

   // re-create with the correct size (don't set a file path)
   if (!handler::initialize(s_width, s_height, s_devicePixelRatio, pDC))
   {
      // if this fails we are dead so close the device
      close();
      return;
   }

   // now set device attributes
   handler::setDeviceAttributes(pDev);

   // replay the display list onto the resized surface
   {
      SuppressDeviceEventsScope scope(plotManager());
      Error error = r::exec::RFunction(".rs.GEplayDisplayList").call();
      if (error)
      {
         std::string errMsg;
         if (r::isCodeExecutionError(error, &errMsg))
            Rprintf("%s\n", errMsg.c_str());
         else
            LOG_ERROR(error);
      }

   }
}

void resizeGraphicsDevice()
{
   // resync display list
   resyncDisplayList();

   // notify listeners of resize
   s_graphicsDeviceEvents.onResized();
}   
   
// routine which creates device  
SEXP rs_createGD()
{   
   // error if there is already an RStudio device
   if (s_pGEDevDesc != nullptr)
   {
      if (!r::session::utils::isServerMode())
      {
         Error error = r::exec::executeString(".rs.newDesktopGraphicsDevice()");
         if (error)
         {
            std::string msg = error.getSummary();
            r::isCodeExecutionError(error, &msg);
            
            std::string rMsg = "Error creating graphics device: " + msg;
            Rf_warning("%s\n", rMsg.c_str());
         }
      }
      else
      {
         Rf_warning("Only one RStudio graphics device is permitted");
      }
      return R_NilValue;
   }


   R_CheckDeviceAvailable();
   
   BEGIN_SUSPEND_INTERRUPTS 
   {
      // initialize device description
      RSDevDesc devDesc;
      std::memset(&devDesc, 0, sizeof(devDesc));

      // device functions
      devDesc.activate = GD_Activate;
      devDesc.deactivate = GD_Deactivate;
      devDesc.size = GD_Size;
      devDesc.clip = GD_Clip;
      devDesc.rect = GD_Rect;
      devDesc.path = GD_Path;
      devDesc.raster = GD_Raster;
      devDesc.cap = GD_Cap;
      devDesc.circle = GD_Circle;
      devDesc.line = GD_Line;
      devDesc.polyline = GD_Polyline;
      devDesc.polygon = GD_Polygon;
      devDesc.locator = GD_Locator;
      devDesc.mode = GD_Mode;
      devDesc.metricInfo = GD_MetricInfo;
      devDesc.strWidth = GD_StrWidth;
      devDesc.strWidthUTF8 = GD_StrWidthUTF8;
      devDesc.text = GD_Text;
      devDesc.textUTF8 = GD_TextUTF8;
      devDesc.hasTextUTF8 = TRUE;
      devDesc.wantSymbolUTF8 = TRUE;
      devDesc.useRotatedTextInContour = FALSE;
      devDesc.newPage = GD_NewPage;
      devDesc.close = GD_Close;
      devDesc.newFrameConfirm = GD_NewFrameConfirm;
      devDesc.onExit = GD_OnExit;
      devDesc.eventEnv = R_NilValue;
      devDesc.eventHelper = nullptr;
      devDesc.holdflush = GD_HoldFlush;
      
      // added in version 14 (R 4.1.0)
      devDesc.setPattern = GD_SetPattern;
      devDesc.releasePattern = GD_ReleasePattern;
      
      devDesc.setClipPath = GD_SetClipPath;
      devDesc.releaseClipPath = GD_ReleaseClipPath;
      
      devDesc.setMask = GD_SetMask;
      devDesc.releaseMask = GD_ReleaseMask;
      
      // added in version 15 (R 4.2.0)
      devDesc.defineGroup = GD_DefineGroup;
      devDesc.useGroup = GD_UseGroup;
      devDesc.releaseGroup = GD_ReleaseGroup;
      
      devDesc.stroke = GD_Stroke;
      devDesc.fill = GD_Fill;
      devDesc.fillStroke = GD_FillStroke;
      devDesc.capabilities = GD_Capabilities;
      
      // added in version 16 (R 4.3.0)
      devDesc.glyph = GD_Glyph;

      // capabilities flags
      devDesc.haveTransparency = 2;
      devDesc.haveTransparentBg = 2;
      devDesc.haveRaster = 2;
      devDesc.haveCapture = 1;
      devDesc.haveLocator = 2;

      // allocate device
      pDevDesc pDev = handler::dev_desc::allocate(devDesc);

      // allocate and initialize context
      DeviceContext* pDC = handler::allocate(pDev);
      if (!handler::initialize(s_width, s_height, s_devicePixelRatio, pDC))
      {
         handler::destroy(pDC);

         // leak the pDev on purpose because we don't have
         // access to R's heap/free function

         Rf_error("Unable to start RStudio device");
      }

      // set device specific context
      pDev->deviceSpecific = pDC;

      // device attributes
      handler::setDeviceAttributes(pDev);

      // notify handler we are about to add (enables shadow device
      // to close itself so it doesn't show up in the dev.list()
      // in front of us
      handler::onBeforeAddDevice(pDC);

      // associate with device description and add it
      s_pGEDevDesc = GEcreateDevDesc(pDev);
      GEaddDevice2(s_pGEDevDesc, kRStudioDevice);
      
      // notify handler we have added (so it can regenerate its context)
      handler::onAfterAddDevice(pDC);

      // make us active
      int deviceNumber = Rf_ndevNumber(s_pGEDevDesc->dev);
      Rf_selectDevice(deviceNumber);
      s_deviceType = activeDeviceType();
   } 
   END_SUSPEND_INTERRUPTS;

   return R_NilValue;
}
   
// ensure that our device is created and active (required for snapshot
// creation/restoration)
Error makeActive()
{
   // don't make active if our graphics version is incompatible
   if (!graphics::validateRequirements())
      return Error(graphics::errc::IncompatibleGraphicsEngine, ERROR_LOCATION);

   // make sure we have been created
   if (s_pGEDevDesc == nullptr)
   {
      SEXP ignoredSEXP;
      Error error = r::exec::executeSafely<SEXP>(boost::bind(rs_createGD),
                                                 &ignoredSEXP);
      if (error)
         return error;
   }
   
   // select us
   int deviceNumber = Rf_ndevNumber(s_pGEDevDesc->dev);
   Rf_selectDevice(deviceNumber);
   s_deviceType = activeDeviceType();

   return Success();
}

bool isActive()
{
   return s_pGEDevDesc != nullptr &&
         Rf_ndevNumber(s_pGEDevDesc->dev) == Rf_curDevice();
}

SEXP rs_activateGD()
{
   Error error = makeActive();
   if (error)
      LOG_ERROR(error);
   return R_NilValue;
}


DisplaySize displaySize()
{
   return DisplaySize(s_width, s_height);
}

double grconvert(double val,
                 const std::string& type,
                 const std::string& from,
                 const std::string& to)
{
   r::exec::RFunction grconvFunc("graphics:::grconvert" + type, val, from, to);
   double convertedVal = val; // default in case of error
   Error error = grconvFunc.call(&convertedVal);
   if (error)
      LOG_ERROR(error);
   return convertedVal;
}

double grconvertX(double x, const std::string& from, const std::string& to)
{
   return grconvert(x, "X", from, to);
}

double grconvertY(double y, const std::string& from, const std::string& to)
{
   return grconvert(y, "Y", from, to);
}

void deviceToUser(double* x, double* y)
{
   *x = grconvertX(*x, "device", "user");
   *y = grconvertY(*y, "device", "user");
}

void deviceToNDC(double* x, double* y)
{
   *x = grconvertX(*x, "device", "ndc");
   *y = grconvertY(*y, "device", "ndc");
}

Error saveSnapshot(const core::FilePath& snapshotFile,
                   const core::FilePath& imageFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error;
   
   // save snapshot file
   error = r::exec::RFunction(".rs.saveGraphics",
                              string_utils::utf8ToSystem(snapshotFile.getAbsolutePath())).call();
   if (error)
      return error;

   // save png file
   DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
   return handler::writeToPNG(imageFile, pDC);
}

Error restoreSnapshot(const core::FilePath& snapshotFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error;
   
   // restore
   return r::exec::RFunction(".rs.restoreGraphics",
                             string_utils::utf8ToSystem(snapshotFile.getAbsolutePath())).call();
}
    
void copyToActiveDevice()
{
   int rsDeviceNumber = GEdeviceNumber(s_pGEDevDesc);
   r::exec::RFunction(".rs.GEcopyDisplayList", rsDeviceNumber).call();
}
   
std::string imageFileExtension()
{
   return "png";
}

void onBeforeExecute()
{
   if (s_pGEDevDesc != nullptr)
   {
      DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
      if (pDC != nullptr)
         handler::onBeforeExecute(pDC);
   }
}

} // anonymous namespace

void playDisplayList()
{
   GEplayDisplayList(s_pGEDevDesc);
}

const int kDefaultWidth = 500;
const int kDefaultHeight = 500;
const double kDefaultDevicePixelRatio = 1.0;
   
Error initialize(
            const FilePath& graphicsPath,
            const boost::function<bool(double*, double*)>& locatorFunction)
{
   // initialize tracing
   std::string traceGraphicsDevice = core::system::getenv("RSTUDIO_TRACE_GRAPHICS_DEVICE");
   if (core::string_utils::isTruthy(traceGraphicsDevice))
      s_gdTracingEnabled = true;
   
   // initialize shadow handler
   r::session::graphics::handler::installShadowHandler();

   // save reference to locator function
   s_locatorFunction = locatorFunction;
   
   // device conversion functions
   UnitConversionFunctions convert;
   convert.deviceToUser = deviceToUser;
   convert.deviceToNDC = deviceToNDC;

   // create plot manager (provide functions & events)
   GraphicsDeviceFunctions graphicsDevice;
   graphicsDevice.isActive = isActive;
   graphicsDevice.displaySize = displaySize;
   graphicsDevice.convert = convert;
   graphicsDevice.saveSnapshot = saveSnapshot;
   graphicsDevice.restoreSnapshot = restoreSnapshot;
   graphicsDevice.copyToActiveDevice = copyToActiveDevice;
   graphicsDevice.imageFileExtension = imageFileExtension;
   graphicsDevice.close = close;
   graphicsDevice.onBeforeExecute = onBeforeExecute;
   Error error = plotManager().initialize(graphicsPath,
                                          graphicsDevice,
                                          &s_graphicsDeviceEvents);
   if (error)
      return error;
   
   // set up default size, pixel ratio
   setSize(kDefaultWidth, kDefaultHeight, kDefaultDevicePixelRatio);

   // check for an incompatible graphics version before fully initializing.
   std::string message;
   if (graphics::validateRequirements(&message))
   {
      RS_REGISTER_CALL_METHOD(rs_createGD);
      RS_REGISTER_CALL_METHOD(rs_activateGD);

      // initialize
      return r::exec::RFunction(".rs.initGraphicsDevice").call();
   }
   else
   {
      // if there is one then print a warning and return Success. This allows
      // users to continue using the product while still being made aware of the
      // fact that their graphics engine is incompatible
      r::exec::warning(message);

      // success with warning
      return Success();
   }
}


void setSize(int width, int height, double devicePixelRatio)
{
   // only set if the values have changed (prevents unnecessary plot 
   // invalidations from occurring)
   if (width != s_width || height != s_height || devicePixelRatio != s_devicePixelRatio)
   {
      s_width = width;
      s_height = height;
      s_devicePixelRatio = devicePixelRatio;
      
      // if there is a device active sync its size
      if (s_pGEDevDesc != nullptr)
         resizeGraphicsDevice();
   }
}
   
int getWidth()
{
   return s_width;
}
   
int getHeight()
{
   return s_height;
}

DeviceType activeDeviceType()
{
   SEXP devicesSEXP = Rf_findVar(Rf_install(".Devices"), R_BaseEnv);
   if (TYPEOF(devicesSEXP) != LISTSXP)
      return DeviceTypeUnknown;

   for (; devicesSEXP != R_NilValue; devicesSEXP = CDR(devicesSEXP))
   {
      SEXP elSEXP = CAR(devicesSEXP);
      if (TYPEOF(elSEXP) != STRSXP)
         break;

      SEXP charSEXP = STRING_ELT(elSEXP, 0);
      boost::string_view device(CHAR(charSEXP), LENGTH(charSEXP));
      if (device.find("agg_") == 0)
      {
         return DeviceTypeAGG;
      }
      else if (device == "png" || device == "X11cairo")
      {
         return DeviceTypeCairo;
      }
      else if (device == "quartz" || device == "quartz_off_screen")
      {
         return DeviceTypeQuartz;
      }
      else if (device == "windows")
      {
         return DeviceTypeWindows;
      }
   }

   return DeviceTypeUnknown;
}

double devicePixelRatio()
{
   return s_devicePixelRatio;
}

void close()
{     
   if (s_pGEDevDesc != nullptr)
   {
      int deviceNumber = Rf_ndevNumber(s_pGEDevDesc->dev);
      Rf_killDevice(deviceNumber);
   }
}
   

 
} // namespace device

// if we don't have pango cairo then provide a null definition
// for the pango cairo init routine (for linking on other platforms)
#ifndef PANGO_CAIRO_FOUND
namespace handler {
void installCairoHandler() {}
}
#endif

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio



