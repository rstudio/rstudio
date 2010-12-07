/*
 * RGraphicsUtils.cpp
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

#include "RGraphicsUtils.hpp"

#include <boost/format.hpp>

#include <core/Error.hpp>

#include <Rinternals.h>
#define R_USE_PROTOTYPES 1
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>

using namespace core;

namespace r {
namespace session {
namespace graphics { 

namespace {
int s_compatibleEngineVersion = 8;
}

void setCompatibleEngineVersion(int version)
{
   s_compatibleEngineVersion = version;
}

bool validateEngineVersion(std::string* pMessage)
{
   // get engineVersion
   int engineVersion = R_GE_getVersion();

   // version too old
   if (engineVersion < 5)
   {
      if (pMessage != NULL)
      {
         boost::format fmt(
            "R graphics engine version %1% is not supported by RStudio. "
            "The Plots tab will be disabled until a newer version of "
            "R is installed.");
         *pMessage = boost::str(fmt % engineVersion);
      }

      return false;
   }

   // version too new
   else if (engineVersion > s_compatibleEngineVersion)
   {
      if (pMessage != NULL)
      {
         boost::format fmt(
            "R graphics engine version %1% is not supported by this "
            "version of RStudio. The Plots tab will be disabled until "
            "a newer version of RStudio is installed.");
         *pMessage = boost::str(fmt % engineVersion);
      }

      return false;
   }

   // compatible
   else
   {
      return true;
   }
}

struct RestorePreviousGraphicsDeviceScope::Impl
{
   Impl() : pPreviousDevice(NULL) {}
   pGEDevDesc pPreviousDevice;
};

   
RestorePreviousGraphicsDeviceScope::RestorePreviousGraphicsDeviceScope()
   : pImpl_(new Impl())
{
   // save ptr to previously selected device (if there is one)
   pImpl_->pPreviousDevice = Rf_NoDevices() ? NULL : GEcurrentDevice();
}
         
RestorePreviousGraphicsDeviceScope::~RestorePreviousGraphicsDeviceScope()
{
   try
   {
      // reslect the previously selected device if we had one
      if (pImpl_->pPreviousDevice != NULL)
         Rf_selectDevice(Rf_ndevNumber(pImpl_->pPreviousDevice->dev));  
   }
   catch(...)
   {
   }
}
        

} // namespace graphics
} // namespace session
} // namesapce r

