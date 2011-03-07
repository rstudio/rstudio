/*
 * RGraphicsFileDevice.cpp
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

#include "RGraphicsFileDevice.hpp"

#include <cstdlib>

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>

#include <r/RExec.hpp>

#include "handler/RGraphicsHandler.hpp"

// nix windows definitions
#undef TRUE
#undef FALSE
#undef ERROR

#include <R_ext/RS.h>

using namespace core ;

namespace r {
namespace session {
namespace graphics {
namespace file_device {
   
namespace {
   
// name of our graphics device
const char * const kRStudioGraphicsFileDevice = "RStudioGraphicsFile";

using namespace handler;
   
Rboolean CFD_NewFrameConfirm(pDevDesc dd)
{   
   // returning false causes the default implementation (printing a prompt
   // of "Hit <Return> to see next plot:" to the console) to be used. this 
   // seems ideal compared to any custom UI we could produce so we leave it be
   return FALSE;
}
   
   
void CFD_Mode(int mode, pDevDesc dev)
{
   // 0 = stop drawing
   // 1 = start drawing
   // 2 = input active
}

void CFD_Size(double *left,
                double *right,
                double *bottom,
                double *top,
                pDevDesc dev)
{
   DeviceContext* pDC = (DeviceContext*) dev->deviceSpecific;

   *left = 0.0;
   *right = pDC->width;
   *bottom = pDC->height;
   *top = 0.0;
}
   
Rboolean CFD_Locator(double *x, double *y, pDevDesc dev)
{   
   return FALSE;
}

void CFD_Activate(pDevDesc dev)
{
   // do nothing
}

void CFD_Deactivate(pDevDesc dev)
{
   // do nothing
}   
   
void CFD_Close(pDevDesc dev)
{   
   DeviceContext* pDC = (DeviceContext*) dev->deviceSpecific;

   // png must be written explicitly (svg is automatically written)
   if (pDC->fileType == "png")
   {
      // write png
      Error error = handler::writeToPNG(pDC->targetPath, pDC, false);
      if (error)
         LOG_ERROR(error);
   }

   // destroy deviceSpecific struct
   handler::destroy(pDC);

   // explicitly free and then null out the dev pointer of the GEDevDesc
   // This is to avoid incompatabilities between the heap we are compiled with
   // and the heap R is compiled with (we observed this to a problem with
   // 64-bit R)
   pGEDevDesc pGEDevDesc = ::desc2GEDesc(dev);
   std::free(pGEDevDesc->dev);
   pGEDevDesc->dev = NULL;
}
   
void CFD_OnExit(pDevDesc dd)
{
   // NOTE: this may be called at various times including during error 
   // handling (jump_to_top_ex). therefore, do not place any process or device
   // final termination code here (even though the name of the function 
   // suggests you might want to do this!)
}


void createDevice(const std::string& fileType,
                  int width,
                  int height,
                  const FilePath& targetPath)
{
   R_CheckDeviceAvailable();

   BEGIN_SUSPEND_INTERRUPTS
   {
      // initialize v8 structure
      DevDescVersion8 devDesc;

      // device functions
      devDesc.activate = CFD_Activate;
      devDesc.deactivate = CFD_Deactivate;
      devDesc.size = CFD_Size;
      devDesc.clip = handler::clip;
      devDesc.rect = handler::rect;
      devDesc.path = handler::path;
      devDesc.raster = handler::raster;
      devDesc.cap = handler::cap;
      devDesc.circle = handler::circle;
      devDesc.line = handler::line;
      devDesc.polyline = handler::polyline;
      devDesc.polygon = handler::polygon;
      devDesc.locator = CFD_Locator;
      devDesc.mode = CFD_Mode;
      devDesc.metricInfo = handler::metricInfo;
      devDesc.strWidth = devDesc.strWidthUTF8 = handler::strWidth;
      devDesc.text = devDesc.textUTF8 = handler::text;
      devDesc.hasTextUTF8 = TRUE;
      devDesc.wantSymbolUTF8 = TRUE;
      devDesc.useRotatedTextInContour = FALSE;
      devDesc.newPage = handler::newPage;
      devDesc.close = CFD_Close;
      devDesc.newFrameConfirm = CFD_NewFrameConfirm;
      devDesc.onExit = CFD_OnExit;
      devDesc.eventEnv = R_NilValue;
      devDesc.eventHelper = NULL;

      // allocate device
      pDevDesc pDev = handler::dev_desc::allocate(devDesc);

      // allocate and initialize context
      bool initialized = false;
      DeviceContext* pDC = handler::allocate(pDev);
      if (fileType == "png")
      {
         initialized = handler::initializePNG(targetPath,
                                              width,
                                              height,
                                              false,
                                              pDC);
      }
      else if (fileType == "svg")
      {
         initialized = handler::initializeSVG(targetPath,
                                              width,
                                              height,
                                              false,
                                              pDC);
      }

      if (!initialized)
      {
         handler::destroy(pDC);

         // leak the pDev on purpose because we don't have
         // access to R's heap/free function

         Rf_error("Unable to start RStudio cairo file device");
      }

      // set device specific context
      pDev->deviceSpecific = pDC;

      // device attributes
      handler::setSize(pDev);
      handler::setDeviceAttributes(pDev);

      // associate with device description and add it
      pGEDevDesc pDD = GEcreateDevDesc(pDev);
      GEaddDevice2(pDD, kRStudioGraphicsFileDevice);

      // make us active
      Rf_selectDevice(Rf_ndevNumber(pDD->dev));
   }
   END_SUSPEND_INTERRUPTS;
}


} // anonymous namespace
    

bool supportsSvg()
{
   return handler::supportsSVG();
}

Error create(const std::string& fileType,
             int width,
             int height,
             const FilePath& targetPath)
{
   // execute but catch R errors and convert them to native errors

   return r::exec::executeSafely(boost::bind(createDevice,
                                                fileType,
                                                width,
                                                height,
                                                targetPath));

}


} // namespace file_device
} // namespace graphics
} // namespace session
} // namespace r



