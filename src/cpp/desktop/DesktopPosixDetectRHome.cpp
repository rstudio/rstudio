/*
 * DesktopMacDetectRHome.cpp
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

#include "DesktopDetectRHome.hpp"

#include <vector>

#include <boost/algorithm/string/trim.hpp>

#include <QtCore>
#include <QMessageBox>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/r_util/REnvironment.hpp>

using namespace core;

namespace desktop {

namespace {

void showRNotFoundError(const std::string& msg)
{
   QMessageBox::critical(NULL, "R Not Found", QString::fromStdString(msg));
}

} // anonymous namespace

bool prepareEnvironment(Options& options)
{
   // check for r script override
   FilePath rWhichRPath;
   std::string rScriptVal = core::system::getenv("RSTUDIO_WHICH_R");
   if (!rScriptVal.empty())
   {
      // set it
      rWhichRPath = FilePath(rScriptVal);

      // but warn (and ignore) if it doesn't exist
      if (!rWhichRPath.exists())
      {
         LOG_WARNING_MESSAGE("Specified RSTUDIO_WHICH_R (" + rScriptVal +
                             ") does not exist (ignoring)");
         rWhichRPath = FilePath();
      }

      // also warn and ignore if it is a directory
      else if (rWhichRPath.isDirectory())
      {
         LOG_WARNING_MESSAGE("Specified RSTUDIO_WHICH_R (" + rScriptVal +
                             ") is a directory rather than file (ignoring)");
         rWhichRPath = FilePath();
      }
   }

   // determine rLdPaths script location
   FilePath supportingFilePath = options.supportingFilePath();
   FilePath rLdScriptPath = supportingFilePath.complete("bin/r-ldpath");
   if (!rLdScriptPath.exists())
      rLdScriptPath = supportingFilePath.complete("session/r-ldpath");

   // attempt to detect R environment
   std::string errMsg;
   r_util::EnvironmentVars rEnvVars;
   bool success = r_util::detectREnvironment(rWhichRPath,
                                             rLdScriptPath,
                                             &rEnvVars,
                                             &errMsg);
   if (!success)
   {
      LOG_ERROR_MESSAGE(errMsg);
      showRNotFoundError(errMsg);
      return false;
   }

   // set environment and return true
   r_util::setREnvironmentVars(rEnvVars);
   return true;
}

} // namespace desktop
