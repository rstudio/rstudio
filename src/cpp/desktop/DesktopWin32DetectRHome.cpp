/*
 * DesktopWin32DetectRHome.cpp
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

#ifndef _WIN32
#error DesktopDetectRHome.cpp is Windows-specific
#endif

#include "DesktopDetectRHome.hpp"

#include <windows.h>

#include <boost/bind.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopRVersion.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

bool prepareEnvironment(Options &options)
{
   bool forceUi = ::GetAsyncKeyState(VK_CONTROL) & ~1;

   RVersion rVersion = detectRVersion(forceUi);
   if (!rVersion.isValid())
      return false;


   // get the short path version of the home dir
   std::string homePath =
         QDir::toNativeSeparators(rVersion.homeDir()).toStdString();
   DWORD len = ::GetShortPathName(homePath.c_str(), nullptr, 0);
   std::vector<TCHAR> buffer(len, 0);
   if (::GetShortPathName(homePath.c_str(), &(buffer[0]), len) != 0)
   {
      // copy path to string and assign it we got one
      std::string shortHomePath(&(buffer[0]));
      if (!shortHomePath.empty())
         homePath = shortHomePath;
   }
   else
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
   }


   // set R_HOME
   system::setenv("R_HOME", homePath);

   std::string path =
         QDir::toNativeSeparators(rVersion.binDir()).toStdString() + ";" +
         system::getenv("PATH");
   system::setenv("PATH", path);

   return true;
}

} // namespace desktop
} // namespace rstudio
