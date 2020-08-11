/*
 * RGraphicsUtils.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "RGraphicsUtils.hpp"

#include <boost/format.hpp>

#include <shared_core/Error.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>

#include <Rinternals.h>
#define R_USE_PROTOTYPES 1
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>

#include <r/session/RGraphicsConstants.h>

#include <r/RErrorCategory.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace graphics { 

namespace {

int s_compatibleEngineVersion = 13;

#ifdef __APPLE__
class QuartzStatus : boost::noncopyable
{
public:
   QuartzStatus() : checked_(false), installed_(false) {}

   bool isInstalled()
   {
      if (!checked_)
      {
         checked_ = true;

         Error error = r::exec::RFunction("capabilities",
                                          "aqua").call(&installed_);
         if (error)
            LOG_ERROR(error);
      }

      return installed_;
   }

private:
   bool checked_;
   bool installed_;
};

bool hasRequiredGraphicsDevices(std::string* pMessage)
{
   static QuartzStatus s_quartzStatus;
   if (!s_quartzStatus.isInstalled())
   {
      if (pMessage != nullptr)
      {
         *pMessage = "\nWARNING: The version of R you are running against "
                     "does not support the quartz graphics device (which is "
                     "required by RStudio for graphics). The Plots tab will "
                     "be disabled until a version of R that supports quartz "
                     "is installed.";
      }
      return false;
   }
   else
   {
      return true;
   }
}

#else

bool hasRequiredGraphicsDevices(std::string* pMessage)
{
   return true;
}

#endif

} // anonymous namespace

std::string getDefaultBackend()
{
   return r::options::getOption<std::string>(
            kGraphicsOptionBackend,
            "default",
            false);
}

std::string getDefaultAntialiasing()
{
   return r::options::getOption<std::string>(
            kGraphicsOptionAntialias,
            "default",
            false);
}

void setCompatibleEngineVersion(int version)
{
   s_compatibleEngineVersion = version;
}

bool validateRequirements(std::string* pMessage)
{
   // get engineVersion
   int engineVersion = R_GE_getVersion();

   // version too old
   if (engineVersion < 5)
   {
      if (pMessage != nullptr)
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
      if (pMessage != nullptr)
      {
         boost::format fmt(
            "R graphics engine version %1% is not supported by this "
            "version of RStudio. The Plots tab will be disabled until "
            "a newer version of RStudio is installed.");
         *pMessage = boost::str(fmt % engineVersion);
      }

      return false;
   }


   // check for required devices
   else
   {
      return hasRequiredGraphicsDevices(pMessage);
   }
}

std::string extraBitmapParams()
{
   std::vector<std::string> params;
   
   std::vector<std::string> supportedBackends;
   Error error = r::exec::RFunction(".rs.graphics.supportedBackends").call(&supportedBackends);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }
   
   std::string backend = getDefaultBackend();
   
   // if the requested backend is not supported, silently use the default
   // (this could happen if a package's configuration was migrated from
   // one machine to another on a different OS)
   if (backend != "default")
   {
      auto it = std::find(supportedBackends.begin(), supportedBackends.end(), backend);
      if (it == supportedBackends.end())
         backend = "default";
   }
   
   // don't use the 'ragg' backend here (these parameters are normally passed
   // to devices defined by the 'grDevices' package, and it doesn't handle
   // 'ragg')
   if (backend != "default" && backend != "ragg")
      params.push_back("type = \"" + backend + "\"");
   
   std::string antialias = getDefaultAntialiasing();

#ifdef _WIN32
   // fix up antialias for windows backend
   if ((backend == "windows" || backend == "default") && antialias == "subpixel")
      antialias = "cleartype";
#endif

   if (antialias != "default")
      params.push_back("antialias = \"" + antialias + "\"");
   
   if (params.empty())
      return "";
   
   return ", " + core::algorithm::join(params, ", ");
}

struct RestorePreviousGraphicsDeviceScope::Impl
{
   Impl() : pPreviousDevice(nullptr) {}
   pGEDevDesc pPreviousDevice;
};

   
RestorePreviousGraphicsDeviceScope::RestorePreviousGraphicsDeviceScope()
   : pImpl_(new Impl())
{
   // save ptr to previously selected device (if there is one)
   pImpl_->pPreviousDevice = Rf_NoDevices() ? nullptr : GEcurrentDevice();
}
         
RestorePreviousGraphicsDeviceScope::~RestorePreviousGraphicsDeviceScope()
{
   try
   {
      // reslect the previously selected device if we had one
      if (pImpl_->pPreviousDevice != nullptr)
         Rf_selectDevice(Rf_ndevNumber(pImpl_->pPreviousDevice->dev));
   }
   catch(...)
   {
   }
}

void reportError(const core::Error& error)
{
   std::string endUserMessage = r::endUserErrorMessage(error);
   std::string errmsg = "Graphics error: " + endUserMessage;
   REprintf("%s\n", errmsg.c_str());
}

void logAndReportError(const Error& error, const ErrorLocation& location)
{
   // log
   core::log::logError(error, location);

   // report to user
   reportError(error);
}
        

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio

