/*
 * DesktopWin32DetectRHome.cpp
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

#ifndef _WIN32
#error DesktopDetectRHome.cpp is Windows-specific
#endif

#include "DesktopDetectRHome.hpp"

#include <windows.h>

#include <boost/bind.hpp>

#include <core/system/System.hpp>

#include "DesktopRVersion.hpp"

using namespace core;

namespace desktop {

bool prepareEnvironment(Options &options)
{
   bool forceUi = ::GetAsyncKeyState(VK_CONTROL) & ~1;

   RVersion rVersion = detectRVersion(forceUi);
   if (!rVersion.isValid())
      return false;

   system::setenv("R_HOME", rVersion.homeDir().toStdString());

   std::string path =
         QDir::toNativeSeparators(rVersion.binDir()).toStdString() + ";" +
         system::getenv("PATH");
   system::setenv("PATH", path);

   return true;
}

} // namespace desktop
