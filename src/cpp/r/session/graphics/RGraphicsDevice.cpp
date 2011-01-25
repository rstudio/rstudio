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

#include "RGraphicsUtils.hpp"
#include "RGraphicsPlotManager.hpp"
#include "handler/RGraphicsHandler.hpp"

// nix windows definitions
#undef TRUE
#undef FALSE
#undef ERROR

using namespace core ;

namespace r {
namespace session {
namespace graphics {
namespace device {
   
namespace {
   
// name of our graphics device
const char * const kRStudioDevice = "RStudio";

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
   // delegate
   handler::newPage(gc, dev);

   // fire event (pass previousPageSnapshot)
   SEXP previousPageSnapshot = s_pGEDevDesc->savedSnapshot;
   s_graphicsDeviceEvents.onNewPage(previousPageSnapshot);
}

Rboolean GD_NewFrameConfirm(pDevDesc dd)
{   
   // returning false causes the default implementation (printing a prompt
   // of "Hit <Return> to see next plot:" to the console) to be used. this 
   // seems ideal compared to any custom UI we could produce so we leave it be
   return FALSE;
}
   
   
void GD_Mode(int mode, pDevDesc dev) 
{
   // 0 = stop drawing
   // 1 = start drawing
   // 2 = input active

   s_graphicsDeviceEvents.onDrawing();
}

void GD_Size(double *left, 
             double *right,
             double *bottom, 
             double *top,
             pDevDesc dev) 
{
   *left = 0.0;
   *right = s_width;
   *bottom = s_height;
   *top = 0.0;
}
   
Rboolean GD_Locator(double *x, double *y, pDevDesc dev) 
{
   if (s_locatorFunction)
   {
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

}

void GD_Deactivate(pDevDesc dev) 
{

}   
   
void GD_Close(pDevDesc dev) 
{   
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
   // NOTE: this may be called at various times including during error 
   // handling (jump_to_top_ex). therefore, do not place any process or device
   // final termination code here (even though the name of the function 
   // suggests you might want to do this!)
}

void resizeGraphicsDevice()
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
   if (!handler::initializePNG(s_width, s_height, true, pDC))
   {
      // if this fails we are dead so close the device
      close();
      return;
   }
   
   // now update the device structure
   handler::setSize(pDev);

   // replay the display list onto the resized surface
   GEplayDisplayList(s_pGEDevDesc);

   // notify listeners of resize
   s_graphicsDeviceEvents.onResized();
}   
   
// routine which creates device  
SEXP createGD()
{   
   // error if there is already an RStudio device
   if (s_pGEDevDesc != NULL)
      Rf_error("Only one RStudio graphics device is permitted");

   R_CheckDeviceAvailable();
   
   BEGIN_SUSPEND_INTERRUPTS 
   {
      // initialize v8 structure
      DevDescVersion8 devDesc;

      // device functions
      devDesc.activate = GD_Activate;
      devDesc.deactivate = GD_Deactivate;
      devDesc.size = GD_Size;
      devDesc.clip = handler::clip;
      devDesc.rect = handler::rect;
      devDesc.path = handler::path;
      devDesc.raster = handler::raster;
      devDesc.cap = handler::cap;
      devDesc.circle = handler::circle;
      devDesc.line = handler::line;
      devDesc.polyline = handler::polyline;
      devDesc.polygon = handler::polygon;
      devDesc.locator = GD_Locator;
      devDesc.mode = GD_Mode;
      devDesc.metricInfo = handler::metricInfo;
      devDesc.strWidth = devDesc.strWidthUTF8 = handler::strWidth;
      devDesc.text = devDesc.textUTF8 = handler::text;
      devDesc.hasTextUTF8 = TRUE;
      devDesc.wantSymbolUTF8 = TRUE;
      devDesc.useRotatedTextInContour = FALSE;
      devDesc.newPage = GD_NewPage;
      devDesc.close = GD_Close;
      devDesc.newFrameConfirm = GD_NewFrameConfirm;
      devDesc.onExit = GD_OnExit;
      devDesc.eventEnv = R_NilValue;
      devDesc.eventHelper = NULL;

      // allocate device
      pDevDesc pDev = handler::dev_desc::allocate(devDesc);

      // allocate and initialize context
      DeviceContext* pDC = handler::allocate(pDev);
      if (!handler::initializePNG(s_width, s_height, true, pDC))
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
      handler::setDeviceAttributes(true, pDev);

      // associate with device description and add it
      s_pGEDevDesc = GEcreateDevDesc(pDev);
      GEaddDevice2(s_pGEDevDesc, kRStudioDevice);
      
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
   if (!graphics::validateEngineVersion())
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
   
CCODE s_originalInteractiveFunction;   
SEXP interactiveHook(SEXP call, SEXP op, SEXP args, SEXP rho) 
{
   try
   {
      // NOTE: this technique has the side effect that a call to interactive()
      // when there is no device will result in our device being created
      // and the Plots tab coming to the front. there are very few calls to
      // the interactive() function within R so this may not be a practical
      // issue. an alternative implementation would be to hook the R
      // dev.interactive function directly and detect the "no devices" special
      // case and return TRUE for this.


      // call to interactive() is an indicator that that someone may be
      // querying for dev.interactive(). in this case if we are not yet 
      // initialized and there is no active device then we need to initialize
      // and make ourselves active so that dev.interactive returns TRUE
      if (Rf_NoDevices())
      {
         Error error = makeActive();
         if (error)
            LOG_ERROR(error);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // always call original
   return s_originalInteractiveFunction(call, op, args, rho);   
}

DisplaySize displaySize()
{
   return DisplaySize(s_width, s_height);
}

Error saveSnapshot(const core::FilePath& snapshotFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error ;
   
   // save 
   return r::exec::RFunction(".rs.saveGraphics",
                             snapshotFile.absolutePath()).call();
}

Error restoreSnapshot(const core::FilePath& snapshotFile)
{
   // ensure we are active
   Error error = makeActive();
   if (error)
      return error ;
   
   // restore
   return r::exec::RFunction(".rs.restoreGraphics",
                             snapshotFile.absolutePath()).call();
}
   
   
   
// render the current display. note that this function properly defends itself
// against the state of no RStudio device loaded so can be called at any time. 
// in this case it simply writes an "empty" image
Error saveAsImageFile(const FilePath& targetFile)
{
   // verify the device is alive
   if (s_pGEDevDesc == NULL)
      return Error(errc::DeviceNotAvailable, ERROR_LOCATION);
   
   // write as png
   DeviceContext* pDC = (DeviceContext*)s_pGEDevDesc->dev->deviceSpecific;
   return handler::writeToPNG(targetFile, pDC, true);
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

SEXP rs_executeAndAttachManipulator(SEXP manipulatorSEXP)
{
   plotManager().executeAndAttachManipulator(manipulatorSEXP);
   return R_NilValue;
}

SEXP rs_hasActiveManipulator()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(plotManager().hasActiveManipulator(), &rProtect);
}

SEXP rs_activeManipulator()
{
   return plotManager().activeManipulator();
}

SEXP rs_setManipulatorAttribs(SEXP attribsSEXP)
{
   plotManager().setActiveManipulatorAttribs(attribsSEXP);
   return R_NilValue;
}


} // anonymous namespace
    
const int kDefaultWidth = 500;   
const int kDefaultHeight = 500; 
   
Error initialize(
            const FilePath& graphicsPath,
            const boost::function<bool(double*,double*)>& locatorFunction)
{      
   // save reference to locator function
   s_locatorFunction = locatorFunction;
   
   // create plot manager (provide functions & events)
   GraphicsDeviceFunctions graphicsDevice;
   graphicsDevice.displaySize = displaySize;
   graphicsDevice.saveSnapshot = saveSnapshot;
   graphicsDevice.restoreSnapshot = restoreSnapshot;
   graphicsDevice.saveAsImageFile = saveAsImageFile;
   graphicsDevice.copyToActiveDevice = copyToActiveDevice;
   graphicsDevice.imageFileExtension = imageFileExtension;
   graphicsDevice.close = close;
   Error error = plotManager().initialize(graphicsPath, 
                                          graphicsDevice,
                                          &s_graphicsDeviceEvents);
   if (error)
      return error;
   
   // set size
   setSize(kDefaultWidth, kDefaultHeight);

   // check for an incompatible graphics version before fully initializing.
   std::string message;
   if (graphics::validateEngineVersion(&message))
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

      // register execute and attach manipulator routine
      R_CallMethodDef execManipulatorMethodDef ;
      execManipulatorMethodDef.name = "rs_executeAndAttachManipulator" ;
      execManipulatorMethodDef.fun = (DL_FUNC) rs_executeAndAttachManipulator;
      execManipulatorMethodDef.numArgs = 1;
      r::routines::addCallMethod(execManipulatorMethodDef);

      // register has active manipulator routine
      R_CallMethodDef hasActiveManipulatorMethodDef ;
      hasActiveManipulatorMethodDef.name = "rs_hasActiveManipulator" ;
      hasActiveManipulatorMethodDef.fun = (DL_FUNC) rs_hasActiveManipulator;
      hasActiveManipulatorMethodDef.numArgs = 0;
      r::routines::addCallMethod(hasActiveManipulatorMethodDef);

      // register active manipulator routine
      R_CallMethodDef activeManipulatorMethodDef ;
      activeManipulatorMethodDef.name = "rs_activeManipulator" ;
      activeManipulatorMethodDef.fun = (DL_FUNC) rs_activeManipulator;
      activeManipulatorMethodDef.numArgs = 0;
      r::routines::addCallMethod(activeManipulatorMethodDef);

      // register set manipulator attribs routine
      R_CallMethodDef setManipulatorAttribsMethodDef ;
      setManipulatorAttribsMethodDef.name = "rs_setManipulatorAttribs" ;
      setManipulatorAttribsMethodDef.fun = (DL_FUNC) rs_setManipulatorAttribs;
      setManipulatorAttribsMethodDef.numArgs = 1;
      r::routines::addCallMethod(setManipulatorAttribsMethodDef);


      // register interactive() hook to work around dev.interactive device
      // bootstrapping bug
      error = function_hook::registerReplaceHook("interactive",
                                                 interactiveHook,
                                                 &s_originalInteractiveFunction);
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
} // namespace graphics
} // namespace session
} // namespace r



