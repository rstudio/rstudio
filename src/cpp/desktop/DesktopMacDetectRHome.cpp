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

#include <QtCore>
#include <QMessageBox>

#include <core/system/System.hpp>

#include "config.h"

using namespace core;

namespace desktop {

namespace {

bool detectR(FilePath* pLibraryPath)
{
   FilePath realPath;
   Error error = core::system::realPath(
         "/Library/Frameworks/R.framework/Versions/Current",
         &realPath);
   if (error)
      return false;

   // set library path
   *pLibraryPath = realPath.complete("Resources/lib");

   QString version = QString::fromStdString(realPath.filename());
   QRegExp regexp("^(\\d+)\\.(\\d+)(?:\\.(\\d+))*$");
   int index = regexp.indexIn(version);
   if (index != 0)
      return false;

   int major = 0, minor = 0;

   bool success;
   major = regexp.cap(1).toInt(&success);
   if (!success)
      return false;

   minor = regexp.cap(2).toInt(&success);
   if (!success)
      return false;

   if (major > RSTUDIO_R_MAJOR_VERSION_REQUIRED)
      return true;
   if (major < RSTUDIO_R_MAJOR_VERSION_REQUIRED)
      return false;
   return minor*10 >= (RSTUDIO_R_MINOR_VERSION_REQUIRED +
                       RSTUDIO_R_PATCH_VERSION_REQUIRED);
}

} // anonymous namespace

bool prepareEnvironment(Options&)
{
   FilePath libraryPath;
   if (!detectR(&libraryPath))
   {
      QMessageBox::critical(NULL,
                            "R Not Found",
                            "Please make sure a compatible version of R is "
                            "installed, then try again.");
      return false;
   }

   // set DYLD_LIBRARY_PATH
   core::system::setenv("DYLD_LIBRARY_PATH", libraryPath.absolutePath());

   return true;
}

} // namespace desktop
