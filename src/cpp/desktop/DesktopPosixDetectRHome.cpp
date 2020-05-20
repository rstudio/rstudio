/*
 * DesktopPosixDetectRHome.cpp
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

#include "DesktopDetectRHome.hpp"

#include <boost/algorithm/string/trim.hpp>

#include <QMessageBox>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/REnvironment.hpp>

#include "DesktopUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {

void showRNotFoundError(const std::string& msg)
{
   showMessageBox(QMessageBox::Critical,
                  nullptr,
                  QString::fromUtf8("R Not Found"),
                  QString::fromUtf8(msg.c_str()), QString());
}

} // anonymous namespace

bool prepareEnvironment(Options& options)
{
   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = core::system::getenv("RSTUDIO_WHICH_R");
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);

#ifdef Q_OS_MAC
   FilePath rLdScriptPath = options.scriptsPath().completePath("session/r-ldpath");
   if (!rLdScriptPath.exists())
   {
      FilePath executablePath;
      Error error = core::system::executablePath(nullptr, &executablePath);
      if (error)
         LOG_ERROR(error);
      rLdScriptPath = executablePath.getParent().completePath("r-ldpath");
   }
#else
   // determine rLdPaths script location
   FilePath supportingFilePath = options.supportingFilePath();
   FilePath rLdScriptPath = supportingFilePath.completePath("bin/r-ldpath");
   if (!rLdScriptPath.exists())
      rLdScriptPath = supportingFilePath.completePath("session/r-ldpath");
#endif
   // attempt to detect R environment
   std::string rScriptPath, rVersion, errMsg;
   r_util::EnvironmentVars rEnvVars;
   bool success = r_util::detectREnvironment(rWhichRPath,
                                             rLdScriptPath,
                                             std::string(),
                                             &rScriptPath,
                                             &rVersion,
                                             &rEnvVars,
                                             &errMsg);
   if (!success)
   {
      showRNotFoundError(errMsg);
      return false;
   }

   if (desktop::options().runDiagnostics())
   {
      std::cout << std::endl << "Using R script: " << rScriptPath
                << std::endl;
   }

   // set environment and return true
   r_util::setREnvironmentVars(rEnvVars);
   return true;
}

} // namespace desktop
} // namespace rstudio
