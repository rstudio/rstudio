/*
 * DesktopWin32ProgressActivator.cpp
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
#error DesktopWin32ProgressActivator.cpp is Windows-specific
#endif

#include "DesktopProgressActivator.hpp"

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <QApplication>

#include "DesktopUtils.hpp"

#include <windows.h>

// hook that gets notified of all windows shown system-wide. we use
// this to detect GraphApp and bring it to the foreground whenever
// our appliation is also in the foreground
extern "C" void CALLBACK WinEventProc(HWINEVENTHOOK, DWORD event, HWND hwnd,
                                      LONG, LONG, DWORD, DWORD)
{
   if ((event == EVENT_OBJECT_SHOW) &&
        ::IsWindow(hwnd) &&
       (QApplication::applicationState() & Qt::ApplicationActive))
   {
      TCHAR szClass[80];
      szClass[0] = TEXT('\0');
      if ((::GetClassName(hwnd, szClass, ARRAYSIZE(szClass)) != 0) &&
          (::strcmp("GraphApp", szClass) == 0))
      {
         ::SetForegroundWindow(hwnd);
      }
   }
}

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

struct ProgressActivator::Impl
{
   HWINEVENTHOOK hWinEventHook;
};

ProgressActivator::ProgressActivator()
   : pImpl_(new Impl())
{
  pImpl_->hWinEventHook = ::SetWinEventHook(
        EVENT_OBJECT_SHOW, EVENT_OBJECT_SHOW,
        nullptr, WinEventProc, 0, 0,
        WINEVENT_OUTOFCONTEXT | WINEVENT_SKIPOWNPROCESS);

  if (pImpl_->hWinEventHook == 0)
  {
     Error error = LAST_SYSTEM_ERROR();
     LOG_ERROR(error);
  }
}

ProgressActivator::~ProgressActivator()
{
   if (pImpl_->hWinEventHook != 0)
      ::UnhookWinEvent(pImpl_->hWinEventHook);
}


} // namespace desktop
} // namespace rstudio
