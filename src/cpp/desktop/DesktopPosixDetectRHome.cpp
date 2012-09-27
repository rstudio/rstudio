/*
 * DesktopPosixDetectRHome.cpp
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

#include "DesktopDetectRHome.hpp"

#include <vector>

#include <boost/algorithm/string/trim.hpp>

#include <QtCore>
#include <QMessageBox>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/REnvironment.hpp>

#include "DesktopUtils.hpp"
#include "DesktopOptions.hpp"

using namespace core;

namespace desktop {

namespace {

void showRNotFoundError(const std::string& msg)
{
   showMessageBox(QMessageBox::Critical,
                  NULL,
                  QString::fromUtf8("R Not Found"),
                  QString::fromUtf8(msg.c_str()));
}

} // anonymous namespace

bool prepareEnvironment(Options& options)
{
   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = core::system::getenv("RSTUDIO_WHICH_R");
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);

   // determine rLdPaths script location
   FilePath supportingFilePath = options.supportingFilePath();
   FilePath rLdScriptPath = supportingFilePath.complete("bin/r-ldpath");
   if (!rLdScriptPath.exists())
      rLdScriptPath = supportingFilePath.complete("session/r-ldpath");

   // attempt to detect R environment
   std::string rScriptPath, errMsg;
   r_util::EnvironmentVars rEnvVars;
   bool success = r_util::detectREnvironment(rWhichRPath,
                                             rLdScriptPath,
                                             std::string(),
                                             &rScriptPath,
                                             &rEnvVars,
                                             &errMsg);
   if (!success)
   {
      showRNotFoundError(errMsg);
      return false;
   }

   if (desktop::options().verifyInstallation())
   {
      std::cout << std::endl << "Using R script: " << rScriptPath
                << std::endl;
   }

   // set environment and return true
   r_util::setREnvironmentVars(rEnvVars);
   return true;
}

} // namespace desktop
