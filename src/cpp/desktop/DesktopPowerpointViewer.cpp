/*
 * DesktopPowerpointViewer.cpp
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
#include "DesktopPowerpointViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

PowerpointViewer::PowerpointViewer():
    OfficeViewer(L"Powerpoint.Application", L"Presentations",
                 1 /* position of read-only flag in Open method */),
    slideIndex_(0)
{
}

Error PowerpointViewer::getDocumentWindow(IDispatch* source, IDispatch** window) const
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   // Get the first document window for the presentation
   IDispatch* idispWindows = nullptr;
   VARIANT varResult;
   VARIANT varItem;
   varItem.vt = VT_INT;
   varItem.intVal = 1;
   VERIFY_HRESULT(getIDispatchProp(source, L"Windows", &idispWindows));
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, &varResult, idispWindows, L"Item", 1, varItem));
   *window = varResult.pdispVal;

LErrExit:
   return errorHR;
}

Error PowerpointViewer::savePosition(IDispatch* source)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispPres      = nullptr;
   IDispatch* idispSelection = nullptr;
   IDispatch* idispRange     = nullptr;

   // Get the window containing the document interface
   errorHR = getDocumentWindow(source, &idispPres);
   if (errorHR)
      return errorHR;

   // Get the selection (the slides the user has selected)
   VERIFY_HRESULT(getIDispatchProp(idispPres, L"Selection", &idispSelection));
   VERIFY_HRESULT(getIDispatchProp(idispSelection, L"SlideRange", &idispRange));

   // Find the slide number the user's working on
   VERIFY_HRESULT(getIntProp(idispRange, L"SlideNumber", &slideIndex_));

LErrExit:
   return errorHR;
}

Error PowerpointViewer::restorePosition(IDispatch* target) const
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispPres = nullptr;
   IDispatch* idispView = nullptr;

   // Get the window containing the document interface
   errorHR = getDocumentWindow(target, &idispPres);
   if (errorHR)
      return errorHR;

   // Get the slide view
   VERIFY_HRESULT(getIDispatchProp(idispPres, L"View", &idispView));

   // Go to the slide in question
   VARIANT varSlide;
   varSlide.vt = VT_INT;
   varSlide.intVal = slideIndex_;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, nullptr, idispView,
                                 L"GotoSlide", 1, varSlide));

LErrExit:
   return errorHR;
}

void PowerpointViewer::resetPosition()
{
   slideIndex_ = 0;
}

bool PowerpointViewer::hasPosition() const
{
   return slideIndex_ > 0;
}

} // namespace desktop
} // namespace rstudio
