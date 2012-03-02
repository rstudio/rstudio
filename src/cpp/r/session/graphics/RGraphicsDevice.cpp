/*
 * RGraphicsDevice.cpp
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

#include "RGraphicsDevice.hpp"

#include <cstdlib>

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RUtil.hpp>

#include "RGraphicsUtils.hpp"
#include "RGraphicsPlotManager.hpp"
#include "handler/RGraphicsHandler.hpp"

#include "config.h"

// nix windows definitions
#undef TRUE
#undef FALSE
#undef ERROR

#ifdef TRACE_GD_CALLS
#define TRACE_GD_CALL std::cerr << \
   std::string(BOOST_CURRENT_FUNCTION).substr( \
   std::string(BOOST_CURRENT_FUNCTION).find_last_of("::") + 1) \
   << std::endl;
#else
#define TRACE_GD_CALL
#endif

extern "C" double Rf_xDevtoUsr(double, pGEDevDesc);
extern "C" double Rf_yDevtoUsr(double, pGEDevDesc);
extern "C" double Rf_xDevtoNDC(double, pGEDevDesc);
extern "C" double Rf_yDevtoNDC(double, pGEDevDesc);

using namespace core ;

namespace r {
namespace session {
namespace graphics {
namespace device {
   
namespace {
   
// name of our graphics device
const char * const kRStudioDevice = "RStudioGD";

// GE device description
pGEDevDesc s_pGEDevDesc = NULL;   

// externally provided locator function
boost::function<bool(double*,double*)> s_locatorFunction;
   
// global size attributes (used to initialize new devices)
int s_width = 0;
int s_height = 0;   
   
// provide GraphicsDeviceEvents for plot manager
GraphicsDeviceEvents s_graphicsDeviceEvents;   
   
using namespace handler;

   
void GD_NewPage(const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL

   // delegate
   handler::newPage(gc, dev);

   // fire event (pass previousPageSnapshot)
   SEXP previousPageSnapshot = s_pGEDevDesc->savedSnapshot;
   s_graphicsDeviceEvents.onNewPage(previousPageSnapshot);
}

Rboolean GD_NewFrameConfirm(pDevDesc dd)
{   
   TRACE_GD_CALL

   // returning false causes the default implementation (printing a prompt
   // of "Hit <Return> to see next plot:" to the console) to be used. this 
   // seems ideal compared to any custom UI we could produce so we leave it be
   return FALSE;
}
   
   
void GD_Mode(int mode, pDevDesc dev) 
{
   TRACE_GD_CALL

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
   TRACE_GD_CALL

   *left = 0.0;
   *right = s_width;
   *bottom = s_height;
   *top = 0.0;
}

void GD_Clip(double x0, double x1, double y0, double y1, pDevDesc dev)
{
   TRACE_GD_CALL

   handler::clip(x0, x1, y0, y1, dev);
}


void GD_Rect(double x0,
             double y0,
             double x1,
             double y1,
             const pGEcontext gc,
             pDevDesc dev)
{
   TRACE_GD_CALL

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
   TRACE_GD_CALL

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
   TRACE_GD_CALL

   handler::raster(raster, w, h, x, y, width, height, rot, interpolate, gc, dd);
}

SEXP GD_Cap(pDevDesc dd)
{
   TRACE_GD_CALL

   return handler::cap(dd);
}

void GD_Circle(double x,
               double y,
               double r,
               const pGEcontext gc,
               pDevDesc dev)
{
   TRACE_GD_CALL

   handler::circle(x, y, r, gc, dev);
}

void GD_Line(double x1,
             double y1,
             double x2,
             double y2,
             const pGEcontext gc,
             pDevDesc dev)
{
   TRACE_GD_CALL

   handler::line(x1, y1, x2, y2, gc, dev);
}

void GD_Polyline(int n,
                 double *x,
                 double *y,
                 const pGEcontext gc,
                 pDevDesc dev)
{
   TRACE_GD_CALL

   handler::polyline(n, x, y, gc, dev);
}

void GD_Polygon(int n,
                double *x,
                double *y,
                const pGEcontext gc,
                pDevDesc dev)
{
   TRACE_GD_CALL

   handler::polygon(n, x, y, gc, dev);
}

void GD_MetricInfo(int c,
                   const pGEcontext gc,
                   double* ascent,
                   double* descent,
                   double* width,
                   pDevDesc dev)
{
   TRACE_GD_CALL

   handler::metricInfo(c, gc, ascent, descent, width, dev);
}

double GD_StrWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL

   return handler::strWidth(str, gc, dev);
}

double GD_StrWidthUTF8(const char *str, const pGEcontext gc, pDevDesc dev)
{
   TRACE_GD_CALL

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
   TRACE_GD_CALL

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
   TRACE_GD_CALL

   handler::text(x, y, str, rot, hadj, gc, dev);
}


Rboolean GD_Locator(double *x, double *y, pDevDesc dev) 
{
   TRACE_GD_CALL

   if (s_locatorFunction)
   {
      s_graphicsDeviceEvents.onDrawing();

      if(s_locatorFunction(x,y))
      {
         // if our graphics device went away while we were waiting 
         // for locator input then we need to return false
         
         if (s_pGEDevDesc != NULL)
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
   TRACE_GD_CALL
}

void GD_Deactivate(pDevDesc dev) 
{
   TRACE_GD_CALL
}   
   
void GD_Close(pDevDesc dev) 
{
   TRACE_GD_CALL

   if (s_pGEDevDesc != NULL)
   {
      // destroy device specific struct
      DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
      handler::destroy(pDC);

      // explicitly free and then null out the dev pointer of the GEDevDesc
      // This is to avoid incompatabilities between the heap we are compiled with
      // and the heap R is compiled with (we observed this to a problem with
      // 64-bit R)
      std::free(s_pGEDevDesc->dev);
      s_pGEDevDesc->dev = NULL;
      
      // set GDDevDesc to NULL so we don't reference it again
      s_pGEDevDesc = NULL;
   }

   s_graphicsDeviceEvents.onClosed();
}
   
void GD_OnExit(pDevDesc dd)
{
   TRACE_GD_CALL

   // NOTE: this may be called at various times including during error 
   // handling (jump_to_top_ex). therefore, do not place any process or device
   // final termination code here (even though the name of the function 
   // suggests you might want to do this!)
}

int GD_HoldFlush(pDevDesc dd, int level)
{
   TRACE_GD_CALL

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
   if (!handler::initialize(s_width, s_height, true, pDC))
   {
      // if this fails we are dead so close the device
      close();
      return;
   }

   // now update the device structure
   handler::setSize(pDev);

   // replay the display list onto the resized surface
   {
      SuppressDeviceEventsScope scope(plotManager());
      GEplayDisplayList(s_pGEDevDesc);
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
SEXP createGD()
{   
   // error if there is already an RStudio device
   if (s_pGEDevDesc != NULL)
   {
      Rf_warning("Only one RStudio graphics device is permitted");
      return R_NilValue;
   }


   R_CheckDeviceAvailable();
   
   BEGIN_SUSPEND_INTERRUPTS 
   {
      // initialize v9 structure
      DevDescVersion9 devDesc;

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
      devDesc.eventHelper = NULL;
      devDesc.holdflush = GD_HoldFlush;

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
      if (!handler::initialize(s_width, s_height, true, pDC))
      {
         handler::destroy(pDC);

         // leak the pDev on purpose because we don't have
         // access to R's heap/free function

         Rf_error("Unable to start RStudio device");
      }

      // set device specific context
      pDev->deviceSpecific = pDC;

      // device attributes
      handler::setSize(pDev);
      handler::setDeviceAttributes(pDev);

      // notify handler we are about to add (enables shadow device
      // to close itself so it doesn't show up in the dev.list()
      // in front of us
      handler::onBeforeAddInteractiveDevice(pDC);

      // associate with device description and add it
      s_pGEDevDesc = GEcreateDevDesc(pDev);
      GEaddDevice2(s_pGEDevDesc, kRStudioDevice);
      
      // notify handler we have added (so it can regenerate its context)
      handler::onAfterAddInteractiveDevice(pDC);

      // make us active
      Rf_selectDevice(Rf_ndevNumber(s_pGEDevDesc->dev)); 
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
   if (s_pGEDevDesc == NULL)
   {
      SEXP ignoredSEXP;
      Error error = r::exec::executeSafely<SEXP>(boost::bind(createGD), 
                                                 &ignoredSEXP);
      if (error)
         return error;
   }
   
   // select us
   Rf_selectDevice(Rf_ndevNumber(s_pGEDevDesc->dev)); 
   
   return Success();
}

SEXP rs_activateGD()
{
   Error error = makeActive();
   if (error)
      LOG_ERROR(error);
   return R_NilValue;
}


CCODE s_originalDevSetFunction;
SEXP devSetHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::function_hook::checkArity(op, args, call);

   // dev.set(which = 1) is a special syntax for creating a new graphics device,
   // so check for which = 1 and throw an error if this would result in the
   // creation of a 2nd RStudio graphics device
   int devNum = r::sexp::asInteger(CAR(args));
   if (devNum == 1 && s_pGEDevDesc != NULL)
      Rf_error("Only one RStudio graphics device is permitted");

   // call original
   return s_originalDevSetFunction(call, op, args, rho);
}

DisplaySize displaySize()
{
   return DisplaySize(s_width, s_height);
}

void deviceConvert(double* x,
                   double* y,
                   const boost::function<double(double,pGEDevDesc)>& xConv,
                   const boost::function<double(double,pGEDevDesc)>& yConv)
{
   if (s_pGEDevDesc != NULL)
   {
      *x = xConv(*x, s_pGEDevDesc);
      *y = yConv(*y, s_pGEDevDesc);
   }
   else
   {
      LOG_WARNING_MESSAGE(
            "deviceConvert called with no active graphics device");
   }
}

Error saveSnapshot(const core::FilePath& snapshotFile,
                   const core::FilePath& imageFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error ;
   
   // save snaphot file
   error = r::exec::RFunction(".rs.saveGraphics",
                              string_utils::utf8ToSystem(snapshotFile.absolutePath())).call();
   if (error)
      return error;

   // resync display list before saving png if necessary. for unknown reasons
   // there are permutations of plotting code which leaves the underlying PNG
   // in the RCairoGraphicsHandler not containing the full graphics context, so
   // we need to perform a re-synch of display list before rendering the PNG
   if (handler::resyncDisplayListBeforeWriteToPNG())
      resyncDisplayList();

   // save png file
   DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
   return handler::writeToPNG(imageFile, pDC, true);
}

Error restoreSnapshot(const core::FilePath& snapshotFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error ;
   
   // restore
   return r::exec::RFunction(".rs.restoreGraphics",
                             string_utils::utf8ToSystem(snapshotFile.absolutePath())).call();
}
    
void copyToActiveDevice()
{
   int rsDeviceNumber = GEdeviceNumber(s_pGEDevDesc);
   GEcopyDisplayList(rsDeviceNumber);
}
   
std::string imageFileExtension()
{
   return "png";
}

void onBeforeExecute()
{
   if (s_pGEDevDesc != NULL)
   {
      DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
      if (pDC != NULL)
         handler::onBeforeExecute(pDC);
   }
}

bool usePangoCairoHandler()
{
#ifdef PANGO_CAIRO_FOUND
   // use pango cairo for versions of R < 2.14
   return !r::util::hasRequiredVersion("2.14");
#else
   return false;
#endif
}

} // anonymous namespace
    
const int kDefaultWidth = 500;   
const int kDefaultHeight = 500; 
   
Error initialize(
            const FilePath& graphicsPath,
            const boost::function<bool(double*,double*)>& locatorFunction)
{      
   // initialize appropriate back-end
   if (usePangoCairoHandler())
      r::session::graphics::handler::installCairoHandler();
   else
      r::session::graphics::handler::installShadowHandler();

   // save reference to locator function
   s_locatorFunction = locatorFunction;
   
   // device conversion functions
   UnitConversionFunctions convert;
   convert.deviceToUser = boost::bind(deviceConvert,
                                    _1, _2, Rf_xDevtoUsr, Rf_yDevtoUsr);
   convert.deviceToNDC = boost::bind(deviceConvert,
                                    _1, _2, Rf_xDevtoNDC, Rf_yDevtoNDC);

   // create plot manager (provide functions & events)
   GraphicsDeviceFunctions graphicsDevice;
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
   
   // set size
   setSize(kDefaultWidth, kDefaultHeight);

   // check for an incompatible graphics version before fully initializing.
   std::string message;
   if (graphics::validateRequirements(&message))
   {
      // register device creation routine
      R_CallMethodDef createGDMethodDef ;
      createGDMethodDef.name = "rs_createGD" ;
      createGDMethodDef.fun = (DL_FUNC) createGD ;
      createGDMethodDef.numArgs = 0;
      r::routines::addCallMethod(createGDMethodDef);

      // regsiter device activiation routine
      R_CallMethodDef activateGDMethodDef ;
      activateGDMethodDef.name = "rs_activateGD" ;
      activateGDMethodDef.fun = (DL_FUNC) rs_activateGD ;
      activateGDMethodDef.numArgs = 0;
      r::routines::addCallMethod(activateGDMethodDef);


      // register dev.set hook to handle special dev.set(which = 1) case
      error = function_hook::registerReplaceHook("dev.set",
                                                 devSetHook,
                                                 &s_originalDevSetFunction);
      if (error)
         return error;

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


void setSize(int width, int height)
{
   // only set if the values have changed (prevents unnecessary plot 
   // invalidations from occuring)
   if ( width != s_width || height != s_height )
   {
      s_width = width;
      s_height = height;
      
      // if there is a device active sync its size
      if (s_pGEDevDesc != NULL)
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
   
void close()
{     
   if (s_pGEDevDesc != NULL)
      Rf_killDevice(Rf_ndevNumber(s_pGEDevDesc->dev));
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



