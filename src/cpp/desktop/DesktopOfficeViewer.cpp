/*
 * DesktopOfficeViewer.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <iostream>

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <boost/utility.hpp>
#include <boost/scoped_array.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopComUtils.hpp"
#include "DesktopOfficeViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

OfficeViewer::OfficeViewer(const std::wstring& progId):
   idisp_(nullptr),
   progId_(progId)
{
}

OfficeViewer::~OfficeViewer()
{
   try
   {
      if (idisp_)
         idisp_->Release();
   }
   catch (...)
   {
      // Ignore exceptions during teardown
   }
}

Error OfficeViewer::ensureInterface()
{
   HRESULT hr = S_OK;
   Error errorHR = Success();

   // If we have an active IDispatch pointer, check to see whether it has been
   // closed
   if (idisp_ != nullptr)
   {
      // Test the interface by looking up a known DISPID
      const WCHAR* wstrQuit = L"Quit";
      DISPID dispid;
      hr = idisp_->GetIDsOfNames(IID_NULL, const_cast<WCHAR**>(&wstrQuit),
                                     1, LOCALE_USER_DEFAULT, &dispid);

      // If the lookup fails, release this IDispatch pointer--it's stale.
      // We'll CoCreate a new instance below.
      if (FAILED(hr) &&
          SCODE_CODE(hr) == RPC_S_SERVER_UNAVAILABLE)
      {
         idisp_->Release();
         idisp_ = nullptr;
      }
   }

   // Get an IDispatch for the Word Application root object
   if (idisp_ == nullptr)
   {
      CLSID clsid;
      CoInitialize(nullptr);
      VERIFY_HRESULT(CLSIDFromProgID(progId_.c_str(), &clsid));
      VERIFY_HRESULT(CoCreateInstance(clsid, nullptr, CLSCTX_LOCAL_SERVER,
                                      IID_IDispatch,
                                      reinterpret_cast<void**>(&idisp_)));
      idisp_->AddRef();
   }

LErrExit:
   return errorHR;
}

Error OfficeViewer::showApp()
{
   // Allow the application to become the foreground window.
   // CoAllowSetForegroundWindow would be preferable here, since we'd be able
   // to restrict activation to only the process we started, but it is not
   // exposed by MinGW headers.  Note that AllowSetForegroundWindow already
   // limits activation to processes initiated by the foreground process, and
   // self-expires on user input.
   AllowSetForegroundWindow(ASFW_ANY);

   Error errorHR = Success();
   HRESULT hr = S_OK;

   VARIANT visible;
   visible.vt = VT_BOOL;
   visible.boolVal = true;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idisp_,
                                 L"Visible", 1, visible));
LErrExit:
   return errorHR;
}

IDispatch* OfficeViewer::idispApp()
{
   return idisp_;
}

} // namespace rstudio
} // namespace desktop

