/*
 * DesktopUtilsMac.mm
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

#include "DesktopUtils.hpp"

#include <QWidget>

namespace desktop {

bool isRetina(QMainWindow* pMainWindow)
{
   OSWindowRef macWindow = qt_mac_window_for(pMainWindow);
   NSWindow* pWindow = (NSWindow*)macWindow;

   if ([pWindow respondsToSelector:@selector(backingScaleFactor)])
   {
      double scaleFactor = [pWindow backingScaleFactor];
      return scaleFactor == 2.0;
   }
   else
   {
      return false;
   }
}

} // namespace desktop
