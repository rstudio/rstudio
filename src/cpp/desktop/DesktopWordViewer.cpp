/*
 * DesktopWordViewer.cpp
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

#include <iostream>

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <shared_core/Error.hpp>
#include <core/system/System.hpp>

#include "DesktopComUtils.hpp"
#include "DesktopWordViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

WordViewer::WordViewer():
   OfficeViewer(L"Word.Application", L"Documents",
                2 /* position of read-only flag in Open method */),
   docScrollX_(0),
   docScrollY_(0)
{
}

Error WordViewer::savePosition(IDispatch* idispDoc)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispWindow = nullptr;

   VERIFY_HRESULT(getIDispatchProp(idispDoc, L"ActiveWindow", &idispWindow));
   VERIFY_HRESULT(getIntProp(idispWindow, L"HorizontalPercentScrolled", &docScrollX_));
   VERIFY_HRESULT(getIntProp(idispWindow, L"VerticalPercentScrolled", &docScrollY_));

LErrExit:
   return errorHR;
}

Error WordViewer::restorePosition(IDispatch* idispDoc) const
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispWindow = nullptr;
   VARIANT varPos;
   varPos.vt = VT_INT;

   VERIFY_HRESULT(getIDispatchProp(idispDoc, L"ActiveWindow", &idispWindow));
   varPos.intVal = docScrollX_;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idispWindow,
                                 L"HorizontalPercentScrolled", 1, varPos));
   varPos.intVal = docScrollY_;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idispWindow,
                                 L"VerticalPercentScrolled", 1, varPos));

LErrExit:
   return errorHR;
}

bool WordViewer::hasPosition() const
{
   return docScrollX_ > 0 || docScrollY_ > 0;
}

void WordViewer::resetPosition()
{
   docScrollX_ = 0;
   docScrollY_ = 0;
}

} // namespace desktop
} // namespace rstudio
